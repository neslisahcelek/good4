package com.good4.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.domain.Result
import com.good4.core.util.AppEnvironment
import com.good4.navigation.Route
import com.good4.user.data.repository.UserRepository
import com.good4.user.domain.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val configRepository: AppConfigRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Route?>(null)
    val startDestination = _startDestination.asStateFlow()

    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole = _userRole.asStateFlow()

    init {
        checkAppState()
    }

    private fun checkAppState() {
        viewModelScope.launch {
            configRepository.loadConfig()


            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                when (val result = userRepository.refreshStudentCreditIfNeeded(currentUser.uid)) {
                    is Result.Success -> {
                        _userRole.value = result.data.role
                        val shouldCheckEmailVerification =
                            AppEnvironment.isEmailVerificationRequired &&
                                result.data.role == UserRole.STUDENT

                        val authUserForVerification = if (shouldCheckEmailVerification) {
                            when (val reloadResult = authRepository.reloadCurrentUser()) {
                                is Result.Success -> reloadResult.data
                                is Result.Error -> currentUser
                            }
                        } else {
                            currentUser
                        }

                        val isVerifiedByFirestore = result.data.verified
                        
                        if (
                            shouldCheckEmailVerification &&
                            (authUserForVerification.isEmailVerified.not() || isVerifiedByFirestore.not())
                        ) {
                            _startDestination.value = Route.EmailVerification
                        } else {
                            when (result.data.role) {
                                UserRole.ADMIN -> _startDestination.value = Route.AdminHome
                                UserRole.BUSINESS -> _startDestination.value = Route.BusinessHome
                                UserRole.STUDENT -> _startDestination.value = Route.StudentHome
                            }
                        }
                    }

                    is Result.Error -> {
                        _startDestination.value = Route.Login
                    }
                }
            } else {
                _startDestination.value = Route.Login
            }
        }
    }
}
