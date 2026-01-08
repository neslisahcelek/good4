package com.good4.auth.presentation.register.business

import com.good4.core.presentation.UiText

data class BusinessRegisterState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val businessName: String = "",
    val businessPhone: String = "",
    val businessEmail: String = "",
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val description: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isRegisterSuccess: Boolean = false
)

