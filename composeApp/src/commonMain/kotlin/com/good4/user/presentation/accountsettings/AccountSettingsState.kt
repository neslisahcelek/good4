package com.good4.user.presentation.accountsettings

import com.good4.core.presentation.UiText

data class AccountSettingsState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSendingPasswordReset: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleteDialogVisible: Boolean = false,
    val isAccountDeleted: Boolean = false,
    val isLoggedOut: Boolean = false,
    val fullName: String = "",
    val phoneNumber: String = "",
    val university: String = "",
    val major: String = "",
    val educationLevel: String = "",
    val businessName: String = "",
    val businessPhone: String = "",
    val email: String = "",
    val universities: List<String> = emptyList(),
    val isPasswordResetEmailSent: Boolean = false,
    val showPhoneField: Boolean = false,
    val errorMessage: UiText? = null,
    val infoMessage: UiText? = null
)

enum class AccountSettingsMode {
    STUDENT,
    BUSINESS,
    SUPPORTER,
    ADMIN
}
