package com.good4.supporter.presentation.cart

import com.good4.core.presentation.UiText
import com.good4.product.Product

data class CartItem(
    val product: Product,
    val quantity: Int
)

data class ActiveSupporterOrder(
    val id: String,
    val code: String,
    val productName: String,
    val businessName: String,
    val expiresAtEpochSeconds: Long?
)

data class SupporterCartState(
    val items: List<CartItem> = emptyList(),
    val activeOrders: List<ActiveSupporterOrder> = emptyList(),
    val cancellingOrderIds: Set<String> = emptySet(),
    val isReviewingOrder: Boolean = false,
    val isCreatingOrder: Boolean = false,
    val errorMessage: UiText? = null,
    val createdOrderId: String? = null
)

val SupporterCartState.totalPrice: Double
    get() = items.sumOf {
        val unitPrice = (it.product.discountPrice ?: it.product.originalPrice ?: it.product.price).toDouble()
        unitPrice * it.quantity
    }

val SupporterCartState.totalItemCount: Int
    get() = items.sumOf { it.quantity }
