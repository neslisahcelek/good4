package com.good4.code.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class CodeDto(
    @SerialName("value")
    val value: String? = null,
    @SerialName("businessId")
    val businessId: String? = null,
    @SerialName("productId")
    val productId: String? = null,
    @SerialName("userId")
    val userId: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("createdAt")
    val createdAt: Instant? = null,
    @SerialName("usedAt")
    val usedAt: Instant? = null
)


