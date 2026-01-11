package com.good4.auth.presentation.register.student

import com.good4.core.presentation.UiText
import org.jetbrains.compose.resources.StringResource

data class StudentRegisterState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val university: String = "",
    val major: String = "",
    val educationLevel: String = "",
    val educationLevels: List<StringResource> = emptyList(),
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isRegisterSuccess: Boolean = false
)

