package com.good4.business.presentation.verify

import com.good4.order.domain.Order

data class VerifyCodeState(
    val codeInput: String = "",
    val isLoading: Boolean = false,
    val verificationSuccess: Boolean = false,
    val verifiedProductName: String? = null,
    val pendingOrder: Order? = null,
    val isConfirmingOrder: Boolean = false,
    val isCancellingOrder: Boolean = false,
    val orderConfirmedSuccess: Boolean = false,
    val orderCancelledSuccess: Boolean = false,
    val errorMessage: String? = null
)
