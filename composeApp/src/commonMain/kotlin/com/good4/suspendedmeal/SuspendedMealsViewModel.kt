package com.good4.suspendedmeal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.domain.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class ActiveMealCode(
    val mealId: String,
    val code: String,
    val expiresAtMillis: Long,
    val redeemed: Boolean = false
)

data class SuspendedMealsState(
    val isLoading: Boolean = true,
    val meals: List<SuspendedMeal> = emptyList(),
    val loadError: String? = null,
    val requestingMealId: String? = null,
    val activeCode: ActiveMealCode? = null,
    val message: String? = null
)

class SuspendedMealsViewModel(
    private val repository: SuspendedMealRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SuspendedMealsState())
    val state = _state.asStateFlow()
    private var statusJob: Job? = null

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            when (val result = repository.activeMeals()) {
                is Result.Success -> _state.update { it.copy(isLoading = false, meals = result.data) }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, loadError = "Askıdaki yemekler yüklenemedi. Bağlantını kontrol edip tekrar dene.")
                }
            }
        }
    }

    fun requestCode(mealId: String) {
        if (_state.value.requestingMealId != null) return
        viewModelScope.launch {
            _state.update { it.copy(requestingMealId = mealId, message = null) }
            try {
                when (val result = repository.requestCode(mealId)) {
                    is CodeRequestResult.Code -> {
                        _state.update { it.copy(activeCode = ActiveMealCode(mealId, result.code, result.expiresAtMillis)) }
                        watchCode(result.code)
                    }
                    CodeRequestResult.AlreadyUsed ->
                        _state.update { it.copy(message = "Bu askıdaki yemekten daha önce yararlandın.") }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(message = error.message.toCodeMessage()) }
                if (error.message == "CAMPAIGN_LIMIT_REACHED" || error.message == "CAMPAIGN_NOT_ACTIVE") load()
            } finally {
                _state.update { it.copy(requestingMealId = null) }
            }
        }
    }

    fun dismissCode() {
        statusJob?.cancel()
        _state.update { it.copy(activeCode = null) }
        load()
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    /** Polls the code while it is shown so the student sees when the business has accepted it. */
    private fun watchCode(code: String) {
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                val active = _state.value.activeCode ?: break
                if (Clock.System.now().toEpochMilliseconds() >= active.expiresAtMillis) break
                if (repository.codeStatus(code) == "redeemed") {
                    _state.update { it.copy(activeCode = active.copy(redeemed = true)) }
                    break
                }
            }
        }
    }
}

private fun String?.toCodeMessage(): String = when (this) {
    "EDU_VERIFICATION_REQUIRED" -> "Askıda yemek için üniversite e-postanı doğrulaman gerekiyor."
    "CAMPAIGN_LIMIT_REACHED" -> "Bu askıdaki yemeklerin hepsi alınmış."
    "CAMPAIGN_NOT_ACTIVE", "CAMPAIGN_NOT_PUBLISHED" -> "Bu askıda yemek artık geçerli değil."
    "BUSINESS_NOT_ACTIVE" -> "İşletme şu anda askıda yemek vermiyor."
    else -> "Kod alınamadı. Lütfen tekrar dene."
}
