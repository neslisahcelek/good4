package com.good4.supporter.presentation.profile

import com.good4.core.presentation.UiText
import com.good4.user.User

data class SupporterProfileState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val deleteErrorMessage: UiText? = null,
    val isDeleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false,
    val isAccountDeleted: Boolean = false
)
