package com.good4.admin.presentation.products

import com.good4.business.domain.Business
import com.good4.product.Product

data class AdminProductsState(
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val businesses: List<Business> = emptyList(),
    val selectedBusinessId: String? = null,
    val productName: String = "",
    val productDescription: String = "",
    val productOriginalPrice: String = "",
    val productDiscountPrice: String = "",
    val productCount: String = "",
    val productImageUrl: String = "",
    val isAddLoading: Boolean = false,
    val addSuccess: Boolean = false,
    val errorMessage: String? = null
)

