package com.good4.auth.presentation.verify_email

sealed interface EmailVerificationAction {
    data object OnResendClick : EmailVerificationAction
    data object OnCheckClick : EmailVerificationAction
    data object OnClearError : EmailVerificationAction
    data object OnClearInfo : EmailVerificationAction
    data object OnLogoutClick : EmailVerificationAction
}
