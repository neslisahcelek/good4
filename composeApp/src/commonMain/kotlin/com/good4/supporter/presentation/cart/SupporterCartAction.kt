package com.good4.supporter.presentation.cart

import com.good4.product.Product

sealed interface SupporterCartAction {
    data class OnAddItem(val product: Product) : SupporterCartAction
    data class OnRemoveItem(val productId: String) : SupporterCartAction
    data class OnIncreaseQuantity(val productId: String) : SupporterCartAction
    data class OnDecreaseQuantity(val productId: String) : SupporterCartAction
    data class OnCancelActiveOrder(val orderId: String) : SupporterCartAction
    data object OnCreateOrder : SupporterCartAction
    data object OnConfirmCreateOrder : SupporterCartAction
    data object OnCancelOrderReview : SupporterCartAction
    data object OnRefreshActiveOrders : SupporterCartAction
    data object OnDismissError : SupporterCartAction
    data object OnOrderNavigated : SupporterCartAction
}
