package com.good4.quiz.presentation.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.quiz.data.QuizRepository
import com.good4.quiz.data.mapQuizErrorToUiText
import com.good4.quiz.domain.QuizReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizReportUiState(
    val report: QuizReport? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null
)

class QuizReportViewModel(
    private val sessionId: String,
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizReportUiState())
    val uiState: StateFlow<QuizReportUiState> = _uiState.asStateFlow()

    init {
        loadReport()
    }

    fun loadReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getQuizReport(sessionId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, report = result.data) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = mapQuizErrorToUiText(result.error.message)
                        )
                    }
                }
            }
        }
    }
}
