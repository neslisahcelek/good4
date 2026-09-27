package com.good4.business.presentation.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.code.data.repository.CodeRepository
import com.good4.community.CommunityRepository
import com.good4.core.domain.Result
import com.good4.product.data.repository.FirestoreProductRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_business_not_found
import good4.composeapp.generated.resources.verify_code_error_failed
import good4.composeapp.generated.resources.verify_code_error_invalid
import good4.composeapp.generated.resources.verify_code_not_found
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class VerifyCodeViewModel(
    private val authRepository: AuthRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val codeRepository: CodeRepository,
    private val productRepository: FirestoreProductRepository,
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VerifyCodeState())
    val state = _state.asStateFlow()

    private var businessId: String? = null

    init {
        loadBusinessId()
    }

    private fun loadBusinessId() {
        val userId = authRepository.currentUser?.uid
        if (userId == null) {
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        isBusinessContextLoading = false,
                        businessContextError = getString(Res.string.error_business_not_found)
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(isBusinessContextLoading = true, businessContextError = null)
            }
            when (val result = businessRepository.getOwnedBusinessId(userId)) {
                is Result.Success -> {
                    businessId = result.data
                    _state.update {
                        it.copy(
                            isBusinessContextLoading = false,
                            businessContextError = if (result.data == null) {
                                getString(Res.string.error_business_not_found)
                            } else {
                                null
                            }
                        )
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isBusinessContextLoading = false,
                            businessContextError = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun onCodeInputChange(code: String) {
        if (code.length <= 6 && code.all { it.isDigit() }) {
            _state.update {
                it.copy(
                    codeInput = code,
                    errorMessage = null,
                    verificationSuccess = false
                )
            }
        }
    }

    fun verifyCode() {
        val snapshot = _state.value
        if (snapshot.isBusinessContextLoading) return

        val bid = businessId
        if (bid.isNullOrBlank()) {
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        errorMessage = snapshot.businessContextError
                            ?: getString(Res.string.error_business_not_found)
                    )
                }
            }
            return
        }

        val code = snapshot.codeInput
        if (code.length != 6 && code.length != 4) {
            viewModelScope.launch {
                _state.update { it.copy(errorMessage = getString(Res.string.verify_code_error_invalid)) }
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    verificationSuccess = false
                )
            }

            when (val studentResult = codeRepository.verifyCode(code, bid)) {
                is Result.Success -> {
                    when (codeRepository.markCodeAsUsed(studentResult.data.id)) {
                        is Result.Success -> {
                            when (productRepository.recordProductDelivery(studentResult.data.productId)) {
                                is Result.Success -> {
                                    _state.update {
                                        it.copy(
                                            isLoading = false,
                                            verificationSuccess = true,
                                            verifiedProductName = studentResult.data.productName,
                                            codeInput = ""
                                        )
                                    }
                                }

                                is Result.Error -> {
                                    _state.update {
                                        it.copy(
                                            isLoading = false,
                                            errorMessage = getString(Res.string.verify_code_error_failed)
                                        )
                                    }
                                }
                            }
                        }

                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = getString(Res.string.verify_code_error_failed)
                                )
                            }
                        }
                    }
                }

                is Result.Error -> {
                    tryVerifyAsCommunityCoupon(code, bid)
                }
            }
        }
    }

    private suspend fun tryVerifyAsCommunityCoupon(code: String, bid: String) {
        val coupon = communityRepository.verifyCouponCode(code, bid)
        if (coupon != null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    verificationSuccess = true,
                    verifiedProductName = coupon.title,
                    codeInput = "",
                    errorMessage = null
                )
            }
        } else {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = getString(Res.string.verify_code_not_found)
                )
            }
        }
    }

    fun resetState() {
        _state.update {
            it.copy(
                codeInput = "",
                isLoading = false,
                verificationSuccess = false,
                verifiedProductName = null,
                errorMessage = null
            )
        }
    }
}
