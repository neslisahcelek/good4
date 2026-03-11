package com.good4.auth.presentation.login

sealed interface LoginAction {
    data class OnEmailChange(val email: String) : LoginAction
    data class OnPasswordChange(val password: String) : LoginAction
    data object OnTogglePasswordVisibility : LoginAction
    data object OnLoginClick : LoginAction
    data object OnStudentRegisterClick : LoginAction
    data object OnBusinessRegisterClick : LoginAction
    data object OnSupporterRegisterClick : LoginAction
    data object OnForgotPasswordClick : LoginAction
    data object OnClearError : LoginAction
    data object OnClearInfo : LoginAction
}

