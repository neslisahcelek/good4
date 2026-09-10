package com.good4.community

import com.good4.auth.data.repository.AuthRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import kotlinx.serialization.Serializable

@Serializable
data class CommunityDto(val name: String = "", val description: String = "", val logoUrl: String = "")

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
    val status: String = "published"
)

data class Community(val id: String, val data: CommunityDto)
data class CommunityEntry(val id: String, val data: CommunityEntryDto)

class CommunityRepository(private val store: FirestoreRepository, private val auth: AuthRepository) {
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
        val normalized = entry.copy(status = if (entry.kind == "coupon") "pending" else "published")
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
}
