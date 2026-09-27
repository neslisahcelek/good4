package com.good4.business.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.code.data.repository.CodeRepository
import com.good4.code.data.repository.statusEnum
import com.good4.code.domain.CodeStatus
import com.good4.core.domain.Result
import com.good4.core.util.userFriendlyErrorMessage
import com.good4.product.data.repository.FirestoreProductRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.business_name_fallback
import good4.composeapp.generated.resources.error_business_not_found
import good4.composeapp.generated.resources.error_data_load_failed
import good4.composeapp.generated.resources.product_name_fallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class BusinessDashboardViewModel(
    private val authRepository: AuthRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val codeRepository: CodeRepository,
    private val productRepository: FirestoreProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BusinessDashboardState())
    val state = _state.asStateFlow()

    private var cachedBusinessId: String? = null
    private var hasLoadedOnce: Boolean = false

    init {
        loadDashboard()
    }

    fun refreshDashboard(showLoading: Boolean = !hasLoadedOnce) {
        loadDashboard(showLoading = showLoading)
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun loadDashboard(showLoading: Boolean = true) {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _state.update { current ->
                    current.copy(
                        isLoading = showLoading,
                        errorMessage = null
                    )
                }
                val loadErrorFallback = getString(Res.string.error_data_load_failed)

                when (val ownedResult = businessRepository.getOwnedBusinessId(userId)) {
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = userFriendlyErrorMessage(
                                    ownedResult.error.message,
                                    loadErrorFallback
                                )
                            )
                        }
                        return@launch
                    }

                    is Result.Success -> {
                        val businessId = ownedResult.data
                        if (businessId == null) {
                            cachedBusinessId = null
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = getString(Res.string.error_business_not_found)
                                )
                            }
                            return@launch
                        }

                        cachedBusinessId = businessId

                        val fallbackName = getString(Res.string.business_name_fallback)

                        codeRepository.checkAndExpireCodes()

                    when (val businessResult = businessRepository.getBusinessById(businessId)) {
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    businessName = businessResult.data.name.ifBlank { fallbackName }
                                )
                            }
                        }

                        is Result.Error -> {
                            _state.update {
                                it.copy(businessName = fallbackName)
                            }
                        }
                    }

                    when (val countsResult = codeRepository.getCodeCountsByBusinessId(businessId)) {
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    pendingCount = countsResult.data.pending,
                                    completedCount = countsResult.data.completed
                                )
                            }
                        }

                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = userFriendlyErrorMessage(
                                        countsResult.error.message,
                                        loadErrorFallback
                                    )
                                )
                            }
                            return@launch
                        }
                    }

                    val recentCodes = when (val recentResult = codeRepository.getRecentCodesByBusinessId(businessId, limit = 20)) {
                        is Result.Success -> {
                            val productFallback = getString(Res.string.product_name_fallback)
                            recentResult.data
                                .filter { code ->
                                    code.statusEnum != CodeStatus.CANCELLED &&
                                            code.statusEnum != CodeStatus.EXPIRED
                                }
                                .sortedByDescending { code -> code.usedAt ?: code.createdAt ?: 0L }
                                .map { code ->
                                    RecentCodeUiModel(
                                        id = code.id,
                                        codeValue = code.value,
                                        productName = code.productName ?: productFallback,
                                        status = code.status
                                    )
                                }
                                .take(10)
                        }

                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = userFriendlyErrorMessage(
                                        recentResult.error.message,
                                        loadErrorFallback
                                    )
                                )
                            }
                            return@launch
                        }
                    }

                    when (val productsResult = productRepository.getProductsByBusinessId(businessId, includeOutOfStock = true)) {
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    recentCodes = recentCodes,
                                    totalProducts = productsResult.data.size,
                                    errorMessage = null
                                )
                            }
                        }

                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    recentCodes = recentCodes,
                                    totalProducts = 0,
                                    errorMessage = userFriendlyErrorMessage(
                                        productsResult.error.message,
                                        loadErrorFallback
                                    )
                                )
                            }
                        }
                    }
                    }
                }
            } finally {
                hasLoadedOnce = true
            }
        }
    }
}
