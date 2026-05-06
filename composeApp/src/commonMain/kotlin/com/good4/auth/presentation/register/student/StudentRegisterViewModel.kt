package com.good4.auth.presentation.register.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.presentation.register.mapAuthSignUpErrorToUiText
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.data.local.cacheStartupSession
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.core.util.normalizeForEmail
import com.good4.core.util.normalizePersonalNameInput
import com.good4.core.util.validateStudentEmail
import com.good4.user.data.dto.UserDto
import com.good4.user.data.repository.UserRepository
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
import good4.composeapp.generated.resources.error_email_required
import good4.composeapp.generated.resources.error_full_name_required
import good4.composeapp.generated.resources.error_password_min_length
import good4.composeapp.generated.resources.error_password_required
import good4.composeapp.generated.resources.error_passwords_not_match
import good4.composeapp.generated.resources.error_register_profile_save_failed
import good4.composeapp.generated.resources.error_terms_not_accepted
import good4.composeapp.generated.resources.error_university_required
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class StudentRegisterViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val configRepository: AppConfigRepository,
    private val startupSessionCache: StartupSessionCache
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
            ),
            universities = emptyList()
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            configRepository.loadUniversities()
        }
        _state.update { it.copy(universities = configRepository.getUniversities()) }
        viewModelScope.launch {
            configRepository.universities.collect { universities ->
                _state.update { it.copy(universities = universities) }
            }
        }
    }

    fun onAction(action: StudentRegisterAction) {
        when (action) {
            is StudentRegisterAction.OnEmailChange -> {
                _state.update { it.copy(email = action.email, errorMessage = null) }
            }

            is StudentRegisterAction.OnPasswordChange -> {
                _state.update { it.copy(password = action.password, errorMessage = null) }
            }

            is StudentRegisterAction.OnConfirmPasswordChange -> {
                _state.update {
                    it.copy(
                        confirmPassword = action.confirmPassword,
                        errorMessage = null
                    )
                }
            }

            is StudentRegisterAction.OnFullNameChange -> {
                _state.update {
                    it.copy(
                        fullName = action.fullName.normalizePersonalNameInput(),
                        errorMessage = null
                    )
                }
            }

            is StudentRegisterAction.OnUniversityChange -> {
                _state.update { it.copy(university = action.university, errorMessage = null) }
            }

            is StudentRegisterAction.OnMajorChange -> {
                _state.update { it.copy(major = action.major, errorMessage = null) }
            }

            is StudentRegisterAction.OnEducationLevelChange -> {
                _state.update {
                    it.copy(
                        educationLevel = action.educationLevel,
                        errorMessage = null
                    )
                }
            }

            is StudentRegisterAction.OnTogglePasswordVisibility -> {
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            is StudentRegisterAction.OnToggleTermsAccepted -> {
                _state.update {
                    it.copy(
                        isTermsAccepted = !it.isTermsAccepted,
                        errorMessage = null
                    )
                }
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

        if (state.isLoading) {
            return
        }

        val email = state.email.normalizeForEmail()

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
        if (state.universities.isNotEmpty() && state.university.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_university_required))
            }
            return
        }
        if (!state.isTermsAccepted) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_terms_not_accepted))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val authResult = authRepository.signUp(email, state.password)) {
                is Result.Success -> {
                    val userId = authResult.data.uid
                    val nowSecs = Clock.System.now().epochSeconds
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
                        lastCreditResetAt = nowSecs,
                        registrationDate = nowSecs
                    )

                    when (userRepository.createUser(userId, userDto)) {
                        is Result.Success -> {
                            startupSessionCache.cacheStartupSession(
                                uid = userId,
                                role = UserRole.STUDENT,
                                isUserVerified = false,
                                isAuthEmailVerified = authResult.data.isEmailVerified
                            )
                            when (authRepository.sendEmailVerification()) {
                                is Result.Success -> Unit
                                is Result.Error -> Unit
                            }
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    isRegisterSuccess = true
                                )
                            }
                        }

                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = UiText.StringResourceId(
                                        Res.string.error_register_profile_save_failed
                                    )
                                )
                            }
                        }
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapAuthSignUpErrorToUiText(authResult.error)
                        )
                    }
                }
            }
        }
    }
}
