package com.good4.business.presentation.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.code.data.repository.CodeRepository
import com.good4.core.domain.Result
import com.good4.order.data.repository.OrderRepository
import com.good4.order.domain.OrderStatus
import com.good4.product.data.repository.FirestoreProductRepository
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.verify_code_error_failed
import good4.composeapp.generated.resources.verify_code_error_invalid
import good4.composeapp.generated.resources.verify_code_order_cancelled
import good4.composeapp.generated.resources.verify_code_order_error_cancel
import good4.composeapp.generated.resources.verify_code_order_error_confirm
import good4.composeapp.generated.resources.verify_code_order_not_found
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.getString

class VerifyCodeViewModel(
    private val authRepository: AuthRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val codeRepository: CodeRepository,
    private val productRepository: FirestoreProductRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository
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
                    verificationSuccess = false,
                    orderConfirmedSuccess = false,
                    orderCancelledSuccess = false,
                    pendingOrder = null
                )
            }
        }
    }

    fun verifyCode() {
        val code = _state.value.codeInput
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
                    verificationSuccess = false,
                    orderConfirmedSuccess = false,
                    orderCancelledSuccess = false,
                    pendingOrder = null
                )
            }

            val bid = businessId ?: ""

            when (val studentResult = codeRepository.verifyCode(code, bid)) {
                is Result.Success -> {
                    when (codeRepository.markCodeAsUsed(studentResult.data.id)) {
                        is Result.Success -> {
                            launch {
                                productRepository.decrementProductPendingCount(studentResult.data.productId)
                                productRepository.incrementProductDeliveredCount(studentResult.data.productId, 1)
                            }
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
                    // Öğrenci kodu olarak bulunamadı — destekçi siparişi olarak dene
                    tryVerifyAsOrder(code, bid)
                }
            }
        }
    }

    private suspend fun tryVerifyAsOrder(code: String, bid: String) {
        when (val orderResult = orderRepository.getOrderByCodeAndBusiness(code, bid)) {
            is Result.Success -> {
                val order = orderResult.data
                val isExpired = order?.expiresAt?.let { it <= Clock.System.now() } == true
                if (order != null && !isExpired) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            pendingOrder = order,
                            orderConfirmedSuccess = false,
                            orderCancelledSuccess = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = getString(Res.string.verify_code_order_not_found)
                        )
                    }
                }
            }

            is Result.Error -> {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = getString(Res.string.verify_code_order_not_found)
                    )
                }
            }
        }
    }

    fun confirmOrder() {
        val order = _state.value.pendingOrder ?: return
        if (_state.value.isConfirmingOrder || _state.value.isCancellingOrder) return

        viewModelScope.launch {
            _state.update { it.copy(isConfirmingOrder = true, errorMessage = null) }

            when (orderRepository.updateOrderStatus(order.id, OrderStatus.CONFIRMED)) {
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isConfirmingOrder = false,
                            errorMessage = getString(Res.string.verify_code_order_error_confirm)
                        )
                    }
                    return@launch
                }

                is Result.Success -> Unit
            }

            order.items.forEach { item ->
                productRepository.incrementProductPendingCount(item.productId, item.quantity)
            }

            val totalMeals = order.items.sumOf { it.quantity }
            userRepository.incrementUserDonations(order.supporterId, totalMeals)

            _state.update {
                it.copy(
                    isConfirmingOrder = false,
                    isCancellingOrder = false,
                    orderConfirmedSuccess = true,
                    orderCancelledSuccess = false,
                    pendingOrder = null,
                    codeInput = ""
                )
            }
        }
    }

    fun cancelOrder() {
        val order = _state.value.pendingOrder ?: return
        if (_state.value.isCancellingOrder || _state.value.isConfirmingOrder) return

        viewModelScope.launch {
            _state.update { it.copy(isCancellingOrder = true, errorMessage = null) }

            when (orderRepository.updateOrderStatus(order.id, OrderStatus.CANCELLED)) {
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isCancellingOrder = false,
                            errorMessage = getString(Res.string.verify_code_order_error_cancel)
                        )
                    }
                    return@launch
                }

                is Result.Success -> Unit
            }

            order.items.forEach { item ->
                productRepository.incrementProductSuspendedCount(item.productId, item.quantity)
            }

            _state.update {
                it.copy(
                    isConfirmingOrder = false,
                    isCancellingOrder = false,
                    orderConfirmedSuccess = false,
                    orderCancelledSuccess = true,
                    verifiedProductName = null,
                    pendingOrder = null,
                    codeInput = "",
                    errorMessage = getString(Res.string.verify_code_order_cancelled)
                )
            }
        }
    }

    fun resetState() {
        _state.update { VerifyCodeState() }
    }
}
