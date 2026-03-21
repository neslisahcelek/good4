package com.good4.supporter.presentation.products

import com.good4.core.presentation.UiText
import com.good4.product.Product

data class SupporterProductListState(
    val supporterName: String = "",
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null
)
