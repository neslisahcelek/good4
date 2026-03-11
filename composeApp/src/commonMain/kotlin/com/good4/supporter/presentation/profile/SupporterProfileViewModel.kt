package com.good4.supporter.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.domain.AuthError
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_delete_account_failed
import good4.composeapp.generated.resources.error_network_connection
import good4.composeapp.generated.resources.error_recent_login_required
import good4.composeapp.generated.resources.error_unknown
import good4.composeapp.generated.resources.error_user_not_logged_in
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SupporterProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupporterProfileState())
    val state = _state.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = userRepository.getUser(userId)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, user = result.data) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun showDeleteAccountDialog() {
        _state.update { it.copy(isDeleteDialogVisible = true) }
    }

    fun hideDeleteAccountDialog() {
        _state.update { it.copy(isDeleteDialogVisible = false) }
    }

    fun clearDeleteError() {
        _state.update { it.copy(deleteErrorMessage = null) }
    }

    fun deleteAccount() {
        if (_state.value.isDeleting) return

        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid
            if (userId == null) {
                _state.update {
                    it.copy(deleteErrorMessage = UiText.StringResourceId(Res.string.error_user_not_logged_in))
                }
                return@launch
            }

            _state.update { it.copy(isDeleting = true, deleteErrorMessage = null) }

            when (val deleteUserResult = userRepository.deleteUser(userId)) {
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            deleteErrorMessage = deleteUserResult.error.toDeleteErrorUiText()
                        )
                    }
                    return@launch
                }
                is Result.Success -> Unit
            }

            when (val deleteAuthResult = authRepository.deleteCurrentUser()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(isDeleting = false, isDeleteDialogVisible = false, isAccountDeleted = true)
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            deleteErrorMessage = deleteAuthResult.error.toAuthDeleteErrorUiText()
                        )
                    }
                }
            }
        }
    }

    private fun AuthError.toAuthDeleteErrorUiText(): UiText = when (this) {
        is AuthError.NetworkError -> UiText.StringResourceId(Res.string.error_network_connection)
        is AuthError.UserNotLoggedIn -> UiText.StringResourceId(Res.string.error_user_not_logged_in)
        is AuthError.RequiresRecentLogin -> UiText.StringResourceId(Res.string.error_recent_login_required)
        else -> UiText.StringResourceId(Res.string.error_unknown)
    }

    private fun com.good4.core.domain.Error.toDeleteErrorUiText(): UiText =
        if (message.isBlank()) UiText.StringResourceId(Res.string.error_delete_account_failed)
        else UiText.DynamicString(message)
}
