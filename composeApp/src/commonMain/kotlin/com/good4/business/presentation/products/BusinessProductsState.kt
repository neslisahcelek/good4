package com.good4.business.presentation.products

import com.good4.product.Product

data class BusinessProductsState(
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null,
    val isAddLoading: Boolean = false,
    val isEditLoading: Boolean = false,
    val addSuccess: Boolean = false,
    val editSuccess: Boolean = false,
    val selectedProduct: Product? = null,
    val productName: String = "",
    val productDescription: String = "",
    val productOriginalPrice: String = "",
    val productDiscountPrice: String = "",
    val productAmount: String = "",
    val productImageUrl: String = "",
    val isProductImageUploading: Boolean = false
)
