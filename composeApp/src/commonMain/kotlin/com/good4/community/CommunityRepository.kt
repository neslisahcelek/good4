package com.good4.community

import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import com.good4.core.network.callV2Function
import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random
import com.good4.user.data.dto.UserDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Serializable
data class CommunityDto(
    val name: String = "",
    val university: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val coverUrl: String = ""
)

@Serializable
data class CommunityAccessDto(val communityIds: List<String> = emptyList(), val active: Boolean = false)

@Serializable
data class V2OrganizationDto(
    val name: String = "", val type: String = "", val status: String = "",
    val university: String = "", val description: String = "", val logoUrl: String = "", val coverUrl: String = ""
)

@Serializable
data class V2MembershipDto(val userId: String = "", val role: String = "", val status: String = "")

@Serializable
data class V2UserRoleDto(val role: String = "")

@Serializable
data class V2EventDto(
    val organizationId: String = "", val title: String = "", val description: String = "",
    val startsAt: Long = 0, val endsAt: Long = 0, val timezone: String = "Europe/Istanbul",
    val location: String = "", val imageUrl: String = "", val capacity: Int = 0,
    val registrationCount: Int = 0, val attendanceCount: Int = 0, val status: String = "published",
    val categoryId: String = ""
)

@Serializable
data class V2EventRegistrationDto(
    val eventId: String = "", val organizationId: String = "", val userId: String = "",
    val displayName: String = "", val status: String = "registered", val registeredAt: Long = 0, val updatedAt: Long = 0
)

@Serializable
data class CommunityEntryDto(
    val kind: String = "event",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    // V2 events only; blank for coupons and older entries.
    val endDate: String = "",
    val endTime: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val code: String = "",
    val businessId: String = "",
    val discountType: String = "percentage",
    val discountValue: Int = 0,
    val capacity: Int = 0,
    val totalLimit: Int = 0,
    val perUserLimit: Int = 1,
    val status: String = "published",
    val categoryId: String = "",
    val registrationCount: Int = 0
)

@Serializable
data class CommunityFollowDto(val userId: String, val followedAt: Long)

@Serializable
data class CommunityEventRegistrationDto(
    val userId: String,
    val displayName: String = "",
    val registeredAt: Long,
    val ticketToken: String = ""
)

@Serializable
data class CommunityCouponClaimDto(
    val value: String,
    val userId: String,
    val businessId: String,
    val status: String,
    val createdAt: Long
)

@Serializable
data class CommunityCouponCodeDto(
    val value: String,
    val communityId: String,
    val entryId: String,
    val userId: String,
    val businessId: String,
    val title: String,
    val expiresOn: String,
    val status: String,
    val createdAt: Long,
    val usedAt: Long?
)

data class CommunityBusiness(val id: String, val name: String)

data class Community(val id: String, val data: CommunityDto)
data class CommunityEntry(val id: String, val data: CommunityEntryDto)
data class CommunityFeaturedEvent(val community: Community, val entry: CommunityEntry)

