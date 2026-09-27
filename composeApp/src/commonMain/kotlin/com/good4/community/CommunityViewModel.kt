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

    fun load() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = null) }
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
                        loading = false
                    )
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(loading = false, error = e.message) } }
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

    fun select(community: Community) {
        loadingJob?.cancel()
        registrationJob?.cancel()
        mutable.update {
            it.copy(
                selected = community,
                entries = emptyList(),
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
                registeredEventIds = emptySet()
            )
        }
        loadingJob = viewModelScope.launch {
            try {
                val access = repository.access()
                val manager = access.active && community.id in access.communityIds
                val entries = repository.entries(community.id, manager)
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
                registeredEventIds = emptySet()
            )
        }
    }
    fun clearError() { mutable.update { it.copy(error = null) } }

    fun blockCommunity(community: Community) {
        val blocked = mutable.value.blockedCommunityIds + community.id
        saveBlockedCommunityIds(blocked)
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
        val url = image?.let { uploadCommunityImage(community.id, it) } ?: entry.imageUrl
        repository.saveEntry(community.id, entryId, entry.copy(imageUrl = url))
    }
    fun cancel(entryId: String, onSaved: () -> Unit) = change(onSaved) { community -> repository.cancelEntry(community.id, entryId) }
    fun updateProfile(data: CommunityDto, logo: ByteArray?, cover: ByteArray?, onSaved: () -> Unit) = change(onSaved) { community ->
        val updated = data.copy(
            logoUrl = logo?.let { uploadCommunityImage(community.id, it) } ?: data.logoUrl,
            coverUrl = cover?.let { uploadCommunityImage(community.id, it) } ?: data.coverUrl
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
        viewModelScope.launch {
            mutable.update { it.copy(registrationLoadingIds = it.registrationLoadingIds + entry.id, error = null) }
            try {
                repository.setRegistration(community.id, entry, next)
                mutable.update {
                    it.copy(
                        registeredEventIds = if (next) it.registeredEventIds + entry.id else it.registeredEventIds - entry.id,
                        registrationLoadingIds = it.registrationLoadingIds - entry.id
                    )
                }
                if (next) showTicket(entry) else closeTicket()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutable.update { it.copy(registrationLoadingIds = it.registrationLoadingIds - entry.id, error = e.message) }
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
        return when {
            "unauthorized" in detail.lowercase() || "permission denied" in detail.lowercase() ->
                "Görsel yüklenemedi. Topluluk yönetici yetkinizi kontrol edip tekrar deneyin."
            detail.isBlank() -> "İşlem tamamlanamadı. Lütfen tekrar deneyin."
            else -> detail
        }
    }
}
