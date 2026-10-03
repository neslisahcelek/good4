package com.good4.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.good4.auth.data.repository.AuthRepository
import com.good4.campuscloset.CampusEmailVerificationLinks
import org.koin.compose.koinInject
import com.good4.admin.presentation.home.AdminHomeScreenRoot
import com.good4.admin.presentation.profile.AdminProfileScreen
import com.good4.auth.presentation.login.LoginScreenRoot
import com.good4.auth.presentation.login.LoginViewModel
import com.good4.auth.presentation.register.RegisterOptionsScreen
import com.good4.auth.presentation.register.business.BusinessRegisterScreenRoot
import com.good4.auth.presentation.register.business.BusinessRegisterViewModel
import com.good4.auth.presentation.register.student.StudentRegisterScreenRoot
import com.good4.auth.presentation.register.student.StudentRegisterViewModel
import com.good4.auth.presentation.verify_email.EmailVerificationScreenRoot
import com.good4.auth.presentation.verify_email.EmailVerificationViewModel
import com.good4.business.presentation.home.BusinessHomeScreenRoot
import com.good4.business.presentation.profile.BusinessProfileScreen
import com.good4.calendar.AcademicCalendarScreen
import com.good4.core.presentation.sessionrestore.SessionRestoreScreenRoot
import com.good4.core.presentation.sessionrestore.SessionRestoreViewModel
import com.good4.core.presentation.splash.SplashScreenRoot
import com.good4.core.presentation.splash.SplashViewModel
import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend
import com.good4.student.presentation.home.StudentHomeScreenRoot
import com.good4.student.presentation.home.EditHomeScreen
import com.good4.notification.NotificationsScreen
import com.good4.campuscloset.CampusClosetChatScreen
import com.good4.campuscloset.CampusClosetInboxScreen
import com.good4.campuscloset.CampusClosetListingScreen
import com.good4.campuscloset.CampusClosetMyListingsScreen
import com.good4.campuscloset.CampusClosetNewListingScreen
import com.good4.campuscloset.CampusClosetScreen
import com.good4.student.presentation.profile.StudentProfileScreen
import com.good4.schedule.presentation.ClassScheduleScreen
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
    val primaryAuth: AuthRepository = koinInject()
    val primaryUser by primaryAuth.authStateFlow.collectAsState(primaryAuth.currentUser)
    val campusLink by CampusEmailVerificationLinks.pending.collectAsState()
    val currentEntry by navController.currentBackStackEntryAsState()
    val pushManager = androidx.compose.runtime.remember(primaryAuth) { com.good4.notification.PushRegistrationManager(primaryAuth) }
    val pushDestination by com.good4.notification.CampusPushNotifications.pending.collectAsState()
    if (AppEnvironment.firebaseBackend == FirebaseBackend.V2) {
        LaunchedEffect(pushManager) { pushManager.observe() }
        androidx.lifecycle.compose.LifecycleResumeEffect(primaryUser?.uid) {
            com.good4.notification.CampusPushNotifications.refresh()
            onPauseOrDispose { }
        }
        LaunchedEffect(pushDestination, primaryUser?.uid, currentEntry?.destination) {
            val notification = pushDestination ?: return@LaunchedEffect
            val uid = primaryUser?.uid ?: return@LaunchedEffect
            val destination = currentEntry?.destination ?: return@LaunchedEffect
            if (destination.hasRoute<Route.Splash>() || destination.hasRoute<Route.SessionRestore>()
                || destination.hasRoute<Route.Login>() || destination.hasRoute<Route.EmailVerification>()) return@LaunchedEffect
            com.good4.notification.CampusPushNotifications.consume(notification)
            if (notification.recipientUid != uid) return@LaunchedEffect
            when (notification.type) {
                "market_message" -> navController.navigate(Route.CampusClosetChat(notification.targetId)) { launchSingleTop = true }
                "market_listing" -> navController.navigate(Route.CampusClosetMyListings) { launchSingleTop = true }
            }
        }
    }
    LaunchedEffect(campusLink, primaryUser?.uid, currentEntry?.destination) {
        val destination = currentEntry?.destination
        if (campusLink != null && primaryUser != null && destination != null
            && !destination.hasRoute<Route.Splash>() && !destination.hasRoute<Route.SessionRestore>()
            && !destination.hasRoute<Route.Login>() && !destination.hasRoute<Route.EmailVerification>()
            && !destination.hasRoute<Route.CampusCloset>()) {
            navController.navigate(Route.CampusCloset) { launchSingleTop = true }
        }
    }
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
                    if (AppEnvironment.firebaseBackend == FirebaseBackend.V2) {
                        navController.navigate(Route.StudentRegister)
                    } else {
                        navController.navigate(Route.RegisterOptions)
                    }
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
                },
                onNavigateToNotifications = {
                    navController.navigate(Route.Notifications)
                },
                onNavigateToCalendar = {
                    navController.navigate(Route.AcademicCalendar)
                },
                onNavigateToCampusCloset = {
                    navController.navigate(Route.CampusCloset)
                },
                onNavigateToClassSchedule = {
                    navController.navigate(Route.ClassSchedule)
                },
                onNavigateToEditHome = { communityManager ->
                    navController.navigate(Route.EditHome(communityManager))
                }
            )
        }

        composable<Route.EditHome> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.EditHome>()
            EditHomeScreen(
                onBack = { navController.popBackStack() },
                communityManager = route.communityManager
            )
        }

        composable<Route.AcademicCalendar> {
            AcademicCalendarScreen(onBack = { navController.popBackStack() })
        }

        composable<Route.ClassSchedule> {
            ClassScheduleScreen(
                onBackClick = { navController.popBackStack() },
                onSelectAcademicProfile = {
                    navController.navigate(Route.StudentAccountSettings(academicSelectionPrompt = true))
                }
            )
        }

        composable<Route.CampusCloset> {
            CampusClosetScreen(
                onBack = { navController.popBackStack() },
                onOpenListing = { navController.navigate(Route.CampusClosetListing(it)) },
                onNewListing = { navController.navigate(Route.CampusClosetNewListing) },
                onOpenInbox = { navController.navigate(Route.CampusClosetInbox) },
                onOpenMyListings = { navController.navigate(Route.CampusClosetMyListings) }
            )
        }

        composable<Route.CampusClosetListing> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.CampusClosetListing>()
            CampusClosetListingScreen(
                listingId = route.listingId,
                onBack = { navController.popBackStack() },
                onOpenChat = { navController.navigate(Route.CampusClosetChat(it)) }
            )
        }

        composable<Route.CampusClosetNewListing> {
            CampusClosetNewListingScreen(
                onBack = { navController.popBackStack() },
                onOpenMyListings = {
                    navController.navigate(Route.CampusClosetMyListings) {
                        popUpTo<Route.CampusClosetNewListing> { inclusive = true }
                    }
                }
            )
        }

        composable<Route.CampusClosetMyListings> {
            CampusClosetMyListingsScreen(
                onBack = { navController.popBackStack() },
                onOpenListing = { navController.navigate(Route.CampusClosetListing(it)) }
            )
        }

        composable<Route.CampusClosetInbox> {
            CampusClosetInboxScreen(
                onBack = { navController.popBackStack() },
                onOpenChat = { navController.navigate(Route.CampusClosetChat(it)) }
            )
        }

        composable<Route.CampusClosetChat> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.CampusClosetChat>()
            CampusClosetChatScreen(
                conversationId = route.conversationId,
                onBack = { navController.popBackStack() },
                onOpenListing = { navController.navigate(Route.CampusClosetListing(it)) }
            )
        }

        composable<Route.Notifications> {
            NotificationsScreen(onBack = { navController.popBackStack() })
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

        composable<Route.WebPanelNotice> {
            com.good4.auth.presentation.webpanel.WebPanelNoticeScreen(
                onSignedOut = { navController.navigateToLogin() }
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

        composable<Route.StudentProfile> {
            StudentProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = { navController.navigateToLogin() },
                onOpenAccountSettings = {
                    navController.navigate(Route.StudentAccountSettings())
                }
            )
        }

        composable<Route.StudentAccountSettings> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.StudentAccountSettings>()
            AccountSettingsScreen(
                mode = AccountSettingsMode.STUDENT,
                academicSelectionPrompt = route.academicSelectionPrompt,
                onAcademicSelectionSaved = { navController.popBackStack() },
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
