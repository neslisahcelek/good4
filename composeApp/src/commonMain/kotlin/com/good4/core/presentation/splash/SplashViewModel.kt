package com.good4.core.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.util.Logger
import com.good4.navigation.Route
import com.good4.user.data.repository.UserRepository
import com.good4.user.domain.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock

class SplashViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val configRepository: AppConfigRepository,
    private val startupSessionCache: StartupSessionCache
) : ViewModel() {
    companion object {
        private const val TAG = "SplashViewModel"
        private const val ROUTE_DECISION_TIMEOUT_MS = 1_800L
        private const val USER_FETCH_TIMEOUT_MS = 1_500L
        private const val CONFIG_LOAD_TIMEOUT_MS = 3_000L
        private const val AUTH_RELOAD_TIMEOUT_MS = 1_000L
    }

    private val startupResolver = SplashStartupResolver(
        authRepository = authRepository,
        getUser = { uid -> userRepository.getUser(uid) },
        startupSessionCache = startupSessionCache
    )

    private val _startDestination = MutableStateFlow<Route?>(null)
    val startDestination = _startDestination.asStateFlow()

    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole = _userRole.asStateFlow()

    init {
        checkAppState()
    }

    private fun checkAppState() {
        viewModelScope.launch {
            val startupStartedAt = Clock.System.now().toEpochMilliseconds()
            Logger.d(TAG, "startup.started_at=$startupStartedAt")

            val resolution = withTimeoutOrNull(ROUTE_DECISION_TIMEOUT_MS) {
                startupResolver.resolve(
                    userFetchTimeoutMs = USER_FETCH_TIMEOUT_MS,
                    authReloadTimeoutMs = AUTH_RELOAD_TIMEOUT_MS
                )
            } ?: run {
                val fallbackRoute = if (authRepository.currentUser != null) {
                    Route.SessionRestore
                } else {
                    Route.Login
                }
                Logger.e(TAG, "Startup timed out, falling back to $fallbackRoute")
                StartupResolution(
                    decision = StartupDecision(
                        route = fallbackRoute,
                        userId = authRepository.currentUser?.uid
                    ),
                    authMs = 0L,
                    userFetchMs = 0L,
                    cacheHit = false
                )
            }

            val decision = resolution.decision
            _userRole.value = decision.userRole
            _startDestination.value = decision.route

            val totalMs = Clock.System.now().toEpochMilliseconds() - startupStartedAt
            Logger.d(TAG, "startup.auth_ms=${resolution.authMs}")
            Logger.d(TAG, "startup.user_fetch_ms=${resolution.userFetchMs}")
            Logger.d(TAG, "startup.cache_hit=${resolution.cacheHit}")
            Logger.d(TAG, "startup.total_ms=$totalMs")
            Logger.d(TAG, "startup.route=${decision.route}")

            launch {
                runDeferredStartupSync(decision.userId)
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

        if (userId != null) {
            Logger.d(TAG, "Deferred startup sync completed for uid=$userId")
        }
    }
}
