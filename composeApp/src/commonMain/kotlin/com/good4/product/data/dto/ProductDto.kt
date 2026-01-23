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
    @SerialName("businessId")
    val businessId: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("discountPrice")
    val discountPrice: Int? = null,
    @SerialName("originalPrice")
    val originalPrice: Int? = null,
    @SerialName("image")
    val image: String? = null,
    @SerialName("imageUrl")
    val imageUrl: String? = null
)
