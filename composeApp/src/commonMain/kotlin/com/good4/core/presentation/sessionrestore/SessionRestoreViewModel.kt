package com.good4.core.presentation.sessionrestore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.data.local.cacheStartupSession
import com.good4.core.data.local.shouldCheckEmailVerificationFor
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.navigation.Route
import com.good4.navigation.toHomeRoute
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_user_info_fetch_failed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SessionRestoreViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val startupSessionCache: StartupSessionCache
) : ViewModel() {

    companion object {
        private const val USER_FETCH_TIMEOUT_MS = 15_000L
        private const val AUTH_RELOAD_TIMEOUT_MS = 1_500L
    }

    private val _state = MutableStateFlow(SessionRestoreState())
    val state = _state.asStateFlow()

    init {
        restoreSession()
    }

    fun onRetry() {
        restoreSession()
    }

    fun onLogout() {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid
            authRepository.signOut()
            startupSessionCache.clear(uid)
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = null,
                    targetRoute = Route.Login,
                    userRole = null
                )
            }
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    targetRoute = null,
                    userRole = null
                )
            }

            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _state.update {
                    it.copy(isLoading = false, targetRoute = Route.Login)
                }
                return@launch
            }

            val userResult = withTimeoutOrNull(USER_FETCH_TIMEOUT_MS) {
                userRepository.getUser(currentUser.uid)
            } ?: Result.Error(com.good4.core.domain.NetworkError("session-restore-timeout"))

            when (userResult) {
                is Result.Success -> {
                    val user = userResult.data
                    val shouldCheckEmailVerification = shouldCheckEmailVerificationFor(user.role)

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

                    startupSessionCache.cacheStartupSession(
                        uid = currentUser.uid,
                        role = user.role,
                        isUserVerified = user.verified,
                        isAuthEmailVerified = isAuthVerified
                    )

                    val route = if (shouldCheckEmailVerification && (!isAuthVerified || !user.verified)) {
                        Route.EmailVerification
                    } else {
                        user.role.toHomeRoute()
                    }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            targetRoute = route,
                            userRole = user.role
                        )
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.StringResourceId(Res.string.error_user_info_fetch_failed),
                            targetRoute = null,
                            userRole = null
                        )
                    }
                }
            }
        }
    }
}
