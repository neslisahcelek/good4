package com.good4.core.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.good4.navigation.Route
import com.good4.user.domain.UserRole

@Composable
fun SplashScreenRoot(
    viewModel: SplashViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: (UserRole) -> Unit,
    onNavigateToEmailVerification: () -> Unit
) {
    val startDestination by viewModel.startDestination.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    LaunchedEffect(startDestination, userRole) {
        startDestination?.let { destination ->
            when (destination) {
                Route.Login -> onNavigateToLogin()
                Route.AdminHome, Route.BusinessHome, Route.StudentHome -> {
                    userRole?.let { role ->
                        onNavigateToHome(role)
                    } ?: onNavigateToLogin()
                }
                Route.EmailVerification -> onNavigateToEmailVerification()
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
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = TextPrimary)
    }
}
