package com.good4.business.presentation.verify

data class VerifyCodeState(
    val codeInput: String = "",
    val isLoading: Boolean = false,
    val verificationSuccess: Boolean = false,
    val verifiedProductName: String? = null,
    val errorMessage: String? = null
)

