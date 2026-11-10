package com.good4.campusevents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.campus_events_load_error
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class CampusEventsViewModel(
    private val repository: CampusEventsDataSource,
    private val nowEpochSeconds: () -> Long = { Clock.System.now().epochSeconds }
) : ViewModel() {
    private val _state = MutableStateFlow(CampusEventsUi(loading = true))
    val state: StateFlow<CampusEventsUi> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = repository.upcoming(nowEpochSeconds())) {
                is Result.Success -> _state.update { current ->
                    // A filter whose events all ended falls back to "Tümü" instead of an empty screen.
                    val category = current.selectedCategory?.takeIf { id -> result.data.any { it.categoryId == id } }
                    current.copy(loading = false, events = result.data, selectedCategory = category)
                }
                is Result.Error -> _state.update {
                    it.copy(loading = false, error = UiText.StringResourceId(Res.string.campus_events_load_error))
                }
            }
        }
    }

    fun selectCategory(categoryId: String?) = _state.update { it.copy(selectedCategory = categoryId) }

    fun openEvent(eventId: String) = _state.update { it.copy(openEventId = eventId) }

    fun closeEvent() = _state.update { it.copy(openEventId = null) }
}
