package com.good4.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class AcademicCalendarEventDto(
    val title: String? = null,
    val description: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val faculty: String? = null,
    val category: String? = null,
    val academicYear: String? = null,
    val sourcePage: Int? = null,
    val active: Boolean? = null
)

data class AcademicCalendarEvent(
    val id: String,
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val faculty: String,
    val category: String,
    val academicYear: String
)

class AcademicCalendarRepository(private val firestore: FirestoreRepository) {
    suspend fun load(): Result<List<AcademicCalendarEvent>, com.good4.core.domain.Error> =
        when (val result = firestore.queryCollectionWithIds("academic_calendar_events", "active", true, AcademicCalendarEventDto::class)) {
            is Result.Success -> Result.Success(result.data.mapNotNull { document ->
                val item = document.data
                val start = item.startDate.orEmpty()
                val end = item.endDate.orEmpty()
                val title = item.title.orEmpty()
                if (item.active == false || start.isBlank() || end.isBlank() || title.isBlank()) null else AcademicCalendarEvent(
                    id = document.id,
                    title = title,
                    description = item.description.orEmpty(),
                    startDate = start,
                    endDate = end,
                    faculty = item.faculty.orEmpty().ifBlank { "Genel" },
                    category = item.category.orEmpty().ifBlank { "Akademik" },
                    academicYear = item.academicYear.orEmpty().ifBlank { "2026-2027" }
                )
            }.sortedBy { it.startDate })
            is Result.Error -> Result.Error(result.error)
        }
}

data class AcademicCalendarState(
    val events: List<AcademicCalendarEvent> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false
)

class AcademicCalendarViewModel(private val repository: AcademicCalendarRepository) : ViewModel() {
    private val _state = MutableStateFlow(AcademicCalendarState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, failed = false)
            _state.value = when (val result = repository.load()) {
                is Result.Success -> AcademicCalendarState(events = result.data, loading = false)
                is Result.Error -> AcademicCalendarState(loading = false, failed = true)
            }
        }
    }
}
