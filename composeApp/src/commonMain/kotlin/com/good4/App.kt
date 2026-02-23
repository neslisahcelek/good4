package com.good4

import androidx.compose.runtime.Composable
import com.good4.core.presentation.Good4Theme
import com.good4.navigation.Good4NavGraph
import com.good4.navigation.Route
import com.good4.user.domain.UserRole

@Composable
fun App(
    startDestination: Route = Route.Splash,
    currentUserRole: UserRole? = null
) {
    Good4Theme {
        Good4NavGraph(
            startDestination = startDestination,
            currentUserRole = currentUserRole
        )
    }
}
