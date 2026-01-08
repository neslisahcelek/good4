package com.good4.auth.presentation.register.business

sealed interface BusinessRegisterAction {
    // Kullanıcı bilgileri
    data class OnEmailChange(val email: String) : BusinessRegisterAction
    data class OnPasswordChange(val password: String) : BusinessRegisterAction
    data class OnConfirmPasswordChange(val confirmPassword: String) : BusinessRegisterAction
    data class OnFullNameChange(val fullName: String) : BusinessRegisterAction
    data class OnPhoneNumberChange(val phoneNumber: String) : BusinessRegisterAction

    // İşletme bilgileri
    data class OnBusinessNameChange(val businessName: String) : BusinessRegisterAction
    data class OnBusinessPhoneChange(val businessPhone: String) : BusinessRegisterAction
    data class OnBusinessEmailChange(val businessEmail: String) : BusinessRegisterAction
    data class OnAddressChange(val address: String) : BusinessRegisterAction
    data class OnCityChange(val city: String) : BusinessRegisterAction
    data class OnDistrictChange(val district: String) : BusinessRegisterAction
    data class OnDescriptionChange(val description: String) : BusinessRegisterAction

    data object OnTogglePasswordVisibility : BusinessRegisterAction
    data object OnRegisterClick : BusinessRegisterAction
    data object OnBackClick : BusinessRegisterAction
    data object OnClearError : BusinessRegisterAction
}

