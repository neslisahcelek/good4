package com.good4.auth.presentation.register.supporter

sealed interface SupporterRegisterAction {
    data class OnFullNameChange(val fullName: String) : SupporterRegisterAction
    data class OnEmailChange(val email: String) : SupporterRegisterAction
    data class OnPasswordChange(val password: String) : SupporterRegisterAction
    data class OnConfirmPasswordChange(val confirmPassword: String) : SupporterRegisterAction
    data object OnTogglePasswordVisibility : SupporterRegisterAction
    data object OnToggleTermsAccepted : SupporterRegisterAction
    data object OnRegisterClick : SupporterRegisterAction
    data object OnBackClick : SupporterRegisterAction
    data object OnClearError : SupporterRegisterAction
}
