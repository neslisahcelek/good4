package com.good4.auth.presentation.register.supporter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.domain.AuthError
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.core.util.validateEmail
import com.good4.user.data.dto.UserDto
import com.good4.user.data.repository.UserRepository
import com.good4.user.domain.UserRole
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_email_already_in_use
import good4.composeapp.generated.resources.error_email_required
import good4.composeapp.generated.resources.error_full_name_required
import good4.composeapp.generated.resources.error_network_connection_short
import good4.composeapp.generated.resources.error_password_min_length
import good4.composeapp.generated.resources.error_password_required
import good4.composeapp.generated.resources.error_passwords_not_match
import good4.composeapp.generated.resources.error_terms_not_accepted
import good4.composeapp.generated.resources.error_unknown
import good4.composeapp.generated.resources.error_user_info_save_failed_prefix
import good4.composeapp.generated.resources.error_weak_password
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.getString

class SupporterRegisterViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupporterRegisterState())
    val state = _state.asStateFlow()

    fun onAction(action: SupporterRegisterAction) {
        when (action) {
            is SupporterRegisterAction.OnFullNameChange ->
                _state.update { it.copy(fullName = action.fullName, errorMessage = null) }
            is SupporterRegisterAction.OnEmailChange ->
                _state.update { it.copy(email = action.email, errorMessage = null) }
            is SupporterRegisterAction.OnPasswordChange ->
                _state.update { it.copy(password = action.password, errorMessage = null) }
            is SupporterRegisterAction.OnConfirmPasswordChange ->
                _state.update { it.copy(confirmPassword = action.confirmPassword, errorMessage = null) }
            is SupporterRegisterAction.OnTogglePasswordVisibility ->
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is SupporterRegisterAction.OnToggleTermsAccepted ->
                _state.update { it.copy(isTermsAccepted = !it.isTermsAccepted, errorMessage = null) }
            is SupporterRegisterAction.OnRegisterClick -> register()
            is SupporterRegisterAction.OnClearError ->
                _state.update { it.copy(errorMessage = null) }
            is SupporterRegisterAction.OnBackClick -> Unit
        }
    }

    private fun register() {
        if (_state.value.isLoading) return

        val state = _state.value

        if (state.fullName.isBlank()) {
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.error_full_name_required)) }
            return
        }

        val trimmedEmail = state.email.trim()
        if (trimmedEmail.isBlank()) {
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.error_email_required)) }
            return
        }

        val emailValidation = trimmedEmail.validateEmail()
        if (emailValidation != null) {
            _state.update { it.copy(errorMessage = UiText.StringResourceId(emailValidation)) }
            return
        }

        if (state.password.isBlank()) {
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.error_password_required)) }
            return
        }
        if (state.password.length < 6) {
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.error_password_min_length)) }
            return
        }
        if (state.password != state.confirmPassword) {
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.error_passwords_not_match)) }
            return
        }
        if (!state.isTermsAccepted) {
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.error_terms_not_accepted)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val authResult = authRepository.signUp(trimmedEmail, state.password)) {
                is Result.Success -> {
                    val userId = authResult.data.uid
                    val nowSecs = Clock.System.now().epochSeconds

                    val userDto = UserDto(
                        email = trimmedEmail,
                        fullName = state.fullName,
                        role = UserRole.SUPPORTER.value,
                        verified = false,
                        registrationDate = nowSecs,
                        createdAt = nowSecs,
                        totalDonations = 0,
                        totalMeals = 0
                    )

                    when (val userResult = userRepository.createUser(userId, userDto)) {
                        is Result.Success -> {
                            authRepository.sendEmailVerification()
                            _state.update { it.copy(isLoading = false, isRegisterSuccess = true) }
                        }
                        is Result.Error -> {
                            val prefix = getString(Res.string.error_user_info_save_failed_prefix)
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = UiText.DynamicString(prefix + (userResult.error.message ?: ""))
                                )
                            }
                        }
                    }
                }
                is Result.Error -> {
                    val errorMessage = when (authResult.error) {
                        is AuthError.EmailAlreadyInUse ->
                            UiText.StringResourceId(Res.string.error_email_already_in_use)
                        is AuthError.WeakPassword ->
                            UiText.StringResourceId(Res.string.error_weak_password)
                        is AuthError.NetworkError ->
                            UiText.StringResourceId(Res.string.error_network_connection_short)
                        else -> UiText.StringResourceId(Res.string.error_unknown)
                    }
                    _state.update { it.copy(isLoading = false, errorMessage = errorMessage) }
                }
            }
        }
    }
}
