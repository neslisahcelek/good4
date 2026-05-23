package com.good4.notification.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationEventDto(
    @SerialName("type")
    val type: String,
    @SerialName("createdAt")
    val createdAt: Long,
    @SerialName("targetRole")
    val targetRole: String? = null,
    @SerialName("targetUserId")
    val targetUserId: String? = null,
    @SerialName("businessId")
    val businessId: String? = null,
    @SerialName("businessOwnerId")
    val businessOwnerId: String? = null,
    @SerialName("payload")
    val payload: Map<String, String> = emptyMap()
)

object NotificationEventTypes {
    const val BUSINESS_CREATED_PENDING = "business_created_pending"
    const val BUSINESS_APPROVED = "business_approved"
}
