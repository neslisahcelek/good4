package com.good4.product.presentation.product_list

import com.good4.product.Product

sealed interface ProductListAction {
    data class OnSearchQueryChange(val query: String) : ProductListAction
    data class OnProductClick(val product: Product) : ProductListAction
}

