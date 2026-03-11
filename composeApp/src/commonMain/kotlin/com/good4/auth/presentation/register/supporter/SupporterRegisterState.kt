package com.good4.auth.presentation.register.supporter

import com.good4.core.presentation.UiText

data class SupporterRegisterState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isTermsAccepted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isRegisterSuccess: Boolean = false
)
