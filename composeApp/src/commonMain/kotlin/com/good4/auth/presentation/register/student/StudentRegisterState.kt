package com.good4.auth.presentation.register.student

import com.good4.core.presentation.UiText

data class StudentRegisterState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val university: String = "",
    val major: String = "",
    val educationLevel: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isRegisterSuccess: Boolean = false
)

