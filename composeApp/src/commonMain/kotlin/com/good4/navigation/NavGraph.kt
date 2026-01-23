package com.good4.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.good4.admin.presentation.home.AdminHomeScreenRoot
import com.good4.auth.presentation.login.LoginScreenRoot
import com.good4.auth.presentation.login.LoginViewModel
import com.good4.auth.presentation.register.business.BusinessRegisterScreenRoot
import com.good4.auth.presentation.register.business.BusinessRegisterViewModel
import com.good4.auth.presentation.register.student.StudentRegisterScreenRoot
import com.good4.auth.presentation.register.student.StudentRegisterViewModel
import com.good4.business.presentation.home.BusinessHomeScreenRoot
import com.good4.core.presentation.SplashScreen
import com.good4.student.presentation.home.StudentHomeScreenRoot
import com.good4.user.domain.UserRole
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun Good4NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Route = Route.Login,
    currentUserRole: UserRole? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Splash
        composable<Route.Splash> {
            SplashScreen()
        }
        
        // Auth
        composable<Route.Login> {
            val viewModel: LoginViewModel = koinViewModel()
            LoginScreenRoot(
                viewModel = viewModel,
                onLoginSuccess = { userRole ->
                    navController.navigateToHome(userRole)
                },
                onNavigateToStudentRegister = {
                    navController.navigate(Route.StudentRegister)
                },
                onNavigateToBusinessRegister = {
                    navController.navigate(Route.BusinessRegister)
                }
            )
        }

        composable<Route.StudentRegister> {
            val viewModel: StudentRegisterViewModel = koinViewModel()
            StudentRegisterScreenRoot(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigateToHome(UserRole.STUDENT)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable<Route.BusinessRegister> {
            val viewModel: BusinessRegisterViewModel = koinViewModel()
            BusinessRegisterScreenRoot(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigateToHome(UserRole.BUSINESS)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Student Routes
        composable<Route.StudentHome> {
            StudentHomeScreenRoot(
                onLogout = {
                    navController.navigateToLogin()
                }
            )
        }

        // Business Routes
        composable<Route.BusinessHome> {
            BusinessHomeScreenRoot(
                onLogout = {
                    navController.navigateToLogin()
                }
            )
        }

        // Admin Routes
        composable<Route.AdminHome> {
            AdminHomeScreenRoot(
                onLogout = {
                    navController.navigateToLogin()
                }
            )
        }
    }
}

fun NavHostController.navigateToHome(userRole: UserRole) {
    val destination = when (userRole) {
        UserRole.ADMIN -> Route.AdminHome
        UserRole.BUSINESS -> Route.BusinessHome
        UserRole.STUDENT -> Route.StudentHome
    }
    navigate(destination) {
        popUpTo(Route.Login) { inclusive = true }
    }
}

fun NavHostController.navigateToLogin() {
    navigate(Route.Login) {
        popUpTo(0) { inclusive = true }
    }
}
