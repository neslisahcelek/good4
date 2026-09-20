package com.good4.community

import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class CommunityDto(
    val name: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val coverUrl: String = ""
)

@Serializable
data class CommunityAccessDto(val communityIds: List<String> = emptyList(), val active: Boolean = false)

@Serializable
data class CommunityEntryDto(
    val kind: String = "event",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val code: String = "",
    val businessId: String = "",
    val discountType: String = "percentage",
    val discountValue: Int = 0,
    val totalLimit: Int = 0,
    val perUserLimit: Int = 1,
    val status: String = "published"
)

@Serializable
data class CommunityFollowDto(val userId: String, val followedAt: Long)

@Serializable
data class CommunityEventRegistrationDto(
    val userId: String,
    val displayName: String = "",
    val registeredAt: Long
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

class CommunityRepository(
    private val store: FirestoreRepository,
    private val auth: AuthRepository,
    private val businesses: FirestoreBusinessRepository? = null
) {
    suspend fun list(): List<Community> = when (val result = store.getCollectionWithIds("communities", CommunityDto::class)) {
        is Result.Success -> result.data.map { Community(it.id, it.data) }.sortedBy { it.data.name }
        is Result.Error -> error("Topluluklar yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.")
    }

    suspend fun access(): CommunityAccessDto {
        val user = auth.currentUser ?: return CommunityAccessDto()
        if (!user.isEmailVerified) return CommunityAccessDto()
        val email = user.email ?: return CommunityAccessDto()
        return when (val result = store.getDocument("community_access", email, CommunityAccessDto::class)) {
            is Result.Success -> result.data
            is Result.Error -> CommunityAccessDto()
        }
    }

    suspend fun businesses(): List<CommunityBusiness> = when (val result = businesses?.getBusinessesWithIds()) {
        is Result.Success -> result.data.map { CommunityBusiness(it.id, it.data.name) }.sortedBy { it.name }
        else -> emptyList()
    }

    suspend fun entries(id: String, manager: Boolean): List<CommunityEntry> {
        val path = "communities/$id/entries"
        val result = if (manager) store.getCollectionWithIds(path, CommunityEntryDto::class)
        else store.queryCollectionWithIds(path, "status", "published", CommunityEntryDto::class)
        return when (result) {
            is Result.Success -> result.data.map { CommunityEntry(it.id, it.data) }.sortedBy { it.data.date + it.data.time }
            is Result.Error -> error("İçerikler yüklenemedi. Tekrar deneyin.")
        }
    }

    private suspend fun requireManager(id: String) {
        val access = access()
        check(access.active && id in access.communityIds) { "Bu topluluğu yönetme yetkiniz bulunmuyor." }
    }

    suspend fun saveEntry(communityId: String, entryId: String?, entry: CommunityEntryDto) {
        requireManager(communityId)
        val normalized = entry.copy(
            code = if (entry.kind == "coupon") "" else entry.code,
            status = if (entry.kind == "coupon") "pending" else "published"
        )
        val result = if (entryId == null) store.addDocument("communities/$communityId/entries", normalized)
        else store.updateDocument("communities/$communityId/entries", entryId, normalized)
        check(result is Result.Success) { "Kaydedilemedi. Lütfen tekrar deneyin." }
    }

    suspend fun cancelEntry(communityId: String, entryId: String) {
        requireManager(communityId)
        check(store.updateFields("communities/$communityId/entries", entryId, mapOf("status" to "cancelled")) is Result.Success) { "İptal edilemedi. Tekrar deneyin." }
    }

    suspend fun saveCommunity(id: String, data: CommunityDto) {
        requireManager(id)
        check(store.updateDocument("communities", id, data) is Result.Success) { "Bilgiler kaydedilemedi. Tekrar deneyin." }
    }

    suspend fun isFollowing(communityId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return store.getDocument("communities/$communityId/followers", userId, CommunityFollowDto::class) is Result.Success
    }

    suspend fun setFollowing(communityId: String, following: Boolean) {
        val userId = auth.currentUser?.uid ?: error("Takip etmek için giriş yapmalısınız.")
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
        return store.getDocument(
            "communities/$communityId/entries/$entryId/registrations",
            userId,
            CommunityEventRegistrationDto::class
        ) is Result.Success
    }

    suspend fun registrations(communityId: String, entryId: String): List<CommunityEventRegistrationDto> {
        requireManager(communityId)
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
        val path = "communities/$communityId/entries/${entry.id}/registrations"
        val result = if (registered) {
            store.updateDocument(
                path,
                user.uid,
                CommunityEventRegistrationDto(
                    userId = user.uid,
                    displayName = user.displayName.orEmpty().ifBlank { "Good4 öğrencisi" },
                    registeredAt = Clock.System.now().epochSeconds
                )
            )
        } else {
            store.deleteDocument(path, user.uid)
        }
        check(result is Result.Success) { "Etkinlik kaydı güncellenemedi. Tekrar deneyin." }
    }

    suspend fun createCouponCode(communityId: String, entry: CommunityEntry): String {
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
