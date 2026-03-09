package com.good4.supportactivity.data.repository

import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.supportactivity.data.dto.SupportActivityDto
import com.good4.supportactivity.domain.SupportActivity
import com.good4.supportactivity.domain.SupportActivityStatus
import kotlinx.datetime.Instant

private const val COLLECTION = "supportActivities"

class SupportActivityRepository(
    private val firestoreRepository: FirestoreRepository
) {
    suspend fun getSupportActivity(id: String): Result<SupportActivity, Error> {
        return when (val result = firestoreRepository.getDocument(COLLECTION, id, SupportActivityDto::class)) {
            is Result.Success -> Result.Success(result.data.toSupportActivity(id))
            is Result.Error -> result
        }
    }

    suspend fun getAllSupportActivities(): Result<List<SupportActivity>, Error> {
        return when (val result = firestoreRepository.getCollectionWithIds(COLLECTION, SupportActivityDto::class)) {
            is Result.Success -> Result.Success(result.data.map { it.data.toSupportActivity(it.id) })
            is Result.Error -> result
        }
    }

    suspend fun getSupportActivitiesByCreator(creatorId: String): Result<List<SupportActivity>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds(
            collectionPath = COLLECTION,
            field = "creatorId",
            value = creatorId,
            clazz = SupportActivityDto::class
        )) {
            is Result.Success -> Result.Success(result.data.map { it.data.toSupportActivity(it.id) })
            is Result.Error -> result
        }
    }

    suspend fun getSupportActivitiesByStatus(status: SupportActivityStatus): Result<List<SupportActivity>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds(
            collectionPath = COLLECTION,
            field = "status",
            value = status.value,
            clazz = SupportActivityDto::class
        )) {
            is Result.Success -> Result.Success(result.data.map { it.data.toSupportActivity(it.id) })
            is Result.Error -> result
        }
    }

    suspend fun getSupportActivitiesByBusiness(businessId: String): Result<List<SupportActivity>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds(
            collectionPath = COLLECTION,
            field = "targetBusinessId",
            value = businessId,
            clazz = SupportActivityDto::class
        )) {
            is Result.Success -> Result.Success(result.data.map { it.data.toSupportActivity(it.id) })
            is Result.Error -> result
        }
    }

    suspend fun createSupportActivity(dto: SupportActivityDto): Result<String, Error> {
        return firestoreRepository.addDocument(COLLECTION, dto)
    }

    suspend fun updateSupportActivity(id: String, dto: SupportActivityDto): Result<Unit, Error> {
        return firestoreRepository.updateDocument(COLLECTION, id, dto)
    }

    suspend fun deleteSupportActivity(id: String): Result<Unit, Error> {
        return firestoreRepository.deleteDocument(COLLECTION, id)
    }
}

private fun SupportActivityDto.toSupportActivity(id: String): SupportActivity {
    return SupportActivity(
        id = id,
        createdAt = createdAt?.let { Instant.fromEpochSeconds(it) },
        creatorId = creatorId ?: "",
        currentCount = currentCount ?: 0,
        description = description ?: "",
        endDate = endDate?.let { Instant.fromEpochSeconds(it) },
        shareId = shareId ?: "",
        shareLink = shareLink ?: "",
        startDate = startDate?.let { Instant.fromEpochSeconds(it) },
        status = SupportActivityStatus.fromValue(status),
        targetBusinessId = targetBusinessId ?: "",
        targetCount = targetCount ?: 0,
        title = title ?: "",
        type = type ?: ""
    )
}
