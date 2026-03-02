package com.good4.admin.presentation.editstudentcredit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.domain.Result
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.admin_users_override_required
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.admin_users_identifier_required
import good4.composeapp.generated.resources.admin_users_override_success
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class EditStudentCreditViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _state = MutableStateFlow(EditStudentCreditState())
    val state = _state.asStateFlow()

    fun onUserIdInputChange(value: String) {
        _state.update { it.copy(userIdInput = value) }
    }

    fun onEmailInputChange(value: String) {
        _state.update { it.copy(emailInput = value) }
    }

    fun onWeeklyCreditInputChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _state.update { it.copy(weeklyCreditInput = value) }
        }
    }

    fun applyOverride() {
        val currentState = _state.value
        val userId = currentState.userIdInput.trim().takeIf { it.isNotEmpty() }
        val email = currentState.emailInput.trim().takeIf { it.isNotEmpty() }
        val parsedValue = currentState.weeklyCreditInput.toIntOrNull()

        if (userId == null && email == null) {
            viewModelScope.launch {
                _state.update { it.copy(errorMessage = getString(Res.string.admin_users_identifier_required)) }
            }
            return
        }

        if (parsedValue == null) {
            viewModelScope.launch {
                _state.update { it.copy(errorMessage = getString(Res.string.admin_users_override_required)) }
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, errorMessage = null) }
            when (
                val result = userRepository.updateStudentWeeklyCreditOverrideByIdentifier(
                    userId = userId,
                    email = email,
                    weeklyCreditOverride = parsedValue
                )
            ) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            userIdInput = "",
                            emailInput = "",
                            weeklyCreditInput = "",
                            successMessage = getString(Res.string.admin_users_override_success)
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
