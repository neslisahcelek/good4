package com.good4.auth.presentation.register.student

sealed interface StudentRegisterAction {
    data class OnEmailChange(val email: String) : StudentRegisterAction
    data class OnPasswordChange(val password: String) : StudentRegisterAction
    data class OnConfirmPasswordChange(val confirmPassword: String) : StudentRegisterAction
    data class OnFullNameChange(val fullName: String) : StudentRegisterAction
    data class OnUniversityChange(val university: String) : StudentRegisterAction
    data class OnMajorChange(val major: String) : StudentRegisterAction
    data class OnEducationLevelChange(val educationLevel: String) : StudentRegisterAction
    data object OnTogglePasswordVisibility : StudentRegisterAction
    data object OnRegisterClick : StudentRegisterAction
    data object OnBackClick : StudentRegisterAction
    data object OnClearError : StudentRegisterAction
}

