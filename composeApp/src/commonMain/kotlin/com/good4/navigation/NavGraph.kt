package com.good4.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.good4.admin.presentation.home.AdminHomeScreenRoot
import com.good4.auth.presentation.login.LoginScreenRoot
import com.good4.auth.presentation.login.LoginViewModel
import com.good4.auth.presentation.register.business.BusinessRegisterScreenRoot
import com.good4.auth.presentation.register.business.BusinessRegisterViewModel
import com.good4.auth.presentation.register.student.StudentRegisterScreenRoot
import com.good4.auth.presentation.register.student.StudentRegisterViewModel
import com.good4.auth.presentation.register.supporter.SupporterRegisterScreenRoot
import com.good4.auth.presentation.register.supporter.SupporterRegisterViewModel
import com.good4.auth.presentation.verify_email.EmailVerificationScreenRoot
import com.good4.auth.presentation.verify_email.EmailVerificationViewModel
import com.good4.business.presentation.home.BusinessHomeScreenRoot
import com.good4.core.presentation.splash.SplashScreenRoot
import com.good4.core.presentation.splash.SplashViewModel
import com.good4.student.presentation.home.StudentHomeScreenRoot
import com.good4.supporter.presentation.home.SupporterHomeScreenRoot
import com.good4.supporter.presentation.ordercode.SupporterOrderCodeScreenRoot
import com.good4.supporter.presentation.ordercode.SupporterOrderCodeViewModel
import com.good4.user.domain.UserRole
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun Good4NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Route = Route.Login,
    onSplashReady: (() -> Unit)? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Splash
        composable<Route.Splash> {
            val viewModel: SplashViewModel = koinViewModel()
            SplashScreenRoot(
                viewModel = viewModel,
                onNavigateToLogin = {
                    onSplashReady?.invoke()
                    navController.navigateToLogin()
                },
                onNavigateToHome = { userRole ->
                    onSplashReady?.invoke()
                    navController.navigateToHomeFromSplash(userRole)
                },
                onNavigateToEmailVerification = {
                    onSplashReady?.invoke()
                    navController.navigate(Route.EmailVerification) {
                        popUpTo(Route.Splash) { inclusive = true }
                    }
                }
            )
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
                },
                onNavigateToSupporterRegister = {
                    navController.navigate(Route.SupporterRegister)
                },
                onNavigateToEmailVerification = {
                    navController.navigate(Route.EmailVerification)
                }
            )
        }

        composable<Route.StudentRegister> {
            val viewModel: StudentRegisterViewModel = koinViewModel()
            StudentRegisterScreenRoot(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigate(Route.EmailVerification)
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

        composable<Route.EmailVerification> {
            val viewModel: EmailVerificationViewModel = koinViewModel()
            EmailVerificationScreenRoot(
                viewModel = viewModel,
                onVerified = { userRole ->
                    navController.navigateToHome(userRole)
                },
                onLogout = {
                    navController.navigateToLogin()
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

        // Supporter Routes
        composable<Route.SupporterRegister> {
            val viewModel: SupporterRegisterViewModel = koinViewModel()
            SupporterRegisterScreenRoot(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigate(Route.EmailVerification)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable<Route.SupporterHome> {
            SupporterHomeScreenRoot(
                onLogout = { navController.navigateToLogin() },
                onNavigateToOrderCode = { orderId ->
                    navController.navigate(Route.SupporterOrderCode(orderId))
                }
            )
        }

        composable<Route.SupporterOrderCode> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.SupporterOrderCode>()
            val viewModel: SupporterOrderCodeViewModel = koinViewModel()
            SupporterOrderCodeScreenRoot(
                orderId = route.orderId,
                viewModel = viewModel,
                onBackToHome = {
                    navController.navigate(Route.SupporterHome) {
                        popUpTo(Route.SupporterHome) { inclusive = true }
                    }
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
        UserRole.SUPPORTER -> Route.SupporterHome
    }
    navigate(destination) {
        popUpTo(Route.Login) { inclusive = true }
    }
}

fun NavHostController.navigateToHomeFromSplash(userRole: UserRole) {
    val destination = when (userRole) {
        UserRole.ADMIN -> Route.AdminHome
        UserRole.BUSINESS -> Route.BusinessHome
        UserRole.STUDENT -> Route.StudentHome
        UserRole.SUPPORTER -> Route.SupporterHome
    }
    navigate(destination) {
        popUpTo(Route.Splash) { inclusive = true }
    }
}

fun NavHostController.navigateToLogin() {
    navigate(Route.Login) {
        popUpTo(0) { inclusive = true }
    }
}
