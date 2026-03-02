package com.good4.business.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.code.data.repository.CodeRepository
import com.good4.code.data.repository.statusEnum
import com.good4.code.domain.CodeStatus
import com.good4.core.domain.Result
import com.good4.user.data.repository.UserRepository
import com.good4.product.data.repository.FirestoreProductRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.business_name_fallback
import good4.composeapp.generated.resources.error_business_not_found
import good4.composeapp.generated.resources.product_name_fallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class BusinessDashboardViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val codeRepository: CodeRepository,
    private val productRepository: FirestoreProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BusinessDashboardState())
    val state = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun refreshDashboard() {
        loadDashboard()
    }

    private fun loadDashboard() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Get user to find businessId
            when (val userResult = userRepository.getUserDto(userId)) {
                is Result.Success -> {
                    val businessId = findBusinessIdForUser(userId)
                    
                    if (businessId != null) {
                        // Get business info
                        when (val businessResult = businessRepository.getBusinessById(businessId)) {
                            is Result.Success -> {
                                val fallback = getString(Res.string.business_name_fallback)
                                _state.update { 
                                    it.copy(businessName = businessResult.data.name.ifBlank { fallback })
                                }
                            }
                            is Result.Error -> {}
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
                                        errorMessage = countsResult.error.message
                                    )
                                }
                                return@launch
                            }
                        }

                        when (val recentResult = codeRepository.getRecentCodesByBusinessId(businessId, limit = 20)) {
                            is Result.Success -> {
                                val productFallback = getString(Res.string.product_name_fallback)
                                val recentCodes = recentResult.data
                                    .filter { code -> code.statusEnum != CodeStatus.CANCELLED }
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
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        recentCodes = recentCodes
                                    )
                                }
                            }
                            is Result.Error -> {
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = recentResult.error.message
                                    )
                                }
                            }
                        }

                        when (val productsResult = productRepository.getProducts(includeOutOfStock = true)) {
                            is Result.Success -> {
                                val totalProducts = productsResult.data.count { it.businessId == businessId }
                                _state.update { it.copy(totalProducts = totalProducts) }
                            }
                            is Result.Error -> Unit
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = getString(Res.string.error_business_not_found)
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = userResult.error.message
                        )
                    }
                }
            }
        }
    }

    private suspend fun findBusinessIdForUser(userId: String): String? {
        return when (val result = businessRepository.getBusinessesWithIds()) {
            is Result.Success -> {
                result.data.find { it.data.ownerId == userId }?.id
            }
            is Result.Error -> null
        }
    }
}
