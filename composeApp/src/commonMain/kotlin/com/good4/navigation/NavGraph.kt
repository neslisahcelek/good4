package com.good4.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.good4.admin.presentation.home.AdminHomeScreenRoot
import com.good4.admin.presentation.profile.AdminProfileScreen
import com.good4.auth.presentation.login.LoginScreenRoot
import com.good4.auth.presentation.login.LoginViewModel
import com.good4.auth.presentation.register.RegisterOptionsScreen
import com.good4.auth.presentation.register.business.BusinessRegisterScreenRoot
import com.good4.auth.presentation.register.business.BusinessRegisterViewModel
import com.good4.auth.presentation.register.student.StudentRegisterScreenRoot
import com.good4.auth.presentation.register.student.StudentRegisterViewModel
import com.good4.auth.presentation.register.supporter.SupporterRegisterScreenRoot
import com.good4.auth.presentation.register.supporter.SupporterRegisterViewModel
import com.good4.auth.presentation.verify_email.EmailVerificationScreenRoot
import com.good4.auth.presentation.verify_email.EmailVerificationViewModel
import com.good4.business.presentation.home.BusinessHomeScreenRoot
import com.good4.business.presentation.profile.BusinessProfileScreen
import com.good4.core.presentation.sessionrestore.SessionRestoreScreenRoot
import com.good4.core.presentation.sessionrestore.SessionRestoreViewModel
import com.good4.core.presentation.splash.SplashScreenRoot
import com.good4.core.presentation.splash.SplashViewModel
import com.good4.student.presentation.home.StudentHomeScreenRoot
import com.good4.student.presentation.profile.StudentProfileScreen
import com.good4.supporter.presentation.home.SupporterHomeScreenRoot
import com.good4.supporter.presentation.ordercode.SupporterOrderCodeScreenRoot
import com.good4.supporter.presentation.ordercode.SupporterOrderCodeViewModel
import com.good4.supporter.presentation.profile.SupporterProfileScreen
import com.good4.user.domain.UserRole
import com.good4.user.presentation.accountsettings.AccountSettingsMode
import com.good4.user.presentation.accountsettings.AccountSettingsScreen
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
                },
                onNavigateToSessionRestore = {
                    onSplashReady?.invoke()
                    navController.navigate(Route.SessionRestore) {
                        popUpTo(Route.Splash) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.SessionRestore> {
            val viewModel: SessionRestoreViewModel = koinViewModel()
            SessionRestoreScreenRoot(
                viewModel = viewModel,
                onNavigateToLogin = {
                    navController.navigateToLogin()
                },
                onNavigateToHome = { role ->
                    navController.navigate(role.toHomeRoute()) {
                        popUpTo(Route.SessionRestore) { inclusive = true }
                    }
                },
                onNavigateToEmailVerification = {
                    navController.navigate(Route.EmailVerification) {
                        popUpTo(Route.SessionRestore) { inclusive = true }
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
                onNavigateToRegisterOptions = {
                    navController.navigate(Route.RegisterOptions)
                },
                onNavigateToEmailVerification = {
                    navController.navigate(Route.EmailVerification)
                }
            )
        }

        composable<Route.RegisterOptions> {
            RegisterOptionsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToStudentRegister = {
                    navController.navigate(Route.StudentRegister)
                },
                onNavigateToBusinessRegister = {
                    navController.navigate(Route.BusinessRegister)
                },
                onNavigateToSupporterRegister = {
                    navController.navigate(Route.SupporterRegister)
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
                onNavigateToProfile = {
                    navController.navigate(Route.StudentProfile)
                }
            )
        }

        // Business Routes
        composable<Route.BusinessHome> {
            BusinessHomeScreenRoot(
                onLogout = {
                    navController.navigateToLogin()
                },
                onNavigateToProfile = {
                    navController.navigate(Route.BusinessProfile)
                }
            )
        }

        // Admin Routes
        composable<Route.AdminHome> {
            AdminHomeScreenRoot(
                onNavigateToProfile = {
                    navController.navigate(Route.AdminProfile)
                }
            )
        }

        // Supporter Routes
        composable<Route.SupporterRegister> {
            val viewModel: SupporterRegisterViewModel = koinViewModel()
            SupporterRegisterScreenRoot(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigateToHome(UserRole.SUPPORTER)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable<Route.SupporterHome> {
            SupporterHomeScreenRoot(
                onNavigateToProfile = {
                    navController.navigate(Route.SupporterProfile)
                },
                onNavigateToOrderCode = { orderId ->
                    navController.navigate(Route.SupporterOrderCode(orderId))
                }
            )
        }

        composable<Route.StudentProfile> {
            StudentProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = { navController.navigateToLogin() },
                onOpenAccountSettings = {
                    navController.navigate(Route.StudentAccountSettings)
                }
            )
        }

        composable<Route.StudentAccountSettings> {
            AccountSettingsScreen(
                mode = AccountSettingsMode.STUDENT,
                onBackClick = { navController.popBackStack() },
                onLogout = { navController.navigateToLogin() }
            )
        }

        composable<Route.BusinessProfile> {
            BusinessProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = { navController.navigateToLogin() },
                onOpenAccountSettings = {
                    navController.navigate(Route.BusinessAccountSettings)
                }
            )
        }

        composable<Route.BusinessAccountSettings> {
            AccountSettingsScreen(
                mode = AccountSettingsMode.BUSINESS,
                onBackClick = { navController.popBackStack() },
                onLogout = { navController.navigateToLogin() }
            )
        }

        composable<Route.SupporterProfile> {
            SupporterProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = { navController.navigateToLogin() },
                onOpenAccountSettings = {
                    navController.navigate(Route.SupporterAccountSettings)
                }
            )
        }

        composable<Route.SupporterAccountSettings> {
            AccountSettingsScreen(
                mode = AccountSettingsMode.SUPPORTER,
                onBackClick = { navController.popBackStack() },
                onLogout = { navController.navigateToLogin() }
            )
        }

        composable<Route.AdminProfile> {
            AdminProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = { navController.navigateToLogin() },
                onOpenAccountSettings = {
                    navController.navigate(Route.AdminAccountSettings)
                }
            )
        }

        composable<Route.AdminAccountSettings> {
            AccountSettingsScreen(
                mode = AccountSettingsMode.ADMIN,
                onBackClick = { navController.popBackStack() },
                onLogout = { navController.navigateToLogin() }
            )
        }

        composable<Route.SupporterOrderCode> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.SupporterOrderCode>()
            val viewModel: SupporterOrderCodeViewModel = koinViewModel()
            SupporterOrderCodeScreenRoot(
                orderId = route.orderId,
                viewModel = viewModel,
                onBackToHome = {
                    val popped = navController.popBackStack()
                    if (!popped) {
                        navController.navigate(Route.SupporterHome) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
}

fun NavHostController.navigateToHome(userRole: UserRole) {
    val destination = userRole.toHomeRoute()
    navigate(destination) {
        popUpTo(Route.Login) { inclusive = true }
    }
}

fun NavHostController.navigateToHomeFromSplash(userRole: UserRole) {
    val destination = userRole.toHomeRoute()
    navigate(destination) {
        popUpTo(Route.Splash) { inclusive = true }
    }
}

fun NavHostController.navigateToLogin() {
    navigate(Route.Login) {
        popUpTo(0) { inclusive = true }
    }
}
