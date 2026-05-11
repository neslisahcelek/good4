package com.good4.core.presentation.splash

import com.good4.auth.data.repository.AuthRepository
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.data.local.cacheStartupSession
import com.good4.core.data.local.shouldCheckEmailVerificationFor
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.navigation.Route
import com.good4.navigation.toHomeRoute
import com.good4.user.User
import com.good4.user.domain.UserRole
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock

internal data class StartupDecision(
    val route: Route,
    val userRole: UserRole? = null,
    val userId: String? = null
)

internal data class StartupResolution(
    val decision: StartupDecision,
    val authMs: Long,
    val userFetchMs: Long,
    val cacheHit: Boolean
)

internal class SplashStartupResolver(
    private val authRepository: AuthRepository,
    private val getUser: suspend (String) -> Result<User, Error>,
    private val startupSessionCache: StartupSessionCache
) {

    suspend fun resolve(
        userFetchTimeoutMs: Long,
        authReloadTimeoutMs: Long
    ): StartupResolution {
        val authStartedAt = nowEpochMillis()
        val currentUser = authRepository.currentUser
        val authMs = nowEpochMillis() - authStartedAt

        if (currentUser == null) {
            return StartupResolution(
                decision = StartupDecision(route = Route.Login),
                authMs = authMs,
                userFetchMs = 0L,
                cacheHit = false
            )
        }

        val cachedSession = startupSessionCache.read(currentUser.uid)
        val userFetchStartedAt = nowEpochMillis()
        val networkDecision = withTimeoutOrNull(userFetchTimeoutMs) {
            resolveFromNetwork(
                userId = currentUser.uid,
                isAuthEmailVerified = currentUser.isEmailVerified,
                authReloadTimeoutMs = authReloadTimeoutMs
            )
        }
        val userFetchMs = nowEpochMillis() - userFetchStartedAt

        if (networkDecision != null) {
            return StartupResolution(
                decision = networkDecision,
                authMs = authMs,
                userFetchMs = userFetchMs,
                cacheHit = false
            )
        }

        if (cachedSession != null) {
            val route = if (cachedSession.shouldOpenEmailVerification()) {
                Route.SessionRestore
            } else {
                cachedSession.role.toHomeRoute()
            }
            return StartupResolution(
                decision = StartupDecision(route = route, userRole = cachedSession.role, userId = cachedSession.uid),
                authMs = authMs,
                userFetchMs = userFetchMs,
                cacheHit = true
            )
        }

        return StartupResolution(
            decision = StartupDecision(route = Route.SessionRestore, userId = currentUser.uid),
            authMs = authMs,
            userFetchMs = userFetchMs,
            cacheHit = false
        )
    }

    private suspend fun resolveFromNetwork(
        userId: String,
        isAuthEmailVerified: Boolean,
        authReloadTimeoutMs: Long
    ): StartupDecision? {
        return when (val result = getUser(userId)) {
            is Result.Success -> {
                val user = result.data
                val shouldCheckEmailVerification = shouldCheckEmailVerificationFor(user.role)

                val isAuthVerified = if (shouldCheckEmailVerification) {
                    withTimeoutOrNull(authReloadTimeoutMs) {
                        when (val reloadResult = authRepository.reloadCurrentUser()) {
                            is Result.Success -> reloadResult.data.isEmailVerified
                            is Result.Error -> isAuthEmailVerified
                        }
                    } ?: isAuthEmailVerified
                } else {
                    true
                }

                startupSessionCache.cacheStartupSession(
                    uid = userId,
                    role = user.role,
                    isUserVerified = user.verified,
                    isAuthEmailVerified = isAuthVerified
                )

                if (shouldCheckEmailVerification && (!isAuthVerified || !user.verified)) {
                    StartupDecision(route = Route.EmailVerification, userRole = user.role, userId = userId)
                } else {
                    StartupDecision(route = user.role.toHomeRoute(), userRole = user.role, userId = userId)
                }
            }

            is Result.Error -> null
        }
    }

    private fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
