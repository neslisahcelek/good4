package com.good4.auth.presentation.verify_email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.domain.AuthError
import com.good4.core.domain.Result
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

class EmailVerificationViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EmailVerificationState())
    val state = _state.asStateFlow()

    private var verificationCooldownUntilMillis: Long = 0L
    private var verificationCooldownJob: Job? = null

    init {
        viewModelScope.launch {
            checkVerification(showErrorIfNotVerified = false, showLoading = false)
        }
    }

    fun onAction(action: EmailVerificationAction) {
        when (action) {
            EmailVerificationAction.OnResendClick -> resendVerificationEmail()
            EmailVerificationAction.OnCheckClick -> {
                viewModelScope.launch {
                    checkVerification(showErrorIfNotVerified = true, showLoading = true)
                }
            }
            EmailVerificationAction.OnClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }
            EmailVerificationAction.OnClearInfo -> {
                _state.update { it.copy(infoMessage = null) }
            }
        }
    }

    private fun resendVerificationEmail() {
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        if (nowMillis < verificationCooldownUntilMillis) {
            val remainingSeconds = ((verificationCooldownUntilMillis - nowMillis) / 1000L)
                .coerceAtLeast(1L)
                .toInt()
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
            _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

            when (val result = authRepository.sendEmailVerification()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
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
                            isLoading = false,
                            errorMessage = errorMessage
                        )
                    }
                }
            }
        }
    }

    private suspend fun checkVerification(
        showErrorIfNotVerified: Boolean,
        showLoading: Boolean
    ) {
        if (showLoading) {
            _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        } else {
            _state.update { it.copy(errorMessage = null, infoMessage = null) }
        }

        when (val result = authRepository.reloadCurrentUser()) {
            is Result.Success -> {
                val authUser = result.data
                if (!authUser.isEmailVerified) {
                    _state.update {
                        it.copy(
                            isLoading = false,
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
                                isLoading = false,
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
                                    isLoading = false,
                                    errorMessage = UiText.StringResourceId(
                                        Res.string.error_email_not_verified
                                    )
                                )
                            }
                            return
                        }
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isVerified = true,
                                userRole = userResult.data.role
                            )
                        }
                    }
                    is Result.Error -> {
                        println("EmailVerification getUser error: ${userResult.error.message}")
                        _state.update {
                            it.copy(
                                isLoading = false,
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
                        isLoading = false,
                        errorMessage = errorMessage
                    )
                }
            }
        }
    }

    private fun startVerificationCooldown(seconds: Int) {
        verificationCooldownUntilMillis = Clock.System.now().toEpochMilliseconds() + seconds * 1000L
        verificationCooldownJob?.cancel()
        verificationCooldownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _state.update {
                    it.copy(
                        canResendEmail = false,
                        resendCooldownSeconds = remaining
                    )
                }
                delay(1.seconds)
            }
            _state.update {
                it.copy(
                    canResendEmail = true,
                    resendCooldownSeconds = 0
                )
            }
        }
    }

    companion object {
        private const val VERIFICATION_COOLDOWN_SECONDS = 60
    }
}