class CommunityRepository(
    private val store: FirestoreRepository,
    private val auth: AuthRepository,
    private val businesses: FirestoreBusinessRepository? = null
) {
    val currentUserId: String? get() = auth.currentUser?.uid
    val authStateFlow get() = auth.authStateFlow
    private val isV2 get() = AppEnvironment.firebaseBackend == FirebaseBackend.V2
    private val feedback by lazy { com.good4.feedback.FeedbackRepository(store, auth) }

    /** Sends a content report to the Good4 team through the feedback inbox. */
    suspend fun reportContent(community: Community, entry: CommunityEntry, reason: String, details: String) {
        val subject = "İçerik bildirimi: ${entry.data.title}".take(120)
        val message = buildString {
            appendLine("Neden: $reason")
            appendLine("Topluluk: ${community.data.name} (${community.id})")
            appendLine("İçerik: ${entry.data.title} (${entry.data.kind}, ${entry.id})")
            if (details.isNotBlank()) appendLine("Açıklama: ${details.trim()}")
        }.take(2000)
        feedback.submit(
            subject, message,
            reportCommunityId = community.id,
            reportEntryId = entry.id,
            reportEntryKind = entry.data.kind
        )
    }

    private val communityCache = com.good4.core.data.repository.ReadCache<List<Community>>(900)
    suspend fun list(): List<Community> = communityCache.load { loadCommunities() }
    private suspend fun loadCommunities(): List<Community> {
        if (isV2) return when (val result = allPages("organizations", mapOf("type" to "community", "status" to "active"), V2OrganizationDto::class)) {
            is Result.Success -> result.data.map {
                Community(it.id, CommunityDto(it.data.name, it.data.university, it.data.description, it.data.logoUrl, it.data.coverUrl))
            }.sortedBy { it.data.name }
            is Result.Error -> error("Topluluklar yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.")
        }
        return when (val result = store.getCollectionWithIds("communities", CommunityDto::class)) {
            is Result.Success -> result.data.map { Community(it.id, it.data) }.sortedBy { it.data.name }
            is Result.Error -> error("Topluluklar yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.")
        }
    }

    private suspend fun <T : Any> allPages(path: String, conditions: Map<String, Any>, clazz: kotlin.reflect.KClass<T>): Result<List<com.good4.core.data.repository.DocumentWithId<T>>, com.good4.core.domain.Error> {
        val items = mutableListOf<com.good4.core.data.repository.DocumentWithId<T>>()
        var cursor: String? = null
        do {
            when (val page = store.queryPage(path, conditions, clazz, cursor = cursor)) {
                is Result.Error -> return page
                is Result.Success -> { items.addAll(page.data.items); cursor = page.data.nextCursor }
            }
        } while (cursor != null)
        return Result.Success(items)
    }

    suspend fun access(): CommunityAccessDto {
        val user = auth.currentUser ?: return CommunityAccessDto()
        if (!user.isEmailVerified) return CommunityAccessDto()
        if (isV2) {
            // Only these roles can hold a community membership, so ordinary students skip the
            // per-organization membership reads. If the role cannot be read, fall back to the scan.
            val role = (store.getDocument("users", user.uid, V2UserRoleDto::class) as? Result.Success)?.data?.role
            if (role != null && role !in listOf("communityManager", "communityStaff", "good4Admin")) {
                return CommunityAccessDto()
            }
            val organizations = list()
            val managed = organizations.mapNotNull { organization ->
                val member = store.getDocument(
                    "organizations/${organization.id}/members", user.uid, V2MembershipDto::class
                )
                val data = (member as? Result.Success)?.data
                organization.id.takeIf { data?.status == "active" && data.role in listOf("manager", "staff") }
            }
            return CommunityAccessDto(managed, managed.isNotEmpty())
        }
        val email = user.email ?: return CommunityAccessDto()
        return when (val result = store.getDocument("community_access", email, CommunityAccessDto::class)) {
            is Result.Success -> result.data
            is Result.Error -> CommunityAccessDto()
        }
    }

    suspend fun businesses(): List<CommunityBusiness> {
        if (isV2) return when (val result = allPages("organizations", mapOf("type" to "business", "status" to "active"), V2OrganizationDto::class)) {
            is Result.Success -> result.data.map { CommunityBusiness(it.id, it.data.name) }.sortedBy { it.name }
            is Result.Error -> emptyList()
        }
        return when (val result = businesses?.getBusinessesWithIds()) {
            is Result.Success -> result.data.map { CommunityBusiness(it.id, it.data.name) }.sortedBy { it.name }
            else -> emptyList()
        }
    }

    suspend fun publishedEntry(communityId: String, eventId: String): CommunityEntry? {
        if (!isV2) return entries(communityId, false).firstOrNull { it.id == eventId && it.data.status == "published" }
        return when (val result = store.getDocument("events", eventId, V2EventDto::class)) {
            is Result.Success -> result.data.takeIf { it.organizationId == communityId && it.status == "published" }
                ?.let { mapV2Event(com.good4.core.data.repository.DocumentWithId(eventId, it)) }
            is Result.Error -> null
        }
    }

    suspend fun entryPage(id: String, manager: Boolean, cursor: String? = null): Pair<List<CommunityEntry>, String?> {
        if (!isV2) return entries(id, manager) to null
        val conditions: Map<String, Any> = if (manager) mapOf("organizationId" to id) else mapOf("organizationId" to id, "status" to "published")
        return when (val result = store.queryPage("events", conditions, V2EventDto::class, cursor = cursor)) {
            is Result.Success -> result.data.items.map(::mapV2Event) to result.data.nextCursor
            is Result.Error -> error("Etkinlikler yüklenemedi. Tekrar deneyin.")
        }
    }

    suspend fun entries(id: String, manager: Boolean): List<CommunityEntry> {
        if (isV2) {
            val result = if (manager) allPages("events", mapOf("organizationId" to id), V2EventDto::class)
            else allPages("events", mapOf("organizationId" to id, "status" to "published"), V2EventDto::class)
            return when (result) {
                is Result.Success -> result.data.map(::mapV2Event).sortedBy { it.data.date + it.data.time }
                is Result.Error -> error("Etkinlikler yüklenemedi. Tekrar deneyin.")
            }
        }
        val path = "communities/$id/entries"
        val result = if (manager) store.getCollectionWithIds(path, CommunityEntryDto::class)
        else store.queryCollectionWithIds(path, "status", "published", CommunityEntryDto::class)
        return when (result) {
            is Result.Success -> result.data.map { CommunityEntry(it.id, it.data) }.sortedBy { it.data.date + it.data.time }
            is Result.Error -> error("İçerikler yüklenemedi. Tekrar deneyin.")
        }
    }

    suspend fun featuredEvents(communities: List<Community>, today: String): List<CommunityFeaturedEvent> {
        if (communities.isEmpty()) return emptyList()
        val todayDate = runCatching { LocalDate.parse(today) }.getOrNull() ?: return emptyList()
        val communityById = communities.associateBy { it.id }
        val candidates = if (isV2) {
            when (val result = store.queryPage("events", mapOf("status" to "published"), V2EventDto::class, pageSize = 20, minimumTimestamp = "endsAt" to Clock.System.now().epochSeconds)) {
                is Result.Success -> result.data.items.mapNotNull { document ->
                    val community = communityById[document.data.organizationId] ?: return@mapNotNull null
                    CommunityFeaturedEvent(community, mapV2Event(document))
                }
                is Result.Error -> error("Etkinlikler yüklenemedi. Tekrar deneyin.")
            }
        } else {
            val fetched = coroutineScope {
                communities.map { community ->
                    async {
                        try {
                            community to entries(community.id, manager = false)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            community to null
                        }
                    }
                }.awaitAll()
            }
            if (fetched.isNotEmpty() && fetched.all { it.second == null }) {
                error("Etkinlikler yüklenemedi. Tekrar deneyin.")
            }
            fetched.flatMap { (community, entries) ->
                entries.orEmpty().map { CommunityFeaturedEvent(community, it) }
            }
        }

        return candidates.asSequence()
            .filter { featured ->
                val data = featured.entry.data
                if (data.kind != "event" || data.status != "published" || data.imageUrl.isBlank()) return@filter false
                // Multi-day events stay featured until their last day.
                val lastDate = runCatching { LocalDate.parse(data.lastDate()) }.getOrNull() ?: return@filter false
                lastDate >= todayDate
            }
            .sortedWith(compareBy({ it.entry.data.date }, { it.entry.data.time }))
            .toList()
    }

    private fun mapV2Event(document: com.good4.core.data.repository.DocumentWithId<V2EventDto>): CommunityEntry {
        val event = document.data
        val zone = TimeZone.of(event.timezone.ifBlank { "Europe/Istanbul" })
        val local = Instant.fromEpochSeconds(event.startsAt).toLocalDateTime(zone)
        val end = event.endsAt.takeIf { it > event.startsAt }?.let { Instant.fromEpochSeconds(it).toLocalDateTime(zone) }
        fun hhmm(hour: Int, minute: Int) = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        return CommunityEntry(document.id, CommunityEntryDto(
            kind = "event",
            title = event.title,
            description = event.description,
            date = local.date.toString(),
            time = hhmm(local.hour, local.minute),
            endDate = end?.date?.toString().orEmpty(),
            endTime = end?.let { hhmm(it.hour, it.minute) }.orEmpty(),
            location = event.location,
            imageUrl = event.imageUrl,
            capacity = event.capacity,
            registrationCount = event.registrationCount,
            status = event.status,
            categoryId = event.categoryId
        ))
    }

    private suspend fun requireManager(id: String) {
        val access = access()
        check(access.active && id in access.communityIds) { "Bu topluluğu yönetme yetkiniz bulunmuyor." }
    }

    suspend fun saveEntry(communityId: String, entryId: String?, entry: CommunityEntryDto) {
        requireManager(communityId)
        if (isV2) {
            check(entry.kind == "event") { "Kuponlar V2'ye taşınana kadar eski panelden yönetilmelidir." }
            callV2Function("saveCommunityPortalEntry", buildJsonObject {
                put("kind", "event"); entryId?.let { put("entryId", it) }; put("title", entry.title)
                put("description", entry.description); put("date", entry.date); put("time", entry.time)
                if (entry.endDate.isNotBlank() && entry.endTime.isNotBlank()) {
                    put("endDate", entry.endDate); put("endTime", entry.endTime)
                }
                put("location", entry.location); put("capacity", entry.capacity)
                put("categoryId", entry.categoryId)
                put("status", if (entry.status == "draft") "draft" else "published")
                put("imageUrl", entry.imageUrl)
            })
            return
        }
        val normalized = entry.copy(
            code = if (entry.kind == "coupon") "" else entry.code,
            status = if (entry.kind == "coupon") "pending"
            else entry.status.takeIf { it == "draft" } ?: "published"
        )
        val result = if (entryId == null) store.addDocument("communities/$communityId/entries", normalized)
        else store.updateDocument("communities/$communityId/entries", entryId, normalized)
        check(result is Result.Success) { "Kaydedilemedi. Lütfen tekrar deneyin." }
    }

    suspend fun cancelEntry(communityId: String, entryId: String) {
        requireManager(communityId)
        if (isV2) {
            callV2Function("cancelCommunityPortalEntry", buildJsonObject { put("entryId", entryId) })
            return
        }
        check(store.updateFields("communities/$communityId/entries", entryId, mapOf("status" to "cancelled")) is Result.Success) { "İptal edilemedi. Tekrar deneyin." }
    }

    suspend fun saveCommunity(id: String, data: CommunityDto) {
        requireManager(id)
        check(!isV2) { "Topluluk profili V2 yönetim panelinden güncellenmelidir." }
        check(store.updateDocument("communities", id, data) is Result.Success) { "Bilgiler kaydedilemedi. Tekrar deneyin." }
    }

    suspend fun isFollowing(communityId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val root = if (isV2) "organizations" else "communities"
        return store.getDocument("$root/$communityId/followers", userId, CommunityFollowDto::class) is Result.Success
    }

    suspend fun followingCommunityIds(): Set<String> {
        if (auth.currentUser == null) return emptySet()
        check(isV2) { "Toplu takip filtresi yalnızca V2'de kullanılabilir." }
        val response = callV2Function("getFollowingCommunityIds", buildJsonObject {})
        val ids = response["communityIds"]?.jsonArray ?: error("Takip ettiğiniz topluluklar yüklenemedi.")
        return ids.map { it.jsonPrimitive.content }.toSet()
    }

    suspend fun setFollowing(communityId: String, following: Boolean) {
        val userId = auth.currentUser?.uid ?: error("Takip etmek için giriş yapmalısınız.")
        if (isV2) {
            callV2Function("setCommunityFollowing", buildJsonObject {
                put("communityId", communityId); put("following", following)
            })
            return
        }
        val result = if (following) {
            store.updateDocument(
                "communities/$communityId/followers",
                userId,
                CommunityFollowDto(userId, Clock.System.now().epochSeconds)
            )
        } else {
            store.deleteDocument("communities/$communityId/followers", userId)
        }
        check(result is Result.Success) { "Takip tercihi kaydedilemedi. Tekrar deneyin." }
    }

    suspend fun isRegistered(communityId: String, entryId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        if (isV2) return when (val result = store.queryCollectionWithIds(
            "events/$entryId/registrations", "userId", userId, V2EventRegistrationDto::class
        )) {
            is Result.Success -> result.data.isNotEmpty()
            is Result.Error -> false
        }
        return store.getDocument(
            "communities/$communityId/entries/$entryId/registrations",
            userId,
            CommunityEventRegistrationDto::class
        ) is Result.Success
    }

    suspend fun registrations(communityId: String, entryId: String): List<CommunityEventRegistrationDto> {
        requireManager(communityId)
        if (isV2) return when (val result = store.getCollectionWithIds(
            "events/$entryId/registrations", V2EventRegistrationDto::class
        )) {
            is Result.Success -> result.data.map {
                CommunityEventRegistrationDto(it.data.userId, it.data.displayName, it.data.registeredAt, it.id)
            }.sortedBy { it.registeredAt }
            is Result.Error -> error("Etkinlik kayıtları yüklenemedi.")
        }
        return when (val result = store.getCollectionWithIds(
            "communities/$communityId/entries/$entryId/registrations",
            CommunityEventRegistrationDto::class
        )) {
            is Result.Success -> result.data.map { it.data }.sortedBy { it.registeredAt }
            is Result.Error -> error("Etkinlik kayıtları yüklenemedi.")
        }
    }

    suspend fun setRegistration(communityId: String, entry: CommunityEntry, registered: Boolean) {
        val user = auth.currentUser ?: error("Etkinliğe katılmak için giriş yapmalısınız.")
        check(entry.data.kind == "event" && entry.data.status == "published") { "Bu etkinlik kayıt almıyor." }
        if (isV2) {
            callV2Function("setEventRegistration", buildJsonObject {
                put("eventId", entry.id); put("registered", registered)
            })
            return
        }
        val path = "communities/$communityId/entries/${entry.id}/registrations"
        if (!registered) {
            check(store.getDocument("communities/$communityId/entries/${entry.id}/attendance", user.uid, EventAttendanceDto::class) !is Result.Success) {
                "Giriş yaptığınız etkinliğin kaydını iptal edemezsiniz. Gerekirse topluluk yöneticisine başvurun."
            }
        }
        val existing = store.getDocument(path, user.uid, CommunityEventRegistrationDto::class)
        if (registered && existing !is Result.Success && entry.data.capacity > 0) {
            val current = store.getCollectionWithIds(path, CommunityEventRegistrationDto::class)
            check(current is Result.Success) { "Kontenjan bilgisi kontrol edilemedi. Tekrar deneyin." }
            check(current.data.size < entry.data.capacity) { "Bu etkinliğin kontenjanı doldu." }
        }
        val profile = if (registered) store.getDocument("users", user.uid, UserDto::class) else null
        val name = (profile as? Result.Success)?.data?.fullName?.takeIf { it.isNotBlank() }
            ?: user.displayName.orEmpty().ifBlank { "Good4 öğrencisi" }
        val result = if (registered) {
            store.updateDocument(
                path,
                user.uid,
                CommunityEventRegistrationDto(
                    userId = user.uid,
                    displayName = name,
                    registeredAt = (existing as? Result.Success)?.data?.registeredAt ?: Clock.System.now().epochSeconds,
                    ticketToken = (existing as? Result.Success)?.data?.ticketToken?.takeIf { it.isNotBlank() } ?: newEventTicketToken()
                )
            )
        } else {
            store.deleteDocument(path, user.uid)
        }
        check(result is Result.Success) { "Etkinlik kaydı güncellenemedi. Tekrar deneyin." }
    }

    suspend fun ticket(communityId: String, entry: CommunityEntry): CommunityEventRegistrationDto {
        val uid = auth.currentUser?.uid ?: error("Giriş yapmalısınız.")
        if (isV2) {
            val result = store.queryCollectionWithIds(
                "events/${entry.id}/registrations", "userId", uid, V2EventRegistrationDto::class
            )
            val registration = (result as? Result.Success)?.data?.firstOrNull()
                ?: error("Önce etkinliğe kayıt olmalısınız.")
            return CommunityEventRegistrationDto(
                registration.data.userId, registration.data.displayName, registration.data.registeredAt, registration.id
            )
        }
        val path = "communities/$communityId/entries/${entry.id}/registrations"
        val first = store.getDocument(path, uid, CommunityEventRegistrationDto::class)
        check(first is Result.Success) { "Önce etkinliğe kayıt olmalısınız." }
        if (first.data.ticketToken.isBlank()) setRegistration(communityId, entry, true)
        val result = store.getDocument(path, uid, CommunityEventRegistrationDto::class)
        check(result is Result.Success) { "Bilet yüklenemedi." }
        return result.data
    }

    suspend fun createCouponCode(communityId: String, entry: CommunityEntry): String {
        check(!isV2) { "Kuponlar V2'ye henüz taşınmadı." }
        val userId = auth.currentUser?.uid ?: error("Kod oluşturmak için giriş yapmalısınız.")
        check(entry.data.kind == "coupon" && entry.data.status == "published") { "Bu kupon henüz kullanıma açık değil." }
        check(entry.data.businessId.isNotBlank()) { "Kupon için doğrulama yapacak işletme seçilmemiş." }
        val claimPath = "communities/$communityId/entries/${entry.id}/claims"
        val existing = store.getDocument(claimPath, userId, CommunityCouponClaimDto::class)
        if (existing is Result.Success) {
            if (existing.data.status == "pending") return existing.data.value
            error("Bu kuponu daha önce kullandınız.")
        }
        repeat(12) {
            val value = Random.nextInt(100000, 1000000).toString()
            if (store.getDocument("community_coupon_codes", value, CommunityCouponCodeDto::class) is Result.Error) {
                val code = CommunityCouponCodeDto(
                    value = value,
                    communityId = communityId,
                    entryId = entry.id,
                    userId = userId,
                    businessId = entry.data.businessId,
                    title = entry.data.title,
                    expiresOn = entry.data.date,
                    status = "pending",
                    createdAt = Clock.System.now().epochSeconds,
                    usedAt = null
                )
                check(store.updateDocument("community_coupon_codes", value, code) is Result.Success) {
                    "Kod oluşturulamadı. Lütfen tekrar deneyin."
                }
                check(store.updateDocument(
                    claimPath,
                    userId,
                    CommunityCouponClaimDto(value, userId, entry.data.businessId, "pending", Clock.System.now().epochSeconds)
                ) is Result.Success) { "Kupon hakkı kaydedilemedi. Lütfen tekrar deneyin." }
                return value
            }
        }
        error("Benzersiz kod oluşturulamadı. Lütfen tekrar deneyin.")
    }

    suspend fun verifyCouponCode(value: String, businessId: String): CommunityCouponCodeDto? {
        check(!isV2) { "Kupon doğrulama V2'ye henüz taşınmadı." }
        val result = store.getDocument("community_coupon_codes", value, CommunityCouponCodeDto::class)
        val code = (result as? Result.Success)?.data ?: return null
        if (code.businessId != businessId || code.status != "pending") return null
        val today = Clock.System.now().toString().substringBefore('T')
        if (runCatching { LocalDate.parse(code.expiresOn) }.isFailure || code.expiresOn < today) return null
        val used = store.updateFields(
            "community_coupon_codes",
            value,
            mapOf("status" to "used", "usedAt" to Clock.System.now().epochSeconds)
        )
        if (used is Result.Success) {
            store.updateFields(
                "communities/${code.communityId}/entries/${code.entryId}/claims",
                code.userId,
                mapOf("status" to "used")
            )
            return code
        }
        return null
    }
}
