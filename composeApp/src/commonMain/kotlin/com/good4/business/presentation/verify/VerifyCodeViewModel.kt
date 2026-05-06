package com.good4.business.presentation.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.code.data.repository.CodeRepository
import com.good4.core.domain.Result
import com.good4.order.data.repository.OrderRepository
import com.good4.order.domain.OrderStatus
import com.good4.order.domain.isExpired
import com.good4.product.data.repository.FirestoreProductRepository
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_business_not_found
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
                    verificationSuccess = false,
                    orderConfirmedSuccess = false,
                    orderCancelledSuccess = false,
                    pendingOrder = null
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
                    verificationSuccess = false,
                    orderConfirmedSuccess = false,
                    orderCancelledSuccess = false,
                    pendingOrder = null
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
                    tryVerifyAsOrder(code, bid)
                }
            }
        }
    }

    private suspend fun tryVerifyAsOrder(code: String, bid: String) {
        when (val orderResult = orderRepository.getOrderByCodeAndBusiness(code, bid)) {
            is Result.Success -> {
                val order = orderResult.data
                if (order != null && !order.isExpired()) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            pendingOrder = order,
                            orderConfirmedSuccess = false,
                            orderCancelledSuccess = false
                        )
                    }
                } else {
                    if (order != null) {
                        orderRepository.updateOrderStatus(order.id, OrderStatus.EXPIRED)
                    }
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
        _state.update {
            it.copy(
                codeInput = "",
                isLoading = false,
                verificationSuccess = false,
                verifiedProductName = null,
                pendingOrder = null,
                isConfirmingOrder = false,
                isCancellingOrder = false,
                orderConfirmedSuccess = false,
                orderCancelledSuccess = false,
                errorMessage = null
            )
        }
    }
}
