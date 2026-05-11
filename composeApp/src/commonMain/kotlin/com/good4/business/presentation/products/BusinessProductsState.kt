package com.good4.business.presentation.products

import com.good4.product.Product

@Suppress("ArrayInDataClass")
data class BusinessProductsState(
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null,
    val isAddLoading: Boolean = false,
    val isEditLoading: Boolean = false,
    val isDeleteLoading: Boolean = false,
    val addSuccess: Boolean = false,
    val editSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val selectedProduct: Product? = null,
    val isDonationProduct: Boolean = false,
    val productName: String = "",
    val productDescription: String = "",
    val productOriginalPrice: String = "",
    val productDiscountPrice: String = "",
    val productDailyPendingLimit: String = "",
    val productImageUrl: String = "",
    val pendingProductImageBytes: ByteArray? = null,
    val isProductImageUploading: Boolean = false
)
