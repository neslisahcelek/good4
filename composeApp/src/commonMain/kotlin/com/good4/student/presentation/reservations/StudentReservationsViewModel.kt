package com.good4.student.presentation.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.code.data.repository.statusEnum
import com.good4.code.domain.CodeStatus
import com.good4.auth.data.repository.AuthRepository
import com.good4.code.data.repository.CodeRepository
import com.good4.core.domain.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class StudentReservationsViewModel(
    private val authRepository: AuthRepository,
    private val codeRepository: CodeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StudentReservationsState())
    val state = _state.asStateFlow()

    init {
        loadReservations()
        startTimer()
        // Check for expired codes periodically
        viewModelScope.launch {
            while (true) {
                delay(5.minutes) // Her 5 dakikada bir expired kodları kontrol et
                checkAndExpireCodes()
            }
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                delay(60.seconds) // Her 60 saniyede bir güncelle
                updateRemainingTimes()
            }
        }
    }

    private fun updateRemainingTimes() {
        val currentReservations = _state.value.reservations
        if (currentReservations.isEmpty()) return

        val updated = currentReservations.map { reservation ->
            if (reservation.statusEnum == CodeStatus.PENDING) {
                val remainingTime = calculateRemainingTime(reservation.createdAt)
                reservation.copy(remainingTime = remainingTime)
            } else {
                reservation
            }
        }

        _state.update { it.copy(reservations = updated) }
    }

    private fun calculateRemainingTime(createdAt: String?): String {
        if (createdAt == null) return ""

        return try {
            val createdInstant = Instant.parse(createdAt)
            val now = Clock.System.now()
            val elapsed = now - createdInstant
            val expirationTime = 45.minutes
            val remaining = expirationTime - elapsed

            if (remaining.isNegative()) {
                "Süresi doldu"
            } else {
                val minutes = remaining.inWholeMinutes
                val seconds = (remaining.inWholeSeconds % 60)
                "${minutes}dk ${seconds}sn"
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun checkAndExpireCodes() {
        viewModelScope.launch {
            codeRepository.checkAndExpireCodes()
            // Refresh after checking
            loadReservations()
        }
    }

    private fun loadReservations() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = codeRepository.getCodesByUserId(userId)) {
                is Result.Success -> {
                    val reservations = result.data.map { code ->
                        val remainingTime = if (code.statusEnum == CodeStatus.PENDING) {
                            calculateRemainingTime(code.createdAt)
                        } else {
                            ""
                        }

                        ReservationUiModel(
                            id = code.id,
                            code = code.value,
                            productName = code.productName ?: "Ürün",
                            businessName = code.businessName ?: "İşletme",
                            status = code.status,
                            remainingTime = remainingTime,
                            createdAt = code.createdAt
                        )
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            reservations = reservations
                        )
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

    fun refresh() {
        loadReservations()
    }
}

