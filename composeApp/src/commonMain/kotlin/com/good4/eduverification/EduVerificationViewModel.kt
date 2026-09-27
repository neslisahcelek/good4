package com.good4.eduverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EduVerificationState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isVerified: Boolean = false,
    val verifiedEmail: String? = null,
    val optedIn: Boolean = false,
    val email: String = "",
    val code: String = "",
    val codeSentTo: String? = null,
    val resendSeconds: Int = 0,
    val isSending: Boolean = false,
    val isConfirming: Boolean = false,
    val errorMessage: String? = null
)

class EduVerificationViewModel(
    private val repository: EduVerificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EduVerificationState())
    val state = _state.asStateFlow()
    private var countdown: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            try {
                val status = repository.status()
                _state.update {
                    it.copy(isLoading = false, isVerified = status.verified, verifiedEmail = status.email)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(isLoading = false, loadError = eduVerificationErrorMessage(error)) }
            }
        }
    }

    fun onOptInChange(optedIn: Boolean) {
        _state.update { it.copy(optedIn = optedIn, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email.take(254), errorMessage = null) }
    }

    fun onCodeChange(code: String) {
        val digits = code.filter(Char::isDigit).take(6)
        _state.update { it.copy(code = digits, errorMessage = null) }
    }

    fun changeEmail() {
        countdown?.cancel()
        _state.update { it.copy(codeSentTo = null, code = "", resendSeconds = 0, errorMessage = null) }
    }

    fun sendCode() {
        val snapshot = _state.value
        if (snapshot.isSending || snapshot.resendSeconds > 0) return
        val email = snapshot.email.trim()
        if (!email.lowercase().endsWith(".edu.tr") && !email.lowercase().endsWith("@edu.tr")) {
            _state.update { it.copy(errorMessage = "Geçerli bir .edu.tr e-posta adresi girin.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, errorMessage = null) }
            try {
                when (val result = repository.requestCode(email)) {
                    is EduCodeRequestResult.AlreadyVerified -> _state.update {
                        it.copy(isSending = false, isVerified = true, verifiedEmail = result.email)
                    }
                    is EduCodeRequestResult.Sent -> {
                        _state.update { it.copy(isSending = false, codeSentTo = result.email, code = "") }
                        startCountdown(result.resendAfterSeconds)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(isSending = false, errorMessage = eduVerificationErrorMessage(error)) }
            }
        }
    }

    fun confirmCode() {
        val snapshot = _state.value
        if (snapshot.isConfirming) return
        if (snapshot.code.length != 6) {
            _state.update { it.copy(errorMessage = "Kod 6 haneli olmalı.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isConfirming = true, errorMessage = null) }
            try {
                val message = when (val result = repository.confirmCode(snapshot.code)) {
                    is EduCodeConfirmResult.Verified -> {
                        countdown?.cancel()
                        _state.update {
                            it.copy(
                                isConfirming = false,
                                isVerified = true,
                                verifiedEmail = result.email.ifBlank { snapshot.codeSentTo }
                            )
                        }
                        return@launch
                    }
                    is EduCodeConfirmResult.InvalidCode ->
                        "Kod hatalı. Kalan deneme hakkı: ${result.attemptsLeft}"
                    EduCodeConfirmResult.Expired -> "Kodun süresi doldu. Yeni kod isteyin."
                    EduCodeConfirmResult.TooManyAttempts -> "Çok fazla hatalı deneme yapıldı. Yeni kod isteyin."
                }
                _state.update { it.copy(isConfirming = false, errorMessage = message) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(isConfirming = false, errorMessage = eduVerificationErrorMessage(error)) }
            }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (remaining in seconds downTo 0) {
                _state.update { it.copy(resendSeconds = remaining) }
                if (remaining > 0) delay(1_000)
            }
        }
    }
}
