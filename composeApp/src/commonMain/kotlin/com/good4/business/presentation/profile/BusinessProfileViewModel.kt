package com.good4.business.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.core.domain.Result
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_business_not_found
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class BusinessProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val businessRepository: FirestoreBusinessRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BusinessProfileState())
    val state = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun loadProfile() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val userResult = userRepository.getUser(userId)) {
                is Result.Success -> {
                    _state.update { it.copy(ownerName = userResult.data.fullName) }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(errorMessage = userResult.error.message)
                    }
                }
            }

            when (val result = businessRepository.getBusinesses()) {
                is Result.Success -> {
                    val business = result.data.find { it.ownerId == userId }
                    if (business != null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                businessName = business.name,
                                address = business.address,
                                addressUrl = business.addressUrl,
                                phone = business.phone
                            )
                        }
                    } else {
                        val message = getString(Res.string.error_business_not_found)
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = message
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
