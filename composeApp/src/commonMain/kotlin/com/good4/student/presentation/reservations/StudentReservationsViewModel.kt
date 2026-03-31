package com.good4.student.presentation.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.code.data.repository.CodeRepository
import com.good4.code.data.repository.statusEnum
import com.good4.code.domain.CodeStatus
import com.good4.config.data.repository.AppConfigRepository
import com.good4.config.domain.AppDefaults
import com.good4.core.domain.Result
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.business_name_fallback
import good4.composeapp.generated.resources.product_name_fallback
import good4.composeapp.generated.resources.reservation_expired_short
import good4.composeapp.generated.resources.time_minute_suffix
import good4.composeapp.generated.resources.time_second_suffix
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.compose.resources.getString
private const val MAX_COMPLETED_RESERVATIONS = 10L

class StudentReservationsViewModel(
    private val authRepository: AuthRepository,
    private val codeRepository: CodeRepository,
    private val userRepository: UserRepository,
    private val configRepository: AppConfigRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StudentReservationsState())
    val state = _state.asStateFlow()
    private var expiredLabel: String = ""
    private var minuteSuffix: String = ""
    private var secondSuffix: String = ""
    private var expirationDuration = AppDefaults.RESERVATION_EXPIRATION_MINUTES.minutes

    init {
        loadReservations()
        viewModelScope.launch {
            expiredLabel = getString(Res.string.reservation_expired_short)
            minuteSuffix = getString(Res.string.time_minute_suffix)
            secondSuffix = getString(Res.string.time_second_suffix)
        }
        viewModelScope.launch {
            expirationDuration = configRepository.getExpirationDuration()
        }
        startTimer()
        viewModelScope.launch {
            while (true) {
                delay(5.minutes)
                checkAndExpireCodes()
            }
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                updateRemainingTimes()
                delay(1.seconds)
            }
        }
    }

    private fun updateRemainingTimes() {
        val currentReservations = _state.value.reservations
        if (currentReservations.isEmpty()) return

        val updated = currentReservations.map { reservation ->
            if (reservation.statusEnum == CodeStatus.PENDING) {
                if (isReservationExpired(reservation.createdAt)) {
                    reservation.copy(
                        status = CodeStatus.EXPIRED.value,
                        remainingTime = expiredLabel
                    )
                } else {
                    val remainingTime = calculateRemainingTime(reservation.createdAt)
                    reservation.copy(remainingTime = remainingTime)
                }
            } else {
                reservation
            }
        }

        _state.update { it.copy(reservations = updated) }
    }

    private fun isReservationExpired(createdAtSecs: Long?): Boolean {
        if (createdAtSecs == null) return false
        return try {
            val createdInstant = Instant.fromEpochSeconds(createdAtSecs)
            val now = Clock.System.now()
            val elapsed = now - createdInstant
            elapsed >= expirationDuration
        } catch (_: Exception) {
            false
        }
    }

    private fun calculateRemainingTime(createdAtSecs: Long?): String {
        if (createdAtSecs == null) return ""

        return try {
            val createdInstant = Instant.fromEpochSeconds(createdAtSecs)
            val now = Clock.System.now()
            val elapsed = now - createdInstant
            val remaining = expirationDuration - elapsed

            if (remaining.isNegative()) {
                expiredLabel
            } else {
                val minutes = remaining.inWholeMinutes
                val seconds = (remaining.inWholeSeconds % 60)
                "${minutes}${minuteSuffix} ${seconds}${secondSuffix}"
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun checkAndExpireCodes() {
        viewModelScope.launch {
            codeRepository.checkAndExpireCodes()
            loadReservations()
        }
    }

    private fun loadReservations() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            expirationDuration = configRepository.getExpirationDuration()

            when (val userResult = userRepository.getUser(userId)) {
                is Result.Success -> {
                    val user = userResult.data
                    _state.update { it.copy(remainingCredit = user.credit) }
                }
                is Result.Error -> {}
            }

            val codesResult = codeRepository.getCodesByUserId(userId)
            if (codesResult is Result.Error) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = codesResult.error.message)
                }
                return@launch
            }

            val allUserCodes = (codesResult as Result.Success).data
            val normalizedCodes = allUserCodes.map { code ->
                if (code.statusEnum == CodeStatus.PENDING && isReservationExpired(code.createdAt)) {
                    when (codeRepository.markCodeAsExpired(code.id)) {
                        is Result.Success -> {
                            userRepository.incrementUserCredit(userId)
                            code.copy(status = CodeStatus.EXPIRED.value)
                        }
                        is Result.Error -> code
                    }
                } else {
                    code
                }
            }

            val pendingCodes = normalizedCodes.filter { it.statusEnum == CodeStatus.PENDING }
            val completedCodes = normalizedCodes
                .filter { it.statusEnum == CodeStatus.USED }
                .sortedByDescending { code -> code.usedAt ?: 0L }
                .take(MAX_COMPLETED_RESERVATIONS.toInt())
            val expiredCodes = normalizedCodes.filter { it.statusEnum == CodeStatus.EXPIRED }
            val cancelledCodes = normalizedCodes.filter { it.statusEnum == CodeStatus.CANCELLED }

            val allCodes = pendingCodes + completedCodes + expiredCodes + cancelledCodes

            val sortedCodes = allCodes.sortedByDescending { code ->
                code.usedAt ?: code.createdAt ?: 0L
            }

            val productFallback = getString(Res.string.product_name_fallback)
            val businessFallback = getString(Res.string.business_name_fallback)
            val reservations = sortedCodes.map { code ->
                val remainingTime = if (code.statusEnum == CodeStatus.PENDING) {
                    calculateRemainingTime(code.createdAt)
                } else {
                    ""
                }

                ReservationUiModel(
                    id = code.id,
                    code = code.value,
                    productName = code.productName ?: productFallback,
                    businessName = code.businessName ?: businessFallback,
                    businessAddress = code.businessAddress ?: "",
                    businessAddressUrl = code.businessAddressUrl ?: "",
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
    }

    fun cancelReservation(reservationId: String) {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            when (val cancelResult = codeRepository.markCodeAsCancelled(reservationId)) {
                is Result.Success -> {
                    userRepository.incrementUserCredit(userId)
                    loadReservations()
                }
                is Result.Error -> {
                    _state.update { it.copy(errorMessage = cancelResult.error.message) }
                }
            }
        }
    }

    fun refresh() {
        loadReservations()
    }
}
