package com.good4.product.presentation.product_list

import com.good4.product.Product

data class ProductListState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

