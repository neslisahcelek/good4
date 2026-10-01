package com.good4.schedule.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.core.domain.Result
import com.good4.schedule.domain.ClassSchedule
import com.good4.schedule.domain.ClassSchedules
import com.good4.user.User
import com.good4.user.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClassScheduleState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val schedule: ClassSchedule? = null,
    val isProfileSelectionComplete: Boolean = false,
    val isAcademicProfileMissing: Boolean = false,
    val errorMessage: String? = null
)

class ClassScheduleViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ClassScheduleState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUser?.uid
        if (userId == null) {
            _state.update { it.copy(isLoading = false, schedule = ClassSchedules.businessFirstYear) }
            return
        }

        viewModelScope.launch {
            // A loaded schedule stays on screen while it refreshes.
            val hasLoadedUser = _state.value.user != null
            _state.update { it.copy(isLoading = !hasLoadedUser, errorMessage = null) }
            when (val result = userRepository.getUser(userId)) {
                is Result.Success -> {
                    val user = result.data
                    val selectedSchedule = ClassSchedules.find(
                        faculty = user.faculty,
                        department = user.major,
                        classYear = user.classYear
                    )
                    val hasCompleteSelection = selectedSchedule != null
                    val isAcademicProfileMissing = listOf(user.faculty, user.major, user.classYear)
                        .any { it.isNullOrBlank() }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            schedule = selectedSchedule ?: ClassSchedules.businessFirstYear,
                            isProfileSelectionComplete = hasCompleteSelection,
                            isAcademicProfileMissing = isAcademicProfileMissing
                        )
                    }
                }

                is Result.Error -> {
                    // Never swap a student's own schedule for the fallback because a refresh failed.
                    if (hasLoadedUser) {
                        _state.update { it.copy(isLoading = false) }
                        return@launch
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            schedule = ClassSchedules.businessFirstYear,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }
}
