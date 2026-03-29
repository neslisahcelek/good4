package com.good4.business.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.core.data.repository.ProductImageUploadRepository
import com.good4.core.domain.Result
import com.good4.core.util.Logger
import com.good4.product.data.dto.ProductDto
import com.good4.product.data.repository.FirestoreProductRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_business_not_found
import good4.composeapp.generated.resources.error_daily_pending_limit_invalid
import good4.composeapp.generated.resources.error_image_upload_failed
import good4.composeapp.generated.resources.error_image_upload_permission_denied
import good4.composeapp.generated.resources.error_product_name_required
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

class BusinessProductsViewModel(
    private val authRepository: AuthRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val productRepository: FirestoreProductRepository,
    private val productImageUploadRepository: ProductImageUploadRepository
) : ViewModel() {
    companion object {
        private const val TAG = "BusinessProductsVM"
        private val DONATION_RESET_TIME_ZONE = TimeZone.of("Europe/Istanbul")
    }

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

            when (val ownedResult = businessRepository.getOwnedBusinessId(userId)) {
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ownedResult.error.message
                        )
                    }
                    return@launch
                }

                is Result.Success -> {
                    businessId = ownedResult.data
                }
            }

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

    fun onProductNameChange(name: String) {
        _state.update { it.copy(productName = name) }
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
                businessId = businessId,
                originalPrice = if (isDonation) 0 else snapshot.productOriginalPrice.toIntOrNull(),
                discountPrice = if (isDonation) 0 else snapshot.productDiscountPrice.toIntOrNull(),
                pendingCount = if (isDonation) donationLimit else null,
                dailyPendingLimit = donationLimit,
                isDonation = isDonation,
                lastDailyResetEpochDay = if (isDonation) currentDonationDateKey() else null,
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

    private enum class SubmitKind { Add, Edit }

    /**
     * @return null = yükleme hatası (state güncellendi) veya görsel yok; aksi halde Firestore'a yazılacak URL.
     */
    private suspend fun resolveImageUrlForSubmit(
        state: BusinessProductsState,
        kind: SubmitKind
    ): String? {
        val pending = state.pendingProductImageBytes
        if (pending != null) {
            Logger.d(TAG, "image_upload_start | kind=$kind | bytes=${pending.size}")
            _state.update { it.copy(isProductImageUploading = true) }
            return when (val upload = productImageUploadRepository.uploadProductImage(pending)) {
                is Result.Success -> {
                    Logger.d(TAG, "image_upload_success | kind=$kind | url=${upload.data}")
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
                    Logger.e(TAG, "image_upload_error | kind=$kind | message=${upload.error.message}")
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

    fun selectProductForEdit(product: com.good4.product.Product) {
        _state.update {
            it.copy(
                selectedProduct = product,
                productName = product.name,
                productDescription = product.description,
                productOriginalPrice = product.originalPrice?.toString() ?: "",
                productDiscountPrice = product.discountPrice?.toString() ?: "",
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
        Logger.d(
            TAG,
            "update_product_click | docId=${product.documentId} | pendingImage=${state.pendingProductImageBytes != null}"
        )

        if (state.productName.isBlank()) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.error_product_name_required))
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
        viewModelScope.launch {
            _state.update { it.copy(isEditLoading = true, errorMessage = null) }
            val snapshot = _state.value
            val imageUrl = resolveImageUrlForSubmit(snapshot, SubmitKind.Edit)
            if (imageUrl == null && snapshot.pendingProductImageBytes != null) {
                Logger.e(TAG, "update_product_abort | reason=image_upload_failed")
                return@launch
            }
            Logger.d(TAG, "update_product_payload | docId=${product.documentId} | imageUrl=${imageUrl.orEmpty()}")

            val isDonation = snapshot.isDonationProduct
            val donationLimit = if (isDonation) snapshot.productDailyPendingLimit.toIntOrNull() else null

            val productDto = ProductDto(
                name = snapshot.productName,
                description = snapshot.productDescription.ifBlank { null },
                businessId = businessId ?: product.businessId,
                originalPrice = if (isDonation) 0 else snapshot.productOriginalPrice.toIntOrNull(),
                discountPrice = if (isDonation) 0 else snapshot.productDiscountPrice.toIntOrNull(),
                pendingCount = if (isDonation) donationLimit else null,
                dailyPendingLimit = donationLimit,
                isDonation = isDonation,
                lastDailyResetEpochDay = if (isDonation) currentDonationDateKey() else null,
                imageUrl = imageUrl,
                totalDelivered = product.totalDelivered,
                totalSuspended = product.totalSuspended,
                createdAt = product.createdAt
            )

            when (val result = productRepository.updateProduct(product.documentId, productDto)) {
                is Result.Success -> {
                    Logger.d(TAG, "update_product_success | docId=${product.documentId}")
                    _state.update {
                        it.copy(
                            isEditLoading = false,
                            editSuccess = true
                        )
                    }
                    loadBusinessProducts()
                }
                is Result.Error -> {
                    Logger.e(
                        TAG,
                        "update_product_error | docId=${product.documentId} | message=${result.error.message}"
                    )
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
                isDonationProduct = false,
                productOriginalPrice = "",
                productDiscountPrice = "",
                productDailyPendingLimit = "",
                productImageUrl = "",
                pendingProductImageBytes = null,
                isProductImageUploading = false,
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
                isDonationProduct = false,
                productOriginalPrice = "",
                productDiscountPrice = "",
                productDailyPendingLimit = "",
                productImageUrl = "",
                pendingProductImageBytes = null,
                isProductImageUploading = false,
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

    private fun currentDonationDateKey(): Int {
        val date = Clock.System.now().toLocalDateTime(DONATION_RESET_TIME_ZONE).date
        return (date.year * 10_000) + (date.monthNumber * 100) + date.dayOfMonth
    }
}
