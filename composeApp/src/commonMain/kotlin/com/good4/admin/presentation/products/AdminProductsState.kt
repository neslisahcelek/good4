package com.good4.admin.presentation.products

import com.good4.business.domain.Business
import com.good4.product.Product

data class AdminProductsState(
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val businesses: List<Business> = emptyList(),
    val selectedBusinessId: String? = null,
    val selectedProduct: Product? = null,
    val isDonationProduct: Boolean = false,
    val productName: String = "",
    val productDescription: String = "",
    val productOriginalPrice: String = "",
    val productDiscountPrice: String = "",
    val productDailyPendingLimit: String = "",
    val productImageUrl: String = "",
    val pendingProductImageBytes: ByteArray? = null,
    val isProductImageUploading: Boolean = false,
    val isAddLoading: Boolean = false,
    val addSuccess: Boolean = false,
    val isEditLoading: Boolean = false,
    val editSuccess: Boolean = false,
    val errorMessage: String? = null
)
