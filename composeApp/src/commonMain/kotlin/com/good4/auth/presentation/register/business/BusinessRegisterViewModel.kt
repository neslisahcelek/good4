package com.good4.auth.presentation.register.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.domain.AuthError
import com.good4.business.data.dto.BusinessDto
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.core.util.validateEmail
import com.good4.user.data.dto.UserDto
import com.good4.user.data.repository.UserRepository
import com.good4.user.domain.UserRole
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_address_required
import good4.composeapp.generated.resources.error_business_name_required
import good4.composeapp.generated.resources.error_business_save_failed_prefix
import good4.composeapp.generated.resources.error_city_required
import good4.composeapp.generated.resources.error_email_already_in_use
import good4.composeapp.generated.resources.error_email_required
import good4.composeapp.generated.resources.error_full_name_required
import good4.composeapp.generated.resources.error_network_connection_short
import good4.composeapp.generated.resources.error_password_min_length
import good4.composeapp.generated.resources.error_password_required
import good4.composeapp.generated.resources.error_passwords_not_match
import good4.composeapp.generated.resources.error_user_info_save_failed_prefix
import good4.composeapp.generated.resources.error_weak_password
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class BusinessRegisterViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val businessRepository: FirestoreBusinessRepository
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
                _state.update { it.copy(fullName = action.fullName, errorMessage = null) }
            }

            is BusinessRegisterAction.OnPhoneNumberChange -> {
                _state.update { it.copy(phoneNumber = action.phoneNumber, errorMessage = null) }
            }

            is BusinessRegisterAction.OnBusinessNameChange -> {
                _state.update { it.copy(businessName = action.businessName, errorMessage = null) }
            }

            is BusinessRegisterAction.OnBusinessPhoneChange -> {
                _state.update { it.copy(businessPhone = action.businessPhone, errorMessage = null) }
            }

            is BusinessRegisterAction.OnAddressChange -> {
                _state.update { it.copy(address = action.address, errorMessage = null) }
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

            is BusinessRegisterAction.OnRegisterClick -> register()
            is BusinessRegisterAction.OnClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }

            is BusinessRegisterAction.OnBackClick -> Unit
        }
    }

    private fun register() {
        val state = _state.value

        if (state.fullName.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_full_name_required))
            }
            return
        }
        if (state.email.isBlank()) {
            _state.update {
                it.copy(errorMessage = UiText.StringResourceId(Res.string.error_email_required))
            }
            return
        }

        val emailValidation = state.email.validateEmail()
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

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val authResult = authRepository.signUp(state.email, state.password)) {
                is Result.Success -> {
                    val userId = authResult.data.uid

                    val businessDto = BusinessDto(
                        name = state.businessName,
                        ownerId = userId,
                        phone = state.businessPhone.ifBlank { state.phoneNumber },
                        address = state.address,
                        city = state.city,
                        district = state.district.ifBlank { null }
                    )

                    when (val businessResult = businessRepository.addBusiness(businessDto)) {
                        is Result.Success -> {
                            val userDto = UserDto(
                                email = state.email,
                                fullName = state.fullName,
                                phoneNumber = state.phoneNumber.ifBlank { null },
                                role = UserRole.BUSINESS.value,
                                verified = false
                            )

                            when (val userResult = userRepository.createUser(userId, userDto)) {
                                is Result.Success -> {
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
                            val prefix = getString(Res.string.error_business_save_failed_prefix)
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = UiText.DynamicString(
                                        prefix + (businessResult.error.message ?: "")
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

                        else -> UiText.DynamicString(authResult.error.message)
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
