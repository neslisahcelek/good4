package com.good4.auth.presentation.verify_email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.domain.AuthError
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.data.local.cacheStartupSession
import com.good4.core.data.local.shouldCheckEmailVerificationFor
import com.good4.core.domain.Result
import com.good4.core.presentation.CooldownTimer
import com.good4.core.presentation.UiText
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_email_not_verified
import good4.composeapp.generated.resources.error_network_connection
import good4.composeapp.generated.resources.error_please_register
import good4.composeapp.generated.resources.error_resend_wait_seconds
import good4.composeapp.generated.resources.error_unknown
import good4.composeapp.generated.resources.error_user_not_logged_in
import good4.composeapp.generated.resources.verification_email_sent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class EmailVerificationViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val startupSessionCache: StartupSessionCache
) : ViewModel() {

    private val _state = MutableStateFlow(EmailVerificationState())
    val state = _state.asStateFlow()

    private val verificationCooldown = CooldownTimer(viewModelScope)

    init {
        viewModelScope.launch {
            resolveVerificationAccess(showErrorIfNotVerified = false, showLoading = false)
        }
    }

    fun onAction(action: EmailVerificationAction) {
        when (action) {
            EmailVerificationAction.OnResendClick -> resendVerificationEmail()
            EmailVerificationAction.OnCheckClick -> {
                viewModelScope.launch {
                    resolveVerificationAccess(showErrorIfNotVerified = true, showLoading = true)
                }
            }
            EmailVerificationAction.OnClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }
            EmailVerificationAction.OnClearInfo -> {
                _state.update { it.copy(infoMessage = null) }
            }
            EmailVerificationAction.OnLogoutClick -> {
                viewModelScope.launch {
                    logout()
                }
            }
        }
    }

    private suspend fun logout() {
        _state.update { it.copy(isLoggingOut = true) }
        authRepository.signOut()
        _state.update { it.copy(isLoggingOut = false) }
    }

    private fun resendVerificationEmail() {
        val state = _state.value

        if (state.isCheckingVerification || state.isResendingEmail || state.isLoggingOut) {
            return
        }

        val remainingSeconds = verificationCooldown.remainingSeconds()
        if (remainingSeconds > 0) {
            _state.update {
                it.copy(
                    errorMessage = UiText.StringResourceId(
                        Res.string.error_resend_wait_seconds,
                        arrayOf(remainingSeconds)
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isResendingEmail = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }

            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _state.update {
                    it.copy(
                        isResendingEmail = false,
                        errorMessage = UiText.StringResourceId(Res.string.error_user_not_logged_in)
                    )
                }
                return@launch
            }

            when (val userResult = userRepository.getUser(currentUser.uid)) {
                is Result.Success -> {
                    if (!shouldCheckEmailVerificationFor(userResult.data.role)) {
                        startupSessionCache.cacheStartupSession(
                            uid = currentUser.uid,
                            role = userResult.data.role,
                            isUserVerified = userResult.data.verified,
                            isAuthEmailVerified = true
                        )
                        _state.update {
                            it.copy(
                                isResendingEmail = false,
                                isVerified = true,
                                userRole = userResult.data.role
                            )
                        }
                        return@launch
                    }
                }

                is Result.Error -> Unit
            }

            when (val result = authRepository.sendEmailVerification()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isResendingEmail = false,
                            infoMessage = UiText.StringResourceId(Res.string.verification_email_sent)
                        )
                    }
                    startVerificationCooldown(VERIFICATION_COOLDOWN_SECONDS)
                }
                is Result.Error -> {
                    val errorMessage = when (result.error) {
                        is AuthError.NetworkError ->
                            UiText.StringResourceId(Res.string.error_network_connection)
                        is AuthError.UserNotLoggedIn ->
                            UiText.StringResourceId(Res.string.error_user_not_logged_in)
                        else -> UiText.StringResourceId(Res.string.error_unknown)
                    }
                    _state.update {
                        it.copy(
                            isResendingEmail = false,
                            errorMessage = errorMessage
                        )
                    }
                }
            }
        }
    }

    private suspend fun resolveVerificationAccess(
        showErrorIfNotVerified: Boolean,
        showLoading: Boolean
    ) {
        if (showLoading) {
            _state.update {
                it.copy(
                    isCheckingVerification = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }
        } else {
            _state.update { it.copy(errorMessage = null, infoMessage = null) }
        }

        val currentUser = authRepository.currentUser
        if (currentUser == null) {
            _state.update {
                it.copy(
                    isCheckingVerification = false,
                    errorMessage = UiText.StringResourceId(Res.string.error_user_not_logged_in)
                )
            }
            return
        }

        when (val userResult = userRepository.getUser(currentUser.uid)) {
            is Result.Success -> {
                val user = userResult.data
                if (!shouldCheckEmailVerificationFor(user.role)) {
                    startupSessionCache.cacheStartupSession(
                        uid = currentUser.uid,
                        role = user.role,
                        isUserVerified = user.verified,
                        isAuthEmailVerified = true
                    )
                    _state.update {
                        it.copy(
                            isCheckingVerification = false,
                            isVerified = true,
                            userRole = user.role
                        )
                    }
                    return
                }

                checkVerification(
                    showErrorIfNotVerified = showErrorIfNotVerified,
                    showLoading = false
                )
            }

            is Result.Error -> {
                _state.update {
                    it.copy(
                        isCheckingVerification = false,
                        errorMessage = UiText.StringResourceId(Res.string.error_please_register)
                    )
                }
            }
        }
    }

    private suspend fun checkVerification(
        showErrorIfNotVerified: Boolean,
        showLoading: Boolean,
        retryCount: Int = 0
    ) {
        if (showLoading) {
            _state.update {
                it.copy(
                    isCheckingVerification = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }
        } else {
            _state.update { it.copy(errorMessage = null, infoMessage = null) }
        }

        when (val result = authRepository.reloadCurrentUser()) {
            is Result.Success -> {
                val authUser = result.data
                if (!authUser.isEmailVerified) {
                    if (showErrorIfNotVerified && retryCount < MAX_VERIFICATION_RETRIES) {
                        delay(VERIFICATION_RETRY_DELAY_SECONDS.seconds)
                        checkVerification(showErrorIfNotVerified, showLoading, retryCount + 1)
                        return
                    }
                    _state.update {
                        it.copy(
                            isCheckingVerification = false,
                            errorMessage = if (showErrorIfNotVerified) {
                                UiText.StringResourceId(Res.string.error_email_not_verified)
                            } else {
                                null
                            }
                        )
                    }
                    return
                }

                when (val markResult = userRepository.markUserVerified(authUser.uid)) {
                    is Result.Success -> Unit
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isCheckingVerification = false,
                                errorMessage = UiText.StringResourceId(
                                    Res.string.error_email_not_verified
                                )
                            )
                        }
                        return
                    }
                }

                when (val userResult = userRepository.getUser(authUser.uid)) {
                    is Result.Success -> {
                        if (!userResult.data.verified) {
                            _state.update {
                                it.copy(
                                    isCheckingVerification = false,
                                    errorMessage = UiText.StringResourceId(
                                        Res.string.error_email_not_verified
                                    )
                                )
                            }
                            return
                        }
                            _state.update {
                                it.copy(
                                    isCheckingVerification = false,
                                    isVerified = true,
                                    userRole = userResult.data.role
                                )
                        }
                    }
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isCheckingVerification = false,
                                errorMessage = UiText.StringResourceId(
                                    Res.string.error_please_register
                                )
                            )
                        }
                    }
                }
            }
            is Result.Error -> {
                val errorMessage = when (result.error) {
                    is AuthError.NetworkError ->
                        UiText.StringResourceId(Res.string.error_network_connection)
                    is AuthError.UserNotLoggedIn ->
                        UiText.StringResourceId(Res.string.error_user_not_logged_in)
                    else -> UiText.StringResourceId(Res.string.error_unknown)
                }
                _state.update {
                    it.copy(
                        isCheckingVerification = false,
                        errorMessage = errorMessage
                    )
                }
            }
        }
    }

    private fun startVerificationCooldown(seconds: Int) {
        verificationCooldown.start(
            seconds = seconds,
            onTick = { remaining ->
                _state.update {
                    it.copy(
                        canResendEmail = false,
                        resendCooldownSeconds = remaining
                    )
                }
            },
            onComplete = {
                _state.update {
                    it.copy(
                        canResendEmail = true,
                        resendCooldownSeconds = 0
                    )
                }
            }
        )
    }

    companion object {
        private const val VERIFICATION_COOLDOWN_SECONDS = 60
        private const val MAX_VERIFICATION_RETRIES = 2
        private const val VERIFICATION_RETRY_DELAY_SECONDS = 2
    }

    override fun onCleared() {
        verificationCooldown.cancel()
        super.onCleared()
    }
}
