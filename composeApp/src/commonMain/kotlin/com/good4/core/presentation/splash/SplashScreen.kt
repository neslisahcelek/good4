package com.good4.core.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.navigation.Route
import com.good4.user.domain.UserRole

@Composable
fun SplashScreenRoot(
    viewModel: SplashViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: (UserRole) -> Unit,
    onNavigateToEmailVerification: () -> Unit,
    onNavigateToSessionRestore: () -> Unit
) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()

    LaunchedEffect(startDestination, userRole) {
        startDestination?.let { destination ->
            when (destination) {
                Route.Login -> onNavigateToLogin()
                Route.AdminHome, Route.BusinessHome, Route.StudentHome, Route.SupporterHome -> {
                    userRole?.let { role ->
                        onNavigateToHome(role)
                    } ?: onNavigateToLogin()
                }

                Route.EmailVerification -> onNavigateToEmailVerification()
                Route.SessionRestore -> onNavigateToSessionRestore()
                else -> Unit
            }
        }
    }

    SplashScreen()
}

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    )
}
