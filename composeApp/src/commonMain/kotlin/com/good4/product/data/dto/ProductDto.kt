package com.good4.product.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    @SerialName("name")
    val name: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("count")
    val count: Int? = null,
    @SerialName("pendingCount")
    val pendingCount: Int? = null,
    @SerialName("businessId")
    val businessId: String? = null,
    @SerialName("createdAt")
    val createdAt: Long? = null,
    @SerialName("discountPrice")
    val discountPrice: Int? = null,
    @SerialName("originalPrice")
    val originalPrice: Int? = null,
    @SerialName("image")
    val imageUrl: String? = null,
    @SerialName("foodType")
    val foodType: String? = null,
    @SerialName("totalDelivered")
    val totalDelivered: Int? = null,
    @SerialName("totalSuspended")
    val totalSuspended: Int? = null
)
