package com.good4.notification.data.repository

import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.notification.data.dto.NotificationEventDto
import com.good4.notification.data.dto.NotificationEventTypes
import com.good4.user.domain.UserRole

class NotificationEventRepository(
    private val firestoreRepository: FirestoreRepository
) {
    suspend fun publishBusinessCreatedPendingEvent(
        businessId: String,
        ownerUserId: String,
        businessName: String,
        createdAt: Long
    ): Result<Unit, Error> {
        val event = NotificationEventDto(
            type = NotificationEventTypes.BUSINESS_CREATED_PENDING,
            createdAt = createdAt,
            targetRole = UserRole.ADMIN.value,
            businessId = businessId,
            businessOwnerId = ownerUserId,
            payload = mapOf("businessName" to businessName)
        )
        return when (val result = firestoreRepository.addDocument("notificationEvents", event)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.error)
        }
    }

    suspend fun publishBusinessApprovedEvent(
        businessId: String,
        ownerUserId: String,
        approvedBy: String,
        approvedAt: Long
    ): Result<Unit, Error> {
        val event = NotificationEventDto(
            type = NotificationEventTypes.BUSINESS_APPROVED,
            createdAt = approvedAt,
            targetUserId = ownerUserId,
            businessId = businessId,
            businessOwnerId = ownerUserId,
            payload = mapOf("approvedBy" to approvedBy)
        )
        return when (val result = firestoreRepository.addDocument("notificationEvents", event)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.error)
        }
    }
}
