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
    val communityName: String = "",
    val isCommunityManager: Boolean = false,
    val phoneNumber: String = "",
    val university: String = "",
    val faculty: String = "",
    val major: String = "",
    val classYear: String = "",
    val educationLevel: String = "",
    val businessName: String = "",
    val businessPhone: String = "",
    val email: String = "",
    val universities: List<String> = emptyList(),
    val isPasswordResetEmailSent: Boolean = false,
    val canResendPasswordReset: Boolean = true,
    /** Google/Apple accounts have no password, so the reset card is hidden for them. */
    val hasPasswordSignIn: Boolean = false,
    val passwordResetCooldownSeconds: Int = 0,
    val showPhoneField: Boolean = false,
    val profileSaveCount: Int = 0,
    val errorMessage: UiText? = null,
    val infoMessage: UiText? = null
)

enum class AccountSettingsMode {
    STUDENT,
    BUSINESS,
    ADMIN
}
