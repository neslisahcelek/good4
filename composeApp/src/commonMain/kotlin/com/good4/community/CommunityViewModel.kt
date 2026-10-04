package com.good4.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityState(
    val communities: List<Community> = emptyList(),
    val featuredEvents: List<CommunityFeaturedEvent> = emptyList(),
    val featuredEventsLoading: Boolean = false,
    val featuredEventsError: String? = null,
    val selectedCategoryId: String = "",
    val followedOnly: Boolean = false,
    val followedCommunityIds: Set<String> = emptySet(),
    val followingLoading: Boolean = false,
    val followingLoaded: Boolean = false,
    val followingError: String? = null,
    val selected: Community? = null,
    val entries: List<CommunityEntry> = emptyList(),
    val entriesCursor: String? = null,
    val access: CommunityAccessDto = CommunityAccessDto(),
    val businesses: List<CommunityBusiness> = emptyList(),
    val isFollowing: Boolean = false,
    val followLoading: Boolean = false,
    val generatedCouponCode: String? = null,
    val generatedCouponEntryId: String? = null,
    val codeGenerating: Boolean = false,
    val registrationsByEntry: Map<String, List<CommunityEventRegistrationDto>> = emptyMap(),
    val attendanceByEntry: Map<String, List<EventAttendanceDto>> = emptyMap(),
    val followerCount: Int? = null,
    val ticket: CommunityEventRegistrationDto? = null,
    val ticketEventId: String? = null,
    val admissionBusy: Boolean = false,
    val admissionMessage: String? = null,
    val registeredEventIds: Set<String> = emptySet(),
    val registrationLoadingIds: Set<String> = emptySet(),
    /** Subset of [registrationLoadingIds] whose pending request cancels a registration. */
    val cancellingRegistrationIds: Set<String> = emptySet(),
    val justRegisteredEntryId: String? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val blockedCommunityIds: Set<String> = emptySet(),
    val reportSending: Boolean = false,
    val reportSentEntryId: String? = null,
    val reportError: String? = null
) {
    val canManage: Boolean get() = access.active && selected?.id in access.communityIds
    val filteredFeaturedEvents: List<CommunityFeaturedEvent> get() = filterFeaturedCommunityEvents(
        featuredEvents, selectedCategoryId, followedOnly, followedCommunityIds,
    )
}

internal fun CommunityState.withStudentRegistration(entryId: String, registered: Boolean, showFeedback: Boolean = false): CommunityState {
    val adjustment = if (registered == (entryId in registeredEventIds)) 0 else if (registered) 1 else -1
    return copy(
        registeredEventIds = if (registered) registeredEventIds + entryId else registeredEventIds - entryId,
        registrationLoadingIds = registrationLoadingIds - entryId,
        cancellingRegistrationIds = cancellingRegistrationIds - entryId,
        justRegisteredEntryId = if (registered && showFeedback) entryId else null,
        entries = entries.map { entry ->
            if (entry.id == entryId) entry.copy(data = entry.data.copy(registrationCount = (entry.data.registrationCount + adjustment).coerceAtLeast(0)))
            else entry
        }
    )
}

class CommunityViewModel(private val repository: CommunityRepository, private val admission: EventAdmissionGateway = NativeEventAdmissionGateway) : ViewModel() {
    private val mutable = MutableStateFlow(CommunityState())
    val state = mutable.asStateFlow()
    private var loadingJob: Job? = null
    private var registrationJob: Job? = null
    private var featuredEventsJob: Job? = null
    private var featuredEventsKey: String? = null
    private var followingJob: Job? = null
    private var followingUserId: String? = null
    private var observedUserId = repository.currentUserId
    private var updatesVisible = false
    private var visibleStudentEventId: String? = null

    init {
        load()
        viewModelScope.launch {
            repository.authStateFlow.collect { user ->
                if (user?.uid != observedUserId) {
                    observedUserId = user?.uid
                    loadingJob?.cancel()
                    registrationJob?.cancel()
                    featuredEventsJob?.cancel()
                    followingJob?.cancel()
                    featuredEventsKey = null
                    followingUserId = null
                    mutable.value = CommunityState(loading = user != null)
                    if (user != null) load()
                }
            }
        }
    }

