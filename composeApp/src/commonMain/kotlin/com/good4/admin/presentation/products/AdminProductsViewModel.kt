package com.good4.admin.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.business.domain.Business
import com.good4.core.data.repository.ProductImageUploadRepository
import com.good4.core.domain.Result
import com.good4.product.data.dto.ProductDto
import com.good4.product.data.repository.FirestoreProductRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_business_required
import good4.composeapp.generated.resources.error_daily_pending_limit_invalid
import good4.composeapp.generated.resources.error_image_upload_failed
import good4.composeapp.generated.resources.error_image_upload_permission_denied
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
    private val businessRepository: FirestoreBusinessRepository,
    private val productImageUploadRepository: ProductImageUploadRepository
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
                                    addressUrl = docWithId.data.addressUrl,
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
        if (_state.value.isDonationProduct) return
        if (price.isEmpty() || price.all { it.isDigit() }) {
            _state.update { it.copy(productOriginalPrice = price) }
        }
    }

    fun onDiscountPriceChange(price: String) {
        if (_state.value.isDonationProduct) return
        if (price.isEmpty() || price.all { it.isDigit() }) {
            _state.update { it.copy(productDiscountPrice = price) }
        }
    }

    fun onDonationProductChange(isDonation: Boolean) {
        _state.update { current ->
            if (isDonation) {
                current.copy(
                    isDonationProduct = true,
                    productOriginalPrice = "0",
                    productDiscountPrice = "0"
                )
            } else {
                current.copy(
                    isDonationProduct = false,
                    productDailyPendingLimit = ""
                )
            }
        }
    }

    fun onDailyPendingLimitChange(limit: String) {
        if (limit.isEmpty() || limit.all { it.isDigit() }) {
            _state.update { current ->
                if (current.isDonationProduct) {
                    current.copy(
                        productDailyPendingLimit = limit,
                        productOriginalPrice = "0",
                        productDiscountPrice = "0"
                    )
                } else {
                    current.copy(productDailyPendingLimit = limit)
                }
            }
        }
    }

    fun onPendingProductImageChange(bytes: ByteArray?) {
        _state.update {
            it.copy(pendingProductImageBytes = bytes?.copyOf())
        }
    }

    fun onBusinessSelect(businessId: String) {
        _state.update { it.copy(selectedBusinessId = businessId) }
    }

    private enum class SubmitKind { Add, Edit }

    private suspend fun resolveImageUrlForSubmit(
        state: AdminProductsState,
        kind: SubmitKind
    ): String? {
        val pending = state.pendingProductImageBytes
        if (pending != null) {
            _state.update { it.copy(isProductImageUploading = true) }
            return when (val upload = productImageUploadRepository.uploadProductImage(pending)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isProductImageUploading = false,
                            pendingProductImageBytes = null,
                            productImageUrl = upload.data
                        )
                    }
                    upload.data
                }
                is Result.Error -> {
                    val uiMessage = mapImageUploadErrorMessage(upload.error.message)
                    _state.update {
                        when (kind) {
                            SubmitKind.Add -> it.copy(
                                isAddLoading = false,
                                isProductImageUploading = false,
                                errorMessage = uiMessage
                            )
                            SubmitKind.Edit -> it.copy(
                                isEditLoading = false,
                                isProductImageUploading = false,
                                errorMessage = uiMessage
                            )
                        }
                    }
                    null
                }
            }
        }
        return state.productImageUrl.ifBlank { null }
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
        if (isDailyPendingLimitInvalid(
                rawValue = state.productDailyPendingLimit,
                isDonationProduct = state.isDonationProduct
            )
        ) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.error_daily_pending_limit_invalid))
                }
            }
            return
        }
        if (!state.isDonationProduct && state.productOriginalPrice.isBlank()) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.error_price_required))
                }
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isAddLoading = true, errorMessage = null) }
            val snapshot = _state.value
            val imageUrl = resolveImageUrlForSubmit(snapshot, SubmitKind.Add)
            if (imageUrl == null && snapshot.pendingProductImageBytes != null) {
                return@launch
            }

            val isDonation = snapshot.isDonationProduct
            val donationLimit = if (isDonation) snapshot.productDailyPendingLimit.toIntOrNull() else null
            val productDto = ProductDto(
                name = snapshot.productName,
                description = snapshot.productDescription.ifBlank { null },
                businessId = snapshot.selectedBusinessId,
                originalPrice = if (isDonation) 0 else snapshot.productOriginalPrice.toIntOrNull() ?: 0,
                discountPrice = if (isDonation) 0 else snapshot.productDiscountPrice.toIntOrNull(),
                pendingCount = if (isDonation) donationLimit else null,
                dailyPendingLimit = donationLimit,
                isDonation = isDonation,
                imageUrl = imageUrl,
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
                isDonationProduct = product.isDonation,
                productDailyPendingLimit = product.dailyPendingLimit?.toString().orEmpty(),
                productImageUrl = product.imageUrl,
                pendingProductImageBytes = null,
                isProductImageUploading = false
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
        if (isDailyPendingLimitInvalid(
                rawValue = state.productDailyPendingLimit,
                isDonationProduct = state.isDonationProduct
            )
        ) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.error_daily_pending_limit_invalid))
                }
            }
            return
        }
        if (!state.isDonationProduct && state.productOriginalPrice.isBlank()) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.error_price_required))
                }
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isEditLoading = true, errorMessage = null) }
            val snapshot = _state.value
            val imageUrl = resolveImageUrlForSubmit(snapshot, SubmitKind.Edit)
            if (imageUrl == null && snapshot.pendingProductImageBytes != null) {
                return@launch
            }

            val isDonation = snapshot.isDonationProduct
            val donationLimit = if (isDonation) snapshot.productDailyPendingLimit.toIntOrNull() else null
            val productDto = ProductDto(
                name = snapshot.productName,
                description = snapshot.productDescription.ifBlank { null },
                businessId = snapshot.selectedBusinessId,
                originalPrice = if (isDonation) 0 else snapshot.productOriginalPrice.toIntOrNull() ?: 0,
                discountPrice = if (isDonation) 0 else snapshot.productDiscountPrice.toIntOrNull(),
                imageUrl = imageUrl,
                pendingCount = if (isDonation) donationLimit else null,
                dailyPendingLimit = donationLimit,
                isDonation = isDonation,
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
                isDonationProduct = false,
                productOriginalPrice = "",
                productDiscountPrice = "",
                productDailyPendingLimit = "",
                productImageUrl = "",
                pendingProductImageBytes = null,
                isProductImageUploading = false,
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
                isDonationProduct = false,
                productOriginalPrice = "",
                productDiscountPrice = "",
                productDailyPendingLimit = "",
                productImageUrl = "",
                pendingProductImageBytes = null,
                isProductImageUploading = false,
                editSuccess = false,
                errorMessage = null
            )
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private suspend fun mapImageUploadErrorMessage(rawMessage: String): String {
        val lower = rawMessage.lowercase()
        return if (
            "permission denied" in lower ||
            "unauthorized" in lower ||
            "storageerror.unauthorized" in lower ||
            "code=403" in lower ||
            "\"code\": 403" in lower
        ) {
            getString(Res.string.error_image_upload_permission_denied)
        } else {
            getString(Res.string.error_image_upload_failed)
        }
    }

    private fun isDailyPendingLimitInvalid(rawValue: String, isDonationProduct: Boolean): Boolean {
        if (!isDonationProduct) return false
        if (rawValue.isBlank()) return true
        val value = rawValue.toIntOrNull() ?: return true
        return value <= 0
    }

}
