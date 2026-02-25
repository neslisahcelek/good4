package com.good4.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.domain.AuthError
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.core.util.AppEnvironment
import com.good4.core.util.validateEmail
import com.good4.user.data.repository.UserRepository
import com.good4.user.domain.UserRole
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_email_required
import good4.composeapp.generated.resources.error_email_not_verified
import good4.composeapp.generated.resources.error_invalid_credentials
import good4.composeapp.generated.resources.error_network_connection
import good4.composeapp.generated.resources.error_password_required
import good4.composeapp.generated.resources.error_please_register
import good4.composeapp.generated.resources.error_resend_wait_seconds
import good4.composeapp.generated.resources.error_unknown
import good4.composeapp.generated.resources.error_user_not_found
import good4.composeapp.generated.resources.forgot_password_email_sent
import good4.composeapp.generated.resources.verification_email_sent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private var passwordResetCooldownUntilMillis: Long = 0L
    private var passwordResetCooldownJob: Job? = null

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnEmailChange -> {
                _state.update {
                    it.copy(
                        email = action.email,
                        errorMessage = null,
                        infoMessage = null,
                        isEmailVerificationRequired = false
                    )
                }
            }
            is LoginAction.OnPasswordChange -> {
                _state.update {
                    it.copy(
                        password = action.password,
                        errorMessage = null,
                        infoMessage = null,
                        isEmailVerificationRequired = false
                    )
                }
            }
            is LoginAction.OnTogglePasswordVisibility -> {
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            is LoginAction.OnLoginClick -> login()
            is LoginAction.OnClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }
            is LoginAction.OnClearInfo -> {
                _state.update { it.copy(infoMessage = null) }
            }
            is LoginAction.OnStudentRegisterClick,
            is LoginAction.OnBusinessRegisterClick,
            is LoginAction.OnForgotPasswordClick -> sendPasswordResetEmail()
        }
    }

    private fun login() {
        val state = _state.value
        
        if (state.isLoading) {
            return
        }
        
        val email = state.email.trim()
        val password = state.password

        if (email.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_email_required))
            }
            return
        }
        if (password.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_password_required))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.signIn(email, password)) {
                is Result.Success -> {
                    val authUser = result.data
                    val userId = result.data.uid

                    when (val userResult = userRepository.getUser(userId)) {
                        is Result.Success -> {
                            val role = userResult.data.role
                            val shouldCheckEmailVerification =
                                AppEnvironment.isEmailVerificationRequired && role == UserRole.STUDENT
                            if (shouldCheckEmailVerification && !authUser.isEmailVerified) {
                                val sendResult = authRepository.sendEmailVerification()
                                _state.update { current ->
                                    current.copy(
                                        isLoading = false,
                                        isEmailVerificationRequired = true,
                                        infoMessage = if (sendResult is Result.Success) {
                                            UiText.StringResourceId(Res.string.verification_email_sent)
                                        } else {
                                            null
                                        },
                                        errorMessage = UiText.StringResourceId(Res.string.error_email_not_verified)
                                    )
                                }
                            } else {
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        isLoginSuccess = true,
                                        userRole = role
                                    )
                                }
                            }
                        }
                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = UiText.StringResourceId(
                                        Res.string.error_please_register
                                    )
                                )
                            }
                            authRepository.signOut()
                        }
                    }
                }
                is Result.Error -> {
                    val authError = result.error
                    val errorMessage = when (authError) {
                        is AuthError.NetworkError ->
                            UiText.StringResourceId(Res.string.error_network_connection)
                        is AuthError.InvalidCredentials ->
                            UiText.StringResourceId(Res.string.error_invalid_credentials)
                        is AuthError.UserNotFound ->
                            UiText.StringResourceId(Res.string.error_user_not_found)
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

    private fun sendPasswordResetEmail() {
        val state = _state.value
        
        if (state.isLoading) {
            return
        }
        
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        if (nowMillis < passwordResetCooldownUntilMillis) {
            val remainingSeconds = ((passwordResetCooldownUntilMillis - nowMillis) / 1000L)
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

        val email = _state.value.email.trim()
        val emailValidation = email.validateEmail()
        if (emailValidation != null) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(emailValidation))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

            when (val result = authRepository.sendPasswordResetEmail(email)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = UiText.StringResourceId(Res.string.forgot_password_email_sent)
                        )
                    }
                    startPasswordResetCooldown(PASSWORD_RESET_COOLDOWN_SECONDS)
                }

                is Result.Error -> {
                    val errorMessage = when (result.error) {
                        is AuthError.NetworkError ->
                            UiText.StringResourceId(Res.string.error_network_connection)

                        is AuthError.UserNotFound ->
                            UiText.StringResourceId(Res.string.error_user_not_found)

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

    private fun startPasswordResetCooldown(seconds: Int) {
        passwordResetCooldownUntilMillis = Clock.System.now().toEpochMilliseconds() + seconds * 1000L
        passwordResetCooldownJob?.cancel()
        passwordResetCooldownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _state.update {
                    it.copy(
                        canSendPasswordReset = false,
                        passwordResetCooldownSeconds = remaining
                    )
                }
                delay(1.seconds)
            }
            _state.update {
                it.copy(
                    canSendPasswordReset = true,
                    passwordResetCooldownSeconds = 0
                )
            }
        }
    }

    companion object {
        private const val PASSWORD_RESET_COOLDOWN_SECONDS = 60
    }
}