    fun selectCategory(categoryId: String) {
        if (categoryId.isNotEmpty() && categoryId != EventCategory.UNCATEGORIZED && EventCategory.fromId(categoryId) == null) return
        mutable.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun setFollowedOnly(selected: Boolean) {
        mutable.update { it.copy(followedOnly = selected) }
        if (selected) refreshFollowing()
    }

    fun refreshFollowing(force: Boolean = false) {
        if (AppEnvironment.firebaseBackend != FirebaseBackend.V2) return
        val uid = repository.currentUserId
        if (uid == null) {
            followingJob?.cancel()
            followingUserId = null
            mutable.update { it.copy(followedCommunityIds = emptySet(), followingLoaded = false, followingLoading = false, followingError = null) }
            return
        }
        val snapshot = mutable.value
        if (!force && followingUserId == uid && (snapshot.followingLoading || snapshot.followingLoaded)) return
        followingUserId = uid
        followingJob?.cancel()
        mutable.update { it.copy(followingLoading = true, followingError = null) }
        followingJob = viewModelScope.launch {
            try {
                val ids = repository.followingCommunityIds()
                if (repository.currentUserId == uid) {
                    mutable.update { it.copy(followedCommunityIds = ids, followingLoading = false, followingLoaded = true, followingError = null) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (repository.currentUserId == uid) {
                    mutable.update { it.copy(followingLoading = false, followingLoaded = false, followingError = "Takip ettiğiniz topluluklar yüklenemedi.") }
                }
            }
        }
    }

    /**
     * Reloads the list in place when the student comes back to it, so a community approved while the
     * app stayed open shows up without a restart. A load that is already running is left alone.
     */
    fun refreshCommunities() {
        if (mutable.value.loading || repository.currentUserId == null) return
        load(silent = true)
    }

    fun load(silent: Boolean = false) {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            if (!silent) mutable.update { it.copy(loading = true, error = null) }
            try {
                val communities = repository.list()
                val access = repository.access()
                val businesses = repository.businesses()
                val blocked = loadBlockedCommunityIds()
                mutable.update {
                    it.copy(
                        // Managers always keep their own community, even if it was blocked earlier.
                        communities = communities.filterNot { c -> c.id in blocked && c.id !in access.communityIds },
                        blockedCommunityIds = blocked,
                        access = access,
                        businesses = businesses,
                        loading = false,
                        error = null
                    )
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                // A failed background refresh keeps the list that is already on screen.
                if (!silent) mutable.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun refreshFeaturedEvents(today: String, force: Boolean = false) {
        val snapshot = mutable.value
        if (!snapshot.loading && snapshot.selected == null) refreshFollowing(force)
        if (snapshot.loading || snapshot.selected != null || snapshot.communities.isEmpty()) return
        val communityKey = snapshot.communities.joinToString("|") {
            "${it.id}:${it.data.name}:${it.data.logoUrl}"
        }
        val key = "$today:$communityKey"
        if (!force && featuredEventsKey == key) return
        featuredEventsKey = key
        featuredEventsJob?.cancel()
        mutable.update { it.copy(featuredEventsLoading = true, featuredEventsError = null) }
        featuredEventsJob = viewModelScope.launch {
            try {
                val events = repository.featuredEvents(snapshot.communities, today)
                mutable.update {
                    it.copy(featuredEvents = events, featuredEventsLoading = false, featuredEventsError = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.update {
                    it.copy(
                        featuredEvents = emptyList(),
                        featuredEventsLoading = false,
                        featuredEventsError = e.message ?: "Etkinlikler yüklenemedi."
                    )
                }
            }
        }
    }

    fun select(community: Community, targetEventId: String? = null) {
        loadingJob?.cancel()
        registrationJob?.cancel()
        mutable.update {
            it.copy(
                selected = community,
                entries = emptyList(),
                entriesCursor = null,
                loading = true,
                error = null,
                generatedCouponCode = null,
                generatedCouponEntryId = null,
                registrationsByEntry = emptyMap(),
                attendanceByEntry = emptyMap(),
                followerCount = null,
                ticket = null,
                ticketEventId = null,
                admissionMessage = null,
                registeredEventIds = emptySet(),
                justRegisteredEntryId = null
            )
        }
        loadingJob = viewModelScope.launch {
            try {
                val access = repository.access()
                val manager = access.active && community.id in access.communityIds
                val page = repository.entryPage(community.id, manager)
                val target = targetEventId?.takeIf { id -> page.first.none { it.id == id } }
                    ?.let { repository.publishedEntry(community.id, it) }
                val entries = if (target == null) page.first else page.first + target
                val following = repository.isFollowing(community.id)
                val events = entries.filter { it.data.kind == "event" && it.data.status == "published" }
                val registrations = if (manager) {
                    events.associate { it.id to repository.registrations(community.id, it.id) }
                } else emptyMap()
                val registered = if (manager) emptySet() else {
                    events.filter { repository.isRegistered(community.id, it.id) }.mapTo(mutableSetOf()) { it.id }
                }
                mutable.update {
                    it.copy(
                        access = access,
                        entries = entries,
                        entriesCursor = page.second,
                        isFollowing = following,
                        registrationsByEntry = registrations,
                        registeredEventIds = registered,
                        loading = false
                    )
                }
                if (manager && updatesVisible) startRegistrationUpdates(community.id)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun loadMoreEntries() {
        val snapshot = mutable.value
        val community = snapshot.selected ?: return
        val cursor = snapshot.entriesCursor ?: return
        if (snapshot.loading) return
        mutable.update { it.copy(loading = true) }
        loadingJob = viewModelScope.launch {
            try {
                val page = repository.entryPage(community.id, snapshot.canManage, cursor)
                val registered = if (snapshot.canManage) emptySet() else page.first.filter { repository.isRegistered(community.id, it.id) }.mapTo(mutableSetOf()) { it.id }
                mutable.update { it.copy(entries = (it.entries + page.first).distinctBy { entry -> entry.id }, entriesCursor = page.second, registeredEventIds = it.registeredEventIds + registered, loading = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun back() {
        loadingJob?.cancel()
        registrationJob?.cancel()
        mutable.update {
            it.copy(
                selected = null,
                error = null,
                loading = false,
                entries = emptyList(),
                generatedCouponCode = null,
                generatedCouponEntryId = null,
                registrationsByEntry = emptyMap(),
                registeredEventIds = emptySet(),
                justRegisteredEntryId = null
            )
        }
    }
    fun clearError() { mutable.update { it.copy(error = null) } }
    fun clearRegistrationFeedback() { mutable.update { it.copy(justRegisteredEntryId = null) } }
    fun setStudentEventVisible(entryId: String?) {
        visibleStudentEventId = entryId
        clearRegistrationFeedback()
    }

    fun blockCommunity(community: Community) {
        val blocked = mutable.value.blockedCommunityIds + community.id
        saveBlockedCommunityIds(blocked)
        com.good4.notification.PushSignals.refresh()
        back()
        mutable.update { state ->
            state.copy(
                blockedCommunityIds = blocked,
                communities = state.communities.filterNot { it.id == community.id },
                featuredEvents = state.featuredEvents.filterNot { it.community.id == community.id }
            )
        }
    }

    fun unblockAllCommunities() {
        saveBlockedCommunityIds(emptySet())
        com.good4.notification.PushSignals.refresh()
        featuredEventsKey = null
        load()
    }

    fun reportEntry(entry: CommunityEntry, reason: String, details: String) {
        val community = mutable.value.selected ?: return
        if (mutable.value.reportSending) return
        viewModelScope.launch {
            mutable.update { it.copy(reportSending = true, reportError = null, reportSentEntryId = null) }
            try {
                repository.reportContent(community, entry, reason, details)
                mutable.update { it.copy(reportSending = false, reportSentEntryId = entry.id) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutable.update {
                    it.copy(reportSending = false, reportError = e.message ?: "Bildirim gönderilemedi. Tekrar deneyin.")
                }
            }
        }
    }

    fun clearReportStatus() { mutable.update { it.copy(reportSentEntryId = null, reportError = null) } }
    fun pauseUpdates() { updatesVisible = false; registrationJob?.cancel() }
    fun resumeUpdates() {
        updatesVisible = true
        val state = mutable.value
        if (state.canManage && !state.loading) state.selected?.let { startRegistrationUpdates(it.id) }
    }
    fun reportError(message: String) { mutable.update { it.copy(error = message) } }
    fun clearAdmissionMessage() { mutable.update { it.copy(admissionMessage = null, error = null) } }
    fun closeTicket() { mutable.update { it.copy(ticket = null, ticketEventId = null) } }

    fun showTicket(entry: CommunityEntry) {
        val community = mutable.value.selected ?: return
        viewModelScope.launch {
            try {
                val ticket = repository.ticket(community.id, entry)
                if (mutable.value.selected?.id == community.id) mutable.update { it.copy(ticket = ticket, ticketEventId = entry.id, error = null) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { reportError(e.message ?: "Bilet yüklenemedi.") }
        }
    }

    fun admit(entry: CommunityEntry, userId: String? = null, scanned: String? = null, undo: Boolean = false) {
        val snapshot = mutable.value
        val community = snapshot.selected ?: return
        if (!snapshot.canManage || snapshot.admissionBusy) return
        viewModelScope.launch {
            mutable.update { it.copy(admissionBusy = true, admissionMessage = null, error = null) }
            try {
                val token = scanned?.let { parseEventTicket(it, community.id, entry.id) }
                val registrations = repository.registrations(community.id, entry.id)
                val student = if (token != null) registrations.singleOrNull { it.ticketToken == token }
                    else registrations.firstOrNull { it.userId == userId }
                check(student != null) { "Öğrencinin kaydı bulunamadı veya bilet iptal edilmiş." }
                val changed = admission.record(community.id, entry.id, student.userId, token, undo)
                mutable.update { it.copy(admissionMessage = when {
                    undo -> "${student.displayName}: giriş işareti geri alındı."
                    changed -> "${student.displayName}: giriş onaylandı."
                    else -> "${student.displayName}: bu öğrencinin girişi zaten yapılmış."
                }) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val detail = e.message.orEmpty()
                reportError(when {
                    "Bu bilet" in detail || "Bu bir Good4" in detail || "Bilet geçersiz" in detail || "Öğrencinin" in detail || "giriş almıyor" in detail -> detail.take(200)
                    else -> "Giriş onayı alınamadı. Bağlantınızı ve yönetim yetkinizi kontrol edip tekrar deneyin."
                })
            }
            finally { mutable.update { it.copy(admissionBusy = false) } }
        }
    }

    fun save(entryId: String?, entry: CommunityEntryDto, image: ByteArray?, onSaved: () -> Unit) = change(onSaved) { community ->
        val url = image?.let { uploadCommunityImage(it) } ?: entry.imageUrl
        repository.saveEntry(community.id, entryId, entry.copy(imageUrl = url))
    }
    fun cancel(entryId: String, onSaved: () -> Unit) = change(onSaved) { community -> repository.cancelEntry(community.id, entryId) }
    fun updateProfile(data: CommunityDto, logo: ByteArray?, cover: ByteArray?, onSaved: () -> Unit) = change(onSaved) { community ->
        val updated = data.copy(
            logoUrl = logo?.let { uploadCommunityImage(it) } ?: data.logoUrl,
            coverUrl = cover?.let { uploadCommunityImage(it) } ?: data.coverUrl
        )
        repository.saveCommunity(community.id, updated)
        mutable.update { state -> state.copy(selected = Community(community.id, updated), communities = state.communities.map { if (it.id == community.id) Community(it.id, updated) else it }) }
    }

    fun toggleFollow() {
        val community = mutable.value.selected ?: return
        if (mutable.value.followLoading) return
        val next = !mutable.value.isFollowing
        val uid = repository.currentUserId
        viewModelScope.launch {
            mutable.update { it.copy(followLoading = true, error = null) }
            try {
                repository.setFollowing(community.id, next)
                if (repository.currentUserId == uid) {
                    mutable.update { state -> state.copy(
                        isFollowing = if (state.selected?.id == community.id) next else state.isFollowing,
                        followLoading = false,
                        followedCommunityIds = if (next) state.followedCommunityIds + community.id else state.followedCommunityIds - community.id,
                    ) }
                    if (next) com.good4.notification.PushSignals.suggestPermission()
                    refreshFollowing(force = true)
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (repository.currentUserId == uid) {
                    mutable.update { it.copy(followLoading = false, error = "Takip tercihi kaydedilemedi. Tekrar deneyin.") }
                }
            }
        }
    }

    fun toggleRegistration(entry: CommunityEntry) {
        val community = mutable.value.selected ?: return
        if (entry.id in mutable.value.registrationLoadingIds) return
        val next = entry.id !in mutable.value.registeredEventIds
        val studentV2 = AppEnvironment.firebaseBackend == FirebaseBackend.V2 && !mutable.value.canManage
        viewModelScope.launch {
            mutable.update {
                it.copy(
                    registrationLoadingIds = it.registrationLoadingIds + entry.id,
                    cancellingRegistrationIds = if (next) it.cancellingRegistrationIds else it.cancellingRegistrationIds + entry.id,
                    error = null
                )
            }
            try {
                repository.setRegistration(community.id, entry, next)
                if (studentV2 && mutable.value.selected?.id != community.id) {
                    mutable.update { it.copy(registrationLoadingIds = it.registrationLoadingIds - entry.id, cancellingRegistrationIds = it.cancellingRegistrationIds - entry.id) }
                    return@launch
                }
                mutable.update {
                    if (studentV2) it.withStudentRegistration(entry.id, next, showFeedback = visibleStudentEventId == entry.id) else it.copy(
                        registeredEventIds = if (next) it.registeredEventIds + entry.id else it.registeredEventIds - entry.id,
                        registrationLoadingIds = it.registrationLoadingIds - entry.id,
                        cancellingRegistrationIds = it.cancellingRegistrationIds - entry.id
                    )
                }
                if (next) {
                    com.good4.notification.PushSignals.suggestPermission()
                    if (!studentV2) showTicket(entry)
                } else closeTicket()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutable.update { it.copy(registrationLoadingIds = it.registrationLoadingIds - entry.id, cancellingRegistrationIds = it.cancellingRegistrationIds - entry.id, error = e.message) }
            }
        }
    }

    fun createCouponCode(entry: CommunityEntry) {
        val community = mutable.value.selected ?: return
        if (mutable.value.codeGenerating) return
        viewModelScope.launch {
            mutable.update { it.copy(codeGenerating = true, generatedCouponCode = null, error = null) }
            try {
                val code = repository.createCouponCode(community.id, entry)
                mutable.update { it.copy(codeGenerating = false, generatedCouponCode = code, generatedCouponEntryId = entry.id) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(codeGenerating = false, error = e.message) } }
        }
    }

    private fun startRegistrationUpdates(communityId: String) {
        registrationJob?.cancel()
        registrationJob = viewModelScope.launch {
            launch {
                try {
                    admission.followers(communityId).collect { count -> mutable.update { it.copy(followerCount = count) } }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { reportError("Takipçi sayısı güncellenemiyor. Tekrar deneyin.") }
            }
            mutable.value.entries.filter { it.data.kind == "event" }.forEach { entry ->
                launch {
                    try {
                        admission.observe(communityId, entry.id).collect { snapshot ->
                            mutable.update { it.copy(registrationsByEntry = it.registrationsByEntry + (entry.id to snapshot.registrations),
                                attendanceByEntry = it.attendanceByEntry + (entry.id to snapshot.attendance)) }
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { reportError("Etkinlik girişleri güncellenemiyor. Tekrar deneyin.") }
                }
            }
        }
    }

    private fun change(onSaved: () -> Unit, action: suspend (Community) -> Unit) {
        val community = mutable.value.selected ?: return
        if (mutable.value.saving || !mutable.value.canManage) return
        viewModelScope.launch {
            mutable.update { it.copy(saving = true, error = null) }
            try {
                action(community)
                mutable.update { it.copy(saving = false) }
                onSaved()
                select(mutable.value.selected ?: community)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutable.update { it.copy(saving = false, error = communityErrorMessage(e)) }
            }
        }
    }

    private fun communityErrorMessage(error: Exception): String {
        val detail = error.message.orEmpty()
        return communityServerMessages[detail.trim()] ?: when {
            "unauthorized" in detail.lowercase() || "permission denied" in detail.lowercase() ->
                "Görsel yüklenemedi. Topluluk yönetici yetkinizi kontrol edip tekrar deneyin."
            detail.isBlank() || Regex("[A-Z_]+").matches(detail.trim()) -> "İşlem tamamlanamadı. Lütfen tekrar deneyin."
            else -> detail
        }
    }
}

/** Turkish copy for the error codes the community callables return. */
internal val communityServerMessages = mapOf(
    "TITLE_REQUIRED" to "Etkinlik başlığını girin.", "TITLE_INVALID" to "Başlık en fazla 120 karakter olabilir.",
    "DESCRIPTION_REQUIRED" to "Etkinlik açıklamasını girin.", "DESCRIPTION_INVALID" to "Etkinlik açıklamasını girin.",
    "LOCATION_REQUIRED" to "Etkinlik konumunu girin.", "LOCATION_INVALID" to "Etkinlik konumunu girin.",
    "EVENT_DATE_TIME_INVALID" to "Başlangıç ve bitiş zamanını kontrol edin.",
    "ENDDATE_REQUIRED" to "Bitiş tarihini seçin.", "ENDTIME_REQUIRED" to "Bitiş saatini seçin.",
    "EVENT_START_IN_PAST" to "Başlangıç geçmiş bir zaman olamaz.",
    "EVENT_END_BEFORE_START" to "Bitiş, başlangıçtan sonra olmalı.",
    "EVENT_DURATION_TOO_LONG" to "Etkinlik en fazla 14 gün sürebilir.",
    "EVENT_CATEGORY_INVALID" to "Etkinlik kategorisini seçin.",
    "CAPACITY_INVALID" to "Kontenjanı sayı olarak girin.",
    "EVENT_NOT_EDITABLE" to "İptal edilen etkinlik düzenlenemez.",
    "EVENT_ALREADY_PUBLISHED" to "Yayındaki etkinlik taslağa alınamaz.",
    "EVENT_NOT_FOUND" to "Etkinlik bulunamadı. Listeyi yenileyip tekrar deneyin.",
    "IMAGE_URL_INVALID" to "Kapak görseli kaydedilemedi. Başka bir görsel deneyin.",
    "COMMUNITY_IMAGE_INVALID" to "Görsel yüklenemedi. Görseli yeniden seçip tekrar deneyin.",
    "COMMUNITY_IMAGE_TYPE_INVALID" to "Görsel JPEG, PNG veya WebP biçiminde olmalı.",
    "COMMUNITY_IMAGE_SIZE_INVALID" to "Görsel boş olmamalı ve en fazla 5 MB olabilir.",
    "COMMUNITY_IMAGE_CONTENT_INVALID" to "Görsel dosyası geçersiz. Başka bir JPEG, PNG veya WebP görseli seçin.",
    "COMMUNITY_MANAGER_REQUIRED" to "Bu işlemi yalnızca topluluk yöneticisi yapabilir.",
    "COMMUNITY_MEMBERSHIP_NOT_FOUND" to "Aktif topluluk yönetim üyeliğiniz bulunamadı.",
    "ACCOUNT_NOT_ACTIVE" to "Bu işlem için hesabınız aktif olmalı.",
    "ROLE_NOT_ALLOWED" to "Bu işlem için topluluk yönetici yetkisi gerekiyor.",
    "COMMUNITY_MEMBERSHIP_REQUIRED" to "Bu topluluğu yönetme yetkiniz bulunmuyor.",
    "COMMUNITY_NOT_ACTIVE" to "Topluluk şu an aktif değil.",
)
