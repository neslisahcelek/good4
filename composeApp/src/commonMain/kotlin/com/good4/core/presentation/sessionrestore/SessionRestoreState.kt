package com.good4.core.presentation.sessionrestore

import com.good4.core.presentation.UiText
import com.good4.navigation.Route
import com.good4.user.domain.UserRole

data class SessionRestoreState(
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
    val targetRoute: Route? = null,
    val userRole: UserRole? = null
)
