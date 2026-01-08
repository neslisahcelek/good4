package com.good4

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.good4.auth.data.repository.AuthRepository
import com.good4.core.domain.Result
import com.good4.core.presentation.SplashScreen
import com.good4.navigation.Good4NavGraph
import com.good4.navigation.Route
import com.good4.user.data.repository.UserRepository
import com.good4.user.domain.UserRole
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val authRepository: AuthRepository = koinInject()
            val userRepository: UserRepository = koinInject()

            var isLoading by remember { mutableStateOf(true) }
            var startDestination by remember { mutableStateOf<Route>(Route.Splash) }

            LaunchedEffect(Unit) {
                val currentUser = authRepository.currentUser
                if (currentUser != null) {
                    when (val result = userRepository.getUser(currentUser.uid)) {
                        is Result.Success -> {
                            val role = result.data.role
                            startDestination = when (role) {
                                UserRole.ADMIN -> Route.AdminHome
                                UserRole.BUSINESS -> Route.BusinessHome
                                UserRole.STUDENT -> Route.StudentHome
                            }
                        }
                        is Result.Error -> {
                            authRepository.signOut()
                            startDestination = Route.Login
                        }
                    }
                } else {
                    startDestination = Route.Login
                }
                isLoading = false
            }

            if (isLoading) {
                SplashScreen()
            } else {
                Good4NavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}