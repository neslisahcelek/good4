package com.good4.auth.presentation.register.business

sealed interface BusinessRegisterAction {
    data class OnEmailChange(val email: String) : BusinessRegisterAction
    data class OnPasswordChange(val password: String) : BusinessRegisterAction
    data class OnConfirmPasswordChange(val confirmPassword: String) : BusinessRegisterAction
    data class OnFullNameChange(val fullName: String) : BusinessRegisterAction
    data class OnPhoneNumberChange(val phoneNumber: String) : BusinessRegisterAction

    data class OnBusinessNameChange(val businessName: String) : BusinessRegisterAction
    data class OnBusinessPhoneChange(val businessPhone: String) : BusinessRegisterAction
    data class OnAddressChange(val address: String) : BusinessRegisterAction
    data class OnAddressUrlChange(val addressUrl: String) : BusinessRegisterAction
    data class OnCityChange(val city: String) : BusinessRegisterAction
    data class OnDistrictChange(val district: String) : BusinessRegisterAction

    data object OnTogglePasswordVisibility : BusinessRegisterAction
    data object OnToggleTermsAccepted : BusinessRegisterAction
    data object OnRegisterClick : BusinessRegisterAction
    data object OnBackClick : BusinessRegisterAction
    data object OnClearError : BusinessRegisterAction
}
