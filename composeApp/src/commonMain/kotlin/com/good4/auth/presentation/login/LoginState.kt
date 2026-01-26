package com.good4.auth.presentation.login

import com.good4.core.presentation.UiText
import com.good4.user.domain.UserRole

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val errorMessage: UiText? = null,
    val infoMessage: UiText? = null,
    val isLoginSuccess: Boolean = false,
    val userRole: UserRole? = null
)

