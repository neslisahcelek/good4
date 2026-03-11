package com.good4.core.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.domain.Result
import com.good4.core.util.AppEnvironment
import com.good4.core.util.Logger
import com.good4.navigation.Route
import com.good4.user.data.repository.UserRepository
import com.good4.user.domain.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock

class SplashViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val configRepository: AppConfigRepository
) : ViewModel() {
    companion object {
        private const val TAG = "SplashViewModel"
        private const val STARTUP_TIMEOUT_MS = 10_000L
        private const val CONFIG_LOAD_TIMEOUT_MS = 3_000L
        private const val AUTH_RELOAD_TIMEOUT_MS = 2_500L
    }

    private val _startDestination = MutableStateFlow<Route?>(null)
    val startDestination = _startDestination.asStateFlow()

    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole = _userRole.asStateFlow()

    private data class StartupDecision(
        val route: Route,
        val userRole: UserRole? = null,
        val userId: String? = null
    )

    init {
        checkAppState()
    }

    private fun checkAppState() {
        viewModelScope.launch {
            val startupStartedAt = Clock.System.now().toEpochMilliseconds()
            Logger.d(TAG, "startup_check_started_at=$startupStartedAt")

            val decision = withTimeoutOrNull(STARTUP_TIMEOUT_MS) {
                resolveStartupDecision()
            } ?: run {
                Logger.e(TAG, "Startup timed out, falling back to Login")
                StartupDecision(route = Route.Login)
            }

            _userRole.value = decision.userRole
            _startDestination.value = decision.route

            val elapsed = Clock.System.now().toEpochMilliseconds() - startupStartedAt
            Logger.d(TAG, "startup_route_resolved route=${decision.route} elapsed_ms=$elapsed")

            launch {
                runDeferredStartupSync(decision.userId)
            }
        }
    }

    private suspend fun resolveStartupDecision(): StartupDecision {
        val currentUser = authRepository.authStateFlow.first() ?: return StartupDecision(route = Route.Login)
        return when (val result = userRepository.getUser(currentUser.uid)) {
            is Result.Success -> {
                val user = result.data
                val shouldCheckEmailVerification =
                    AppEnvironment.isEmailVerificationRequired &&
                            (user.role == UserRole.STUDENT || user.role == UserRole.SUPPORTER)

                val isAuthVerified = if (shouldCheckEmailVerification) {
                    withTimeoutOrNull(AUTH_RELOAD_TIMEOUT_MS) {
                        when (val reloadResult = authRepository.reloadCurrentUser()) {
                            is Result.Success -> reloadResult.data.isEmailVerified
                            is Result.Error -> currentUser.isEmailVerified
                        }
                    } ?: currentUser.isEmailVerified
                } else {
                    true
                }

                if (shouldCheckEmailVerification && (isAuthVerified.not() || user.verified.not())) {
                    StartupDecision(
                        route = Route.EmailVerification,
                        userRole = user.role,
                        userId = currentUser.uid
                    )
                } else {
                    StartupDecision(
                        route = roleToHomeRoute(user.role),
                        userRole = user.role,
                        userId = currentUser.uid
                    )
                }
            }

            is Result.Error -> {
                Logger.e(TAG, "getUser failed for uid=${currentUser.uid}, error=${result.error}")
                StartupDecision(route = Route.Login)
            }
        }
    }

    private suspend fun runDeferredStartupSync(userId: String?) {
        val configLoaded = withTimeoutOrNull(CONFIG_LOAD_TIMEOUT_MS) {
            configRepository.loadConfig()
            true
        } ?: false

        if (!configLoaded) {
            Logger.e(TAG, "Config load timed out, using default config values")
        }

        if (userId == null) {
            return
        }

        when (val result = userRepository.refreshStudentCreditIfNeeded(userId)) {
            is Result.Success -> Logger.d(TAG, "Deferred credit sync completed")
            is Result.Error -> Logger.e(TAG, "Deferred credit sync failed")
        }
    }

    private fun roleToHomeRoute(role: UserRole): Route {
        return when (role) {
            UserRole.ADMIN -> Route.AdminHome
            UserRole.BUSINESS -> Route.BusinessHome
            UserRole.STUDENT -> Route.StudentHome
            UserRole.SUPPORTER -> Route.SupporterHome
        }
    }
}
