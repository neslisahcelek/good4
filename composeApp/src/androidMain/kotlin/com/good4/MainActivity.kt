package com.good4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.good4.auth.data.repository.AuthRepository
import com.good4.config.data.repository.AppConfigRepository
import com.good4.navigation.Good4NavGraph
import com.good4.navigation.Route
import com.good4.user.data.repository.UserRepository
import com.good4.user.domain.UserRole
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val configRepository: AppConfigRepository = koinInject()
            val authRepository: AuthRepository = koinInject()
            val userRepository: UserRepository = koinInject()

            var startDestination by remember { mutableStateOf<Route?>(null) }
            var userRole by remember { mutableStateOf<UserRole?>(null) }

            LaunchedEffect(Unit) {
                configRepository.loadConfig()

                val currentUser = authRepository.currentUser
                if (currentUser != null) {
                    when (val result = userRepository.getUser(currentUser.uid)) {
                        is com.good4.core.domain.Result.Success -> {
                            userRole = result.data.role
                            startDestination = when (result.data.role) {
                                UserRole.ADMIN -> Route.AdminHome
                                UserRole.BUSINESS -> Route.BusinessHome
                                UserRole.STUDENT -> Route.StudentHome
                            }
                        }

                        is com.good4.core.domain.Result.Error -> {
                            startDestination = Route.Login
                        }
                    }
                } else {
                    startDestination = Route.Login
                }
            }

            startDestination?.let { destination ->
                MaterialTheme {
                    Good4NavGraph(
                        startDestination = destination,
                        currentUserRole = userRole
                    )
                }
            }
        }
    }
}

