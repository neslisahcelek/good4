package com.good4.order.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderItemDto(
    @SerialName("businessId")
    val businessId: String? = null,
    @SerialName("businessName")
    val businessName: String? = null,
    @SerialName("productId")
    val productId: String? = null,
    @SerialName("productName")
    val productName: String? = null,
    @SerialName("quantity")
    val quantity: Int? = null,
    @SerialName("unitPrice")
    val unitPrice: Double? = null,
    @SerialName("totalPrice")
    val totalPrice: Double? = null
)

@Serializable
data class OrderDto(
    @SerialName("businessId")
    val businessId: String? = null,
    @SerialName("businessName")
    val businessName: String? = null,
    @SerialName("code")
    val code: String? = null,
    @SerialName("createdAt")
    val createdAt: Long? = null,
    @SerialName("expiresAt")
    val expiresAt: Long? = null,
    @SerialName("grandTotal")
    val grandTotal: Double? = null,
    @SerialName("totalAmount")
    val totalAmount: Double? = null,
    @SerialName("platformDonation")
    val platformDonation: Double? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("supporterId")
    val supporterId: String? = null,
    @SerialName("supporterName")
    val supporterName: String? = null,
    @SerialName("items")
    val items: List<OrderItemDto>? = null
)
