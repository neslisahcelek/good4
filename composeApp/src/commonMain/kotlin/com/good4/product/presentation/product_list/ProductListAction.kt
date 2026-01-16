package com.good4.product.presentation.product_list

import com.good4.product.Product

sealed interface ProductListAction {
    data class OnSearchQueryChange(val query: String) : ProductListAction
    data class OnReserveProduct(val product: Product) : ProductListAction
    data object OnDismissError : ProductListAction
    data class OnReservationExpired(val codeId: String) : ProductListAction
}

