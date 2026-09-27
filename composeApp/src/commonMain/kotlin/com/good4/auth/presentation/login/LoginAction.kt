package com.good4.auth.presentation.login

sealed interface LoginAction {
    data class OnGoogleToken(val token: String, val accessToken: String? = null) : LoginAction
    data class OnGoogleError(val message: String) : LoginAction
    data class OnAppleCredential(val idToken: String, val rawNonce: String) : LoginAction
    data class OnAppleError(val message: String) : LoginAction
    data class OnEmailChange(val email: String) : LoginAction
    data class OnPasswordChange(val password: String) : LoginAction
    data object OnTogglePasswordVisibility : LoginAction
    data object OnToggleUserAgreementAccepted : LoginAction
    data object OnToggleKvkkNoticeAcknowledged : LoginAction
    data object OnCompleteLegalRegistration : LoginAction
    data object OnCancelLegalRegistration : LoginAction
    data object OnLoginClick : LoginAction
    data object OnStudentRegisterClick : LoginAction
    data object OnBusinessRegisterClick : LoginAction
    data object OnForgotPasswordClick : LoginAction
    data object OnClearError : LoginAction
    data object OnClearInfo : LoginAction
}
