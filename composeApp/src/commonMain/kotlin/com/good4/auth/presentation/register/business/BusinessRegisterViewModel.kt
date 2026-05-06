package com.good4.auth.presentation.register.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.presentation.register.mapAuthSignUpErrorToUiText
import com.good4.business.data.dto.BusinessDto
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.data.local.cacheStartupSession
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.core.util.hasValidOptionalPhoneNumber
import com.good4.core.util.normalizeForEmail
import com.good4.core.util.normalizePersonalNameInput
import com.good4.core.util.normalizePhoneNumberInput
import com.good4.core.util.validateEmail
import com.good4.user.data.dto.UserDto
import com.good4.user.data.repository.UserRepository
import com.good4.user.domain.UserRole
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_address_required
import good4.composeapp.generated.resources.error_business_name_required
import good4.composeapp.generated.resources.error_city_required
import good4.composeapp.generated.resources.error_email_required
import good4.composeapp.generated.resources.error_full_name_required
import good4.composeapp.generated.resources.error_password_min_length
import good4.composeapp.generated.resources.error_password_required
import good4.composeapp.generated.resources.error_passwords_not_match
import good4.composeapp.generated.resources.error_phone_invalid_format
import good4.composeapp.generated.resources.error_register_business_save_failed
import good4.composeapp.generated.resources.error_register_profile_save_failed
import good4.composeapp.generated.resources.error_terms_not_accepted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BusinessRegisterViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val startupSessionCache: StartupSessionCache
) : ViewModel() {

    private val _state = MutableStateFlow(BusinessRegisterState())
    val state = _state.asStateFlow()

    fun onAction(action: BusinessRegisterAction) {
        when (action) {
            is BusinessRegisterAction.OnEmailChange -> {
                _state.update { it.copy(email = action.email, errorMessage = null) }
            }

            is BusinessRegisterAction.OnPasswordChange -> {
                _state.update { it.copy(password = action.password, errorMessage = null) }
            }

            is BusinessRegisterAction.OnConfirmPasswordChange -> {
                _state.update {
                    it.copy(
                        confirmPassword = action.confirmPassword,
                        errorMessage = null
                    )
                }
            }

            is BusinessRegisterAction.OnFullNameChange -> {
                _state.update {
                    it.copy(
                        fullName = action.fullName.normalizePersonalNameInput(),
                        errorMessage = null
                    )
                }
            }

            is BusinessRegisterAction.OnPhoneNumberChange -> {
                _state.update {
                    it.copy(
                        phoneNumber = action.phoneNumber.normalizePhoneNumberInput(),
                        errorMessage = null
                    )
                }
            }

            is BusinessRegisterAction.OnBusinessNameChange -> {
                _state.update { it.copy(businessName = action.businessName, errorMessage = null) }
            }

            is BusinessRegisterAction.OnBusinessPhoneChange -> {
                _state.update {
                    it.copy(
                        businessPhone = action.businessPhone.normalizePhoneNumberInput(),
                        errorMessage = null
                    )
                }
            }

            is BusinessRegisterAction.OnAddressChange -> {
                _state.update { it.copy(address = action.address, errorMessage = null) }
            }

            is BusinessRegisterAction.OnAddressUrlChange -> {
                _state.update { it.copy(addressUrl = action.addressUrl, errorMessage = null) }
            }

            is BusinessRegisterAction.OnCityChange -> {
                _state.update { it.copy(city = action.city, errorMessage = null) }
            }

            is BusinessRegisterAction.OnDistrictChange -> {
                _state.update { it.copy(district = action.district, errorMessage = null) }
            }

            is BusinessRegisterAction.OnTogglePasswordVisibility -> {
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            is BusinessRegisterAction.OnToggleTermsAccepted -> {
                _state.update { it.copy(isTermsAccepted = !it.isTermsAccepted, errorMessage = null) }
            }

            is BusinessRegisterAction.OnRegisterClick -> register()
            is BusinessRegisterAction.OnClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }

            is BusinessRegisterAction.OnBackClick -> Unit
        }
    }

    private fun register() {
        val state = _state.value
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

        val emailValidation = email.validateEmail()
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
        if (!state.phoneNumber.hasValidOptionalPhoneNumber() || !state.businessPhone.hasValidOptionalPhoneNumber()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_phone_invalid_format))
            }
            return
        }
        if (state.businessName.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_business_name_required))
            }
            return
        }
        if (state.address.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_address_required))
            }
            return
        }
        if (state.city.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_city_required))
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

                    val businessDto = BusinessDto(
                        name = state.businessName,
                        ownerId = userId,
                        phone = state.businessPhone.ifBlank { state.phoneNumber },
                        address = state.address,
                        addressUrl = state.addressUrl.ifBlank { null },
                        city = state.city,
                        district = state.district.ifBlank { null }
                    )

                    when (businessRepository.addBusiness(businessDto)) {
                        is Result.Success -> {
                            val userDto = UserDto(
                                email = email,
                                fullName = state.fullName,
                                phoneNumber = state.phoneNumber.ifBlank { null },
                                role = UserRole.BUSINESS.value,
                                verified = false
                            )

                            when (userRepository.createUser(userId, userDto)) {
                                is Result.Success -> {
                                    startupSessionCache.cacheStartupSession(
                                        uid = userId,
                                        role = UserRole.BUSINESS,
                                        isUserVerified = false,
                                        isAuthEmailVerified = authResult.data.isEmailVerified
                                    )
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
                                    errorMessage = UiText.StringResourceId(
                                        Res.string.error_register_business_save_failed
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
