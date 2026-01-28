package com.good4.student.presentation.profile

import com.good4.core.presentation.UiText
import com.good4.user.User

data class StudentProfileState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val errorMessage: String? = null,
    val deleteErrorMessage: UiText? = null,
    val isDeleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false,
    val isAccountDeleted: Boolean = false
)

