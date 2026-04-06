package com.good4.user.presentation.accountsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.domain.AuthError
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.account_settings_business_name_required
import good4.composeapp.generated.resources.account_settings_name_required
import good4.composeapp.generated.resources.account_settings_saved
import good4.composeapp.generated.resources.error_delete_account_failed
import good4.composeapp.generated.resources.error_network_connection
import good4.composeapp.generated.resources.error_recent_login_required
import good4.composeapp.generated.resources.error_unknown
import good4.composeapp.generated.resources.error_user_not_logged_in
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountSettingsViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val configRepository: AppConfigRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AccountSettingsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            configRepository.loadUniversities()
        }
        _state.update { it.copy(universities = configRepository.getUniversities()) }
        viewModelScope.launch {
            configRepository.universities.collect { universities ->
                _state.update { current ->
                    current.copy(universities = universities)
                }
            }
        }
    }

    fun refresh(mode: AccountSettingsMode) {
        val userId = authRepository.currentUser?.uid
        if (userId == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = UiText.StringResourceId(Res.string.error_user_not_logged_in)
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

            when (val userResult = userRepository.getUser(userId)) {
                is Result.Success -> {
                    val user = userResult.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            fullName = user.fullName,
                            phoneNumber = user.phoneNumber.orEmpty(),
                            university = user.university.orEmpty(),
                            major = user.major.orEmpty(),
                            educationLevel = user.educationLevel.orEmpty(),
                            email = user.email,
                            showPhoneField = mode == AccountSettingsMode.SUPPORTER ||
                                    user.phoneNumber?.isNotBlank() == true
                        )
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (userResult.error.message.isBlank()) {
                                UiText.StringResourceId(Res.string.error_unknown)
                            } else {
                                UiText.DynamicString(userResult.error.message)
                            }
                        )
                    }
                    return@launch
                }
            }

            if (mode == AccountSettingsMode.BUSINESS) {
                when (val businessIdResult = businessRepository.getOwnedBusinessId(userId)) {
                    is Result.Success -> {
                        val businessId = businessIdResult.data
                        if (businessId == null) {
                            _state.update {
                                it.copy(
                                    errorMessage = UiText.StringResourceId(Res.string.error_unknown)
                                )
                            }
                            return@launch
                        }

                        when (val businessResult = businessRepository.getBusinessById(businessId)) {
                            is Result.Success -> {
                                _state.update {
                                    it.copy(
                                        businessName = businessResult.data.name,
                                        businessPhone = businessResult.data.phone,
                                        showPhoneField = businessResult.data.phone.isNotBlank()
                                    )
                                }
                            }

                            is Result.Error -> {
                                _state.update {
                                    it.copy(
                                        errorMessage = if (businessResult.error.message.isBlank()) {
                                            UiText.StringResourceId(Res.string.error_unknown)
                                        } else {
                                            UiText.DynamicString(businessResult.error.message)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                errorMessage = if (businessIdResult.error.message.isBlank()) {
                                    UiText.StringResourceId(Res.string.error_unknown)
                                } else {
                                    UiText.DynamicString(businessIdResult.error.message)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    fun onFullNameChange(value: String) {
        _state.update { it.copy(fullName = value) }
    }

    fun onPhoneNumberChange(value: String) {
        _state.update { it.copy(phoneNumber = value) }
    }

    fun onBusinessNameChange(value: String) {
        _state.update { it.copy(businessName = value) }
    }

    fun onBusinessPhoneChange(value: String) {
        _state.update { it.copy(businessPhone = value) }
    }

    fun onUniversityChange(value: String) {
        _state.update { it.copy(university = value) }
    }

    fun onMajorChange(value: String) {
        _state.update { it.copy(major = value) }
    }

    fun onEducationLevelChange(value: String) {
        _state.update { it.copy(educationLevel = value) }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun saveChanges(mode: AccountSettingsMode) {
        val userId = authRepository.currentUser?.uid
        if (userId == null) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_user_not_logged_in))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null, infoMessage = null) }

            val result = when (mode) {
                AccountSettingsMode.BUSINESS -> {
                    val businessName = _state.value.businessName.trim()
                    if (businessName.isBlank()) {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = UiText.StringResourceId(
                                    Res.string.account_settings_business_name_required
                                )
                            )
                        }
                        return@launch
                    }

                    businessRepository.updateOwnedBusinessProfile(
                        ownerId = userId,
                        businessName = businessName,
                        phone = if (_state.value.showPhoneField) _state.value.businessPhone else null
                    )
                }

                else -> {
                    val fullName = _state.value.fullName.trim()
                    if (fullName.isBlank()) {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = UiText.StringResourceId(
                                    Res.string.account_settings_name_required
                                )
                            )
                        }
                        return@launch
                    }

                    userRepository.updateUserProfile(
                        userId = userId,
                        fullName = fullName,
                        phoneNumber = if (_state.value.showPhoneField) {
                            _state.value.phoneNumber
                        } else {
                            null
                        },
                        university = if (mode == AccountSettingsMode.STUDENT) {
                            _state.value.university
                        } else {
                            null
                        },
                        major = if (mode == AccountSettingsMode.STUDENT) {
                            _state.value.major
                        } else {
                            null
                        },
                        educationLevel = if (mode == AccountSettingsMode.STUDENT) {
                            _state.value.educationLevel
                        } else {
                            null
                        }
                    )
                }
            }

            when (result) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            infoMessage = UiText.StringResourceId(Res.string.account_settings_saved)
                        )
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = if (result.error.message.isBlank()) {
                                UiText.StringResourceId(Res.string.error_unknown)
                            } else {
                                UiText.DynamicString(result.error.message)
                            }
                        )
                    }
                }
            }
        }
    }

    fun sendPasswordResetEmail() {
        val email = _state.value.email
        if (email.isBlank()) {
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.error_unknown)) }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSendingPasswordReset = true,
                    errorMessage = null,
                    infoMessage = null,
                    isPasswordResetEmailSent = false
                )
            }

            when (val result = authRepository.sendPasswordResetEmail(email)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isSendingPasswordReset = false,
                            infoMessage = null,
                            isPasswordResetEmailSent = true
                        )
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isSendingPasswordReset = false,
                            isPasswordResetEmailSent = false,
                            errorMessage = result.error.toUiText()
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            when (val result = authRepository.signOut()) {
                is Result.Success -> {
                    _state.update { it.copy(isLoggedOut = true) }
                }

                is Result.Error -> {
                    _state.update { it.copy(errorMessage = result.error.toUiText()) }
                }
            }
        }
    }

    fun showDeleteAccountDialog() {
        _state.update { it.copy(isDeleteDialogVisible = true) }
    }

    fun hideDeleteAccountDialog() {
        _state.update { it.copy(isDeleteDialogVisible = false) }
    }

    fun deleteAccount() {
        if (_state.value.isDeleting) return

        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid
            if (userId == null) {
                _state.update {
                    it.copy(errorMessage = UiText.StringResourceId(Res.string.error_user_not_logged_in))
                }
                return@launch
            }

            _state.update { it.copy(isDeleting = true, errorMessage = null) }

            when (val deleteUserResult = userRepository.deleteUser(userId)) {
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = deleteUserResult.error.toDeleteErrorUiText()
                        )
                    }
                    return@launch
                }

                is Result.Success -> Unit
            }

            when (val deleteAuthResult = authRepository.deleteCurrentUser()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            isDeleteDialogVisible = false,
                            isAccountDeleted = true
                        )
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = deleteAuthResult.error.toUiText()
                        )
                    }
                }
            }
        }
    }

    private fun AuthError.toUiText(): UiText = when (this) {
        is AuthError.NetworkError -> UiText.StringResourceId(Res.string.error_network_connection)
        is AuthError.UserNotLoggedIn -> UiText.StringResourceId(Res.string.error_user_not_logged_in)
        is AuthError.RequiresRecentLogin -> UiText.StringResourceId(Res.string.error_recent_login_required)
        else -> UiText.StringResourceId(Res.string.error_unknown)
    }

    private fun com.good4.core.domain.Error.toDeleteErrorUiText(): UiText {
        return if (message.isBlank()) {
            UiText.StringResourceId(Res.string.error_delete_account_failed)
        } else {
            UiText.DynamicString(message)
        }
    }
}
