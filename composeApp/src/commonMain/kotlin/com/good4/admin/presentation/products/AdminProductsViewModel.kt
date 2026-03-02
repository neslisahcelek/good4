package com.good4.admin.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.business.domain.Business
import com.good4.core.domain.Result
import com.good4.product.data.dto.ProductDto
import com.good4.product.data.repository.FirestoreProductRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_business_required
import good4.composeapp.generated.resources.error_price_required
import good4.composeapp.generated.resources.error_product_name_required
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.getString

class AdminProductsViewModel(
    private val productRepository: FirestoreProductRepository,
    private val businessRepository: FirestoreBusinessRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminProductsState())
    val state = _state.asStateFlow()

    init {
        loadProducts()
        loadBusinesses()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = productRepository.getProducts(includeOutOfStock = true)) {
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

    private fun loadBusinesses() {
        viewModelScope.launch {
            when (val result = businessRepository.getBusinessesWithIds()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            businesses = result.data.map { docWithId ->
                                Business(
                                    id = docWithId.id,
                                    name = docWithId.data.name,
                                    ownerId = docWithId.data.ownerId,
                                    phone = docWithId.data.phone,
                                    address = docWithId.data.address,
                                    city = docWithId.data.city,
                                    district = docWithId.data.district
                                )
                            }
                        )
                    }
                }
                is Result.Error -> {}
            }
        }
    }

    fun onProductNameChange(name: String) {
        _state.update { it.copy(productName = name, errorMessage = null) }
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

    fun onCountChange(count: String) {
        if (count.isEmpty() || count.all { it.isDigit() }) {
            _state.update { it.copy(productCount = count) }
        }
    }

    fun onImageUrlChange(url: String) {
        _state.update { it.copy(productImageUrl = url) }
    }

    fun onBusinessSelect(businessId: String) {
        _state.update { it.copy(selectedBusinessId = businessId) }
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
        if (state.selectedBusinessId == null) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.error_business_required))
                }
            }
            return
        }
        if (state.productOriginalPrice.isBlank()) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.error_price_required))
                }
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isAddLoading = true, errorMessage = null) }

            val productDto = ProductDto(
                name = state.productName,
                description = state.productDescription.ifBlank { null },
                businessId = state.selectedBusinessId,
                originalPrice = state.productOriginalPrice.toIntOrNull() ?: 0,
                discountPrice = state.productDiscountPrice.toIntOrNull(),
                count = state.productCount.toIntOrNull() ?: 0,
                imageUrl = state.productImageUrl.ifBlank { null },
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
                    loadProducts()
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
                selectedBusinessId = product.businessId,
                productName = product.name,
                productDescription = product.description,
                productOriginalPrice = product.originalPrice?.toString().orEmpty(),
                productDiscountPrice = product.discountPrice?.toString().orEmpty(),
                productCount = product.amount.toString(),
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
        if (state.selectedBusinessId == null) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.error_business_required))
                }
            }
            return
        }
        if (state.productOriginalPrice.isBlank()) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.error_price_required))
                }
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isEditLoading = true, errorMessage = null) }

            val productDto = ProductDto(
                name = state.productName,
                description = state.productDescription.ifBlank { null },
                businessId = state.selectedBusinessId,
                originalPrice = state.productOriginalPrice.toIntOrNull() ?: 0,
                discountPrice = state.productDiscountPrice.toIntOrNull(),
                count = state.productCount.toIntOrNull() ?: 0,
                imageUrl = state.productImageUrl.ifBlank { null },
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
                    loadProducts()
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
                productName = "",
                productDescription = "",
                productOriginalPrice = "",
                productDiscountPrice = "",
                productCount = "",
                productImageUrl = "",
                selectedBusinessId = null,
                addSuccess = false,
                errorMessage = null
            )
        }
    }

    fun resetEditState() {
        _state.update {
            it.copy(
                selectedProduct = null,
                selectedBusinessId = null,
                productName = "",
                productDescription = "",
                productOriginalPrice = "",
                productDiscountPrice = "",
                productCount = "",
                productImageUrl = "",
                editSuccess = false,
                errorMessage = null
            )
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
