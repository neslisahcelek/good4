package com.good4.supportactivity.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupportActivityDto(
    @SerialName("createdAt")
    val createdAt: Long? = null,
    @SerialName("creatorId")
    val creatorId: String? = null,
    @SerialName("currentCount")
    val currentCount: Int? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("endDate")
    val endDate: Long? = null,
    @SerialName("shareId")
    val shareId: String? = null,
    @SerialName("shareLink")
    val shareLink: String? = null,
    @SerialName("startDate")
    val startDate: Long? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("targetBusinessId")
    val targetBusinessId: String? = null,
    @SerialName("targetCount")
    val targetCount: Int? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("type")
    val type: String? = null
)
