package com.good4.auth.presentation.verify_email

import com.good4.core.presentation.UiText
import com.good4.user.domain.UserRole

data class EmailVerificationState(
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val infoMessage: UiText? = null,
    val isVerified: Boolean = false,
    val userRole: UserRole? = null,
    val canResendEmail: Boolean = true,
    val resendCooldownSeconds: Int = 0
)
