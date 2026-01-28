package com.good4.auth.presentation.register.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.domain.AuthError
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.user.data.dto.UserDto
import com.good4.user.data.repository.UserRepository
import com.good4.core.util.validateStudentEmail
import com.good4.user.domain.UserRole
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.education_level_1
import good4.composeapp.generated.resources.education_level_2
import good4.composeapp.generated.resources.education_level_3
import good4.composeapp.generated.resources.education_level_4
import good4.composeapp.generated.resources.education_level_5
import good4.composeapp.generated.resources.education_level_6
import good4.composeapp.generated.resources.education_level_masters
import good4.composeapp.generated.resources.education_level_phd
import good4.composeapp.generated.resources.error_email_already_in_use
import good4.composeapp.generated.resources.error_email_required
import good4.composeapp.generated.resources.error_full_name_required
import good4.composeapp.generated.resources.error_network_connection_short
import good4.composeapp.generated.resources.error_password_min_length
import good4.composeapp.generated.resources.error_password_required
import good4.composeapp.generated.resources.error_passwords_not_match
import good4.composeapp.generated.resources.error_unknown
import good4.composeapp.generated.resources.error_university_required
import good4.composeapp.generated.resources.error_user_info_save_failed_prefix
import good4.composeapp.generated.resources.error_weak_password
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.getString
class StudentRegisterViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val configRepository: AppConfigRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        StudentRegisterState(
            educationLevels = listOf(
                Res.string.education_level_1,
                Res.string.education_level_2,
                Res.string.education_level_3,
                Res.string.education_level_4,
                Res.string.education_level_5,
                Res.string.education_level_6,
                Res.string.education_level_masters,
                Res.string.education_level_phd
            )
        )
    )
    val state = _state.asStateFlow()

    fun onAction(action: StudentRegisterAction) {
        when (action) {
            is StudentRegisterAction.OnEmailChange -> {
                _state.update { it.copy(email = action.email, errorMessage = null) }
            }
            is StudentRegisterAction.OnPasswordChange -> {
                _state.update { it.copy(password = action.password, errorMessage = null) }
            }
            is StudentRegisterAction.OnConfirmPasswordChange -> {
                _state.update { it.copy(confirmPassword = action.confirmPassword, errorMessage = null) }
            }
            is StudentRegisterAction.OnFullNameChange -> {
                _state.update { it.copy(fullName = action.fullName, errorMessage = null) }
            }
            is StudentRegisterAction.OnUniversityChange -> {
                _state.update { it.copy(university = action.university, errorMessage = null) }
            }
            is StudentRegisterAction.OnMajorChange -> {
                _state.update { it.copy(major = action.major, errorMessage = null) }
            }
            is StudentRegisterAction.OnEducationLevelChange -> {
                _state.update { it.copy(educationLevel = action.educationLevel, errorMessage = null) }
            }
            is StudentRegisterAction.OnTogglePasswordVisibility -> {
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            is StudentRegisterAction.OnRegisterClick -> register()
            is StudentRegisterAction.OnClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }
            is StudentRegisterAction.OnBackClick -> Unit
        }
    }

    private fun register() {
        val state = _state.value
        val email = state.email.trim()

        if (state.fullName.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_full_name_required))
            }
            return
        }
        if (email.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_email_required))
            }
            return
        }
        
        val emailValidation = email.validateStudentEmail()
        if (emailValidation != null) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(emailValidation))
            }
            return
        }
        
        if (state.password.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_password_required))
            }
            return
        }
        if (state.password.length < 6) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_password_min_length))
            }
            return
        }
        if (state.password != state.confirmPassword) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_passwords_not_match))
            }
            return
        }
        if (state.university.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_university_required))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val authResult = authRepository.signUp(email, state.password)) {
                is Result.Success -> {
                    val userId = authResult.data.uid
                    val now = Clock.System.now()
                    val weeklyCredit = configRepository.getStudentWeeklyCredit()

                    val userDto = UserDto(
                        email = email,
                        fullName = state.fullName,
                        phoneNumber = null,
                        role = UserRole.STUDENT.value,
                        verified = false,
                        university = state.university,
                        major = state.major.ifBlank { null },
                        educationLevel = state.educationLevel.ifBlank { null },
                        credit = weeklyCredit,
                        lastCreditResetAt = now,
                        registrationDate = now
                    )

                    when (val userResult = userRepository.createUser(userId, userDto)) {
                        is Result.Success -> {
                            authRepository.sendEmailVerification()
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    isRegisterSuccess = true
                                )
                            }
                        }
                        is Result.Error -> {
                            val prefix = getString(Res.string.error_user_info_save_failed_prefix)
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = UiText.DynamicString(
                                        prefix + (userResult.error.message ?: "")
                                    )
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
}
