package com.good4.business.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.core.domain.Result
import com.good4.product.data.dto.ProductDto
import com.good4.product.data.repository.FirestoreProductRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_business_not_found
import good4.composeapp.generated.resources.error_product_name_required
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.getString

class BusinessProductsViewModel(
    private val authRepository: AuthRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val productRepository: FirestoreProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BusinessProductsState())
    val state = _state.asStateFlow()

    private var businessId: String? = null

    init {
        loadBusinessProducts()
    }

    private fun loadBusinessProducts() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            businessId = findBusinessIdForUser(userId)
            val currentBusinessId = businessId

            if (currentBusinessId == null) {
                val message = getString(Res.string.error_business_not_found)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                }
                return@launch
            }

            when (val result = productRepository.getProductsByBusinessId(
                businessId = currentBusinessId,
                includeOutOfStock = true
            )) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun refreshProducts() {
        loadBusinessProducts()
    }

    private suspend fun findBusinessIdForUser(userId: String): String? {
        return when (val result = businessRepository.getBusinessesWithIds()) {
            is Result.Success -> {
                result.data.find { it.data.ownerId == userId }?.id
            }
            is Result.Error -> null
        }
    }

    fun onProductNameChange(name: String) {
        _state.update { it.copy(productName = name) }
    }

    fun onProductDescriptionChange(description: String) {
        _state.update { it.copy(productDescription = description) }
    }

    fun onOriginalPriceChange(price: String) {
        if (price.isEmpty() || price.all { it.isDigit() }) {
            _state.update { it.copy(productOriginalPrice = price) }
        }
    }

    fun onDiscountPriceChange(price: String) {
        if (price.isEmpty() || price.all { it.isDigit() }) {
            _state.update { it.copy(productDiscountPrice = price) }
        }
    }

    fun onAmountChange(amount: String) {
        if (amount.isEmpty() || amount.all { it.isDigit() }) {
            _state.update { it.copy(productAmount = amount) }
        }
    }

    fun onImageUrlChange(url: String) {
        _state.update { it.copy(productImageUrl = url) }
    }

    fun addProduct() {
        val state = _state.value

        if (state.productName.isBlank()) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.error_product_name_required))
                }
            }
            return
        }
        if (businessId == null) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.error_business_not_found))
                }
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isAddLoading = true, errorMessage = null) }

            val productDto = ProductDto(
                name = state.productName,
                description = state.productDescription.ifBlank { null },
                businessId = businessId,
                originalPrice = state.productOriginalPrice.toIntOrNull(),
                discountPrice = state.productDiscountPrice.toIntOrNull(),
                pendingCount = state.productAmount.toIntOrNull(),
                imageUrl = state.productImageUrl.ifBlank { null },
                totalDelivered = 0,
                totalSuspended = 0,
                createdAt = Clock.System.now().epochSeconds
            )

            when (val result = productRepository.addProduct(productDto)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isAddLoading = false,
                            addSuccess = true
                        )
                    }
                    loadBusinessProducts()
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isAddLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun selectProductForEdit(product: com.good4.product.Product) {
        _state.update {
            it.copy(
                selectedProduct = product,
                productName = product.name,
                productDescription = product.description,
                productOriginalPrice = product.originalPrice?.toString() ?: "",
                productDiscountPrice = product.discountPrice?.toString() ?: "",
                productAmount = product.amount.toString(),
                productImageUrl = product.imageUrl
            )
        }
    }

    fun updateProduct() {
        val state = _state.value
        val product = state.selectedProduct ?: return

        if (state.productName.isBlank()) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.error_product_name_required))
                }
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isEditLoading = true, errorMessage = null) }

            val productDto = ProductDto(
                name = state.productName,
                description = state.productDescription.ifBlank { null },
                businessId = businessId,
                originalPrice = state.productOriginalPrice.toIntOrNull(),
                discountPrice = state.productDiscountPrice.toIntOrNull(),
                pendingCount = state.productAmount.toIntOrNull(),
                imageUrl = state.productImageUrl.ifBlank { null },
                totalDelivered = product.totalDelivered,
                totalSuspended = product.totalSuspended,
                createdAt = product.createdAt
            )

            when (val result = productRepository.updateProduct(product.documentId, productDto)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isEditLoading = false,
                            editSuccess = true
                        )
                    }
                    loadBusinessProducts()
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isEditLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun resetAddState() {
        _state.update {
            it.copy(
                addSuccess = false,
                productName = "",
                productDescription = "",
                productOriginalPrice = "",
                productDiscountPrice = "",
                productAmount = "",
                productImageUrl = "",
                errorMessage = null
            )
        }
    }

    fun resetEditState() {
        _state.update {
            it.copy(
                editSuccess = false,
                selectedProduct = null,
                productName = "",
                productDescription = "",
                productOriginalPrice = "",
                productDiscountPrice = "",
                productAmount = "",
                productImageUrl = "",
                errorMessage = null
            )
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
