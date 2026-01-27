package com.good4.business.presentation.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.code.data.repository.CodeRepository
import com.good4.core.domain.Result
import com.good4.product.data.repository.FirestoreProductRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.verify_code_error_failed
import good4.composeapp.generated.resources.verify_code_error_invalid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class VerifyCodeViewModel(
    private val authRepository: AuthRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val codeRepository: CodeRepository,
    private val productRepository: FirestoreProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VerifyCodeState())
    val state = _state.asStateFlow()

    private var businessId: String? = null

    init {
        loadBusinessId()
    }

    private fun loadBusinessId() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            when (val result = businessRepository.getBusinessesWithIds()) {
                is Result.Success -> {
                    businessId = result.data.find { it.data.ownerId == userId }?.id
                }
                is Result.Error -> {}
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
        val code = _state.value.codeInput
        if (code.length != 6) {
            viewModelScope.launch {
                _state.update {
                    it.copy(errorMessage = getString(Res.string.verify_code_error_invalid))
                }
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, verificationSuccess = false) }

            // For now, we'll verify against all pending codes for this business
            // In production, you'd want to scope this to the specific business
            when (val result = codeRepository.verifyCode(code, businessId ?: "")) {
                is Result.Success -> {
                    // Mark code as used
                    when (codeRepository.markCodeAsUsed(result.data.id)) {
                        is Result.Success -> {
                            viewModelScope.launch {
                                val productId = result.data.productId
                                productRepository.decrementProductCount(productId)
                            }
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    verificationSuccess = true,
                                    verifiedProductName = result.data.productName,
                                    codeInput = ""
                                )
                            }
                        }
                        is Result.Error -> {
                            val message = getString(Res.string.verify_code_error_failed)
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = message
                                )
                            }
                        }
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

    fun resetState() {
        _state.update { VerifyCodeState() }
    }
}
