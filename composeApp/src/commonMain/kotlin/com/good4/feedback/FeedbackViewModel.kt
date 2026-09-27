package com.good4.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val subject: String = "",
    val message: String = "",
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean
        get() = subject.trim().length in 3..120 && message.trim().length in 10..2000 && !isSubmitting
}

class FeedbackViewModel(private val repository: FeedbackRepository) : ViewModel() {
    private val _state = MutableStateFlow(FeedbackUiState())
    val state = _state.asStateFlow()

    fun startNew() {
        _state.value = FeedbackUiState()
    }

    fun onSubjectChange(value: String) {
        if (value.length <= 120) _state.update { it.copy(subject = value, errorMessage = null) }
    }

    fun onMessageChange(value: String) {
        if (value.length <= 2000) _state.update { it.copy(message = value, errorMessage = null) }
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                repository.submit(current.subject, current.message)
                _state.update { it.copy(isSubmitting = false, isSubmitted = true) }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Geri bildirim gönderilemedi. Lütfen tekrar deneyin."
                    )
                }
            }
        }
    }
}
