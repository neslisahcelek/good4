package com.good4.order.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class OrderItem(
    val businessId: String,
    val businessName: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

data class Order(
    val id: String,
    val businessId: String,
    val businessName: String,
    val code: String,
    val createdAt: Instant?,
    val expiresAt: Instant?,
    val grandTotal: Double,
    val totalAmount: Double,
    val platformDonation: Double,
    val status: OrderStatus,
    val supporterId: String,
    val supporterName: String,
    val items: List<OrderItem>
)

fun Order.isActivePending(): Boolean {
    return status == OrderStatus.PENDING && !isExpired()
}

fun Order.isExpired(): Boolean {
    return expiresAt?.let { expiresAt -> expiresAt <= Clock.System.now() } ?: false
}

fun Order.isVisibleOnBusinessDashboard(): Boolean {
    return status != OrderStatus.EXPIRED &&
            status != OrderStatus.CANCELLED &&
            (status != OrderStatus.PENDING || isActivePending())
}
