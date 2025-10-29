package com.good4.product.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class ProductDto(
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String,
    @SerialName("count")
    val count: Int,
    @SerialName("businessId")
    val businessId: String,
    @SerialName("createdAt")
    val createdDate: Instant,
    @SerialName("discountPrice")
    val discountPrice: Int,
    @SerialName("originalPrice")
    val originalPrice: Int,
    @SerialName("image")
    val imageUrl: String,
)