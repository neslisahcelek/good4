package com.good4.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.domain.AuthError
import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.data.local.cacheStartupSession
import com.good4.core.data.local.shouldCheckEmailVerificationFor
import com.good4.core.presentation.CooldownTimer
import com.good4.core.presentation.UiText
import com.good4.core.util.normalizeForEmail
import com.good4.core.util.validateEmail
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_email_not_verified
import good4.composeapp.generated.resources.error_email_required
import good4.composeapp.generated.resources.error_network_connection
import good4.composeapp.generated.resources.error_password_required
import good4.composeapp.generated.resources.error_please_register
import good4.composeapp.generated.resources.error_resend_wait_seconds
import good4.composeapp.generated.resources.error_unknown
import good4.composeapp.generated.resources.error_user_not_found
import good4.composeapp.generated.resources.forgot_password_email_sent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val startupSessionCache: StartupSessionCache
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val passwordResetCooldown = CooldownTimer(viewModelScope)

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnGoogleToken -> login(action.token)
            is LoginAction.OnGoogleError -> _state.update { it.copy(errorMessage = UiText.DynamicString(action.message)) }
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
            is LoginAction.OnSupporterRegisterClick -> Unit

            is LoginAction.OnForgotPasswordClick -> sendPasswordResetEmail()
        }
    }

    private fun login(googleIdToken: String? = null) {
        val state = _state.value

        if (state.isLoading) {
            return
        }

        val email = state.email.normalizeForEmail()
        val password = state.password

        if (googleIdToken == null && email.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_email_required))
            }
            return
        }
        if (googleIdToken == null && password.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_password_required))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = if (googleIdToken == null) authRepository.signIn(email, password) else authRepository.signInWithGoogleToken(googleIdToken)) {
                is Result.Success -> {
                    val authUser = result.data
                    val userId = result.data.uid

                    when (val userResult = userRepository.getUser(userId)) {
                        is Result.Success -> {
                            val role = userResult.data.role
                            val shouldCheckEmailVerification = shouldCheckEmailVerificationFor(role)
                            if (shouldCheckEmailVerification && !authUser.isEmailVerified) {
                                startupSessionCache.cacheStartupSession(
                                    uid = userId,
                                    role = role,
                                    isUserVerified = userResult.data.verified,
                                    isAuthEmailVerified = authUser.isEmailVerified
                                )
                                authRepository.sendEmailVerification()
                                _state.update { current ->
                                    current.copy(
                                        isLoading = false,
                                        isEmailVerificationRequired = true,
                                        infoMessage = null,
                                        errorMessage = null
                                    )
                                }
                            } else {
                                startupSessionCache.cacheStartupSession(
                                    uid = userId,
                                    role = role,
                                    isUserVerified = userResult.data.verified,
                                    isAuthEmailVerified = authUser.isEmailVerified
                                )
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
                                    errorMessage = if (googleIdToken != null) UiText.DynamicString("Bu Google hesabıyla eşleşen Good4 kaydı bulunamadı veya kayda erişilemedi. Topluluk yöneticisiyseniz Good4 ekibiyle iletişime geçin.") else userResult.error.toUserFetchErrorUiText()
                                )
                            }
                            startupSessionCache.clear(userId)
                            authRepository.signOut()
                        }
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapAuthLoginErrorToUiText(result.error)
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

        val remainingSeconds = passwordResetCooldown.remainingSeconds()
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

        val email = _state.value.email.normalizeForEmail()
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
        passwordResetCooldown.start(
            seconds = seconds,
            onTick = { remaining ->
                _state.update {
                    it.copy(
                        canSendPasswordReset = false,
                        passwordResetCooldownSeconds = remaining
                    )
                }
            },
            onComplete = {
                _state.update {
                    it.copy(
                        canSendPasswordReset = true,
                        passwordResetCooldownSeconds = 0
                    )
                }
            }
        )
    }

    companion object {
        private const val PASSWORD_RESET_COOLDOWN_SECONDS = 60
    }

    override fun onCleared() {
        passwordResetCooldown.cancel()
        super.onCleared()
    }
}

private fun Error.toUserFetchErrorUiText(): UiText {
    return when (this) {
        is NetworkError -> UiText.StringResourceId(Res.string.error_network_connection)
        else -> UiText.StringResourceId(Res.string.error_please_register)
    }
}
