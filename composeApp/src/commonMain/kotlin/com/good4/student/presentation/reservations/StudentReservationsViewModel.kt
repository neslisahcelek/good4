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
import com.good4.core.util.ReservationTimeCalculator
import com.good4.product.data.repository.FirestoreProductRepository
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
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val MAX_RESERVATIONS = 10L

class StudentReservationsViewModel(
    private val authRepository: AuthRepository,
    private val codeRepository: CodeRepository,
    private val userRepository: UserRepository,
    private val productRepository: FirestoreProductRepository,
    private val configRepository: AppConfigRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StudentReservationsState())
    val state = _state.asStateFlow()
    private var expiredLabel: String = ""
    private var minuteSuffix: String = ""
    private var secondSuffix: String = ""
    private var expirationDuration = AppDefaults.RESERVATION_EXPIRATION_MINUTES.minutes
    private var hasLoadedOnce: Boolean = false
    private val optimisticCancelledReservationIds = mutableSetOf<String>()

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
                delay(30.seconds)
                loadReservations(showLoading = false)
            }
        }
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
        return ReservationTimeCalculator.isExpiredFromCreatedAt(
            createdAtEpochSeconds = createdAtSecs,
            expirationDuration = expirationDuration
        )
    }

    private fun calculateRemainingTime(createdAtSecs: Long?): String {
        return ReservationTimeCalculator.formatRemainingTimeFromCreatedAt(
            createdAtEpochSeconds = createdAtSecs,
            expirationDuration = expirationDuration,
            minuteSuffix = minuteSuffix,
            secondSuffix = secondSuffix,
            expiredLabel = expiredLabel
        )
    }

    private fun checkAndExpireCodes() {
        loadReservations(showLoading = false)
    }

    private fun loadReservations(showLoading: Boolean = true) {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                if (showLoading) {
                    _state.update { it.copy(isLoading = true) }
                }

                expirationDuration = configRepository.getExpirationDuration()

                when (val userResult = userRepository.getUser(userId)) {
                    is Result.Success -> {
                        val user = userResult.data
                        _state.update { it.copy(remainingCredit = user.credit) }
                    }
                    is Result.Error -> {}
                }

                val recentCodesResult = codeRepository.getRecentCodesByUserId(
                    userId = userId,
                    limit = MAX_RESERVATIONS
                )
                val allUserCodes = when (recentCodesResult) {
                    is Result.Success -> {
                        if (recentCodesResult.data.isNotEmpty()) {
                            recentCodesResult.data
                        } else {
                            loadFallbackReservations(userId) ?: emptyList()
                        }
                    }

                    is Result.Error -> {
                        loadFallbackReservations(userId) ?: run {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = recentCodesResult.error.message
                                )
                            }
                            return@launch
                        }
                    }
                }

                val normalizedCodes = allUserCodes.map { code ->
                    if (code.statusEnum == CodeStatus.PENDING && isReservationExpired(code.createdAt)) {
                        when (codeRepository.markCodeAsExpired(code.id)) {
                            is Result.Success -> {
                                userRepository.incrementUserCredit(userId)
                                productRepository.incrementProductPendingCount(code.productId, 1)
                                code.copy(status = CodeStatus.EXPIRED.value)
                            }
                            is Result.Error -> code
                        }
                    } else {
                        code
                    }
                }

                val sortedCodes = normalizedCodes.sortedByDescending { code ->
                    code.usedAt ?: code.createdAt ?: 0L
                }

                val productFallback = getString(Res.string.product_name_fallback)
                val businessFallback = getString(Res.string.business_name_fallback)
                val reservationsFromBackend = sortedCodes.map { code ->
                    val status = if (code.id in optimisticCancelledReservationIds) {
                        CodeStatus.CANCELLED.value
                    } else {
                        code.status
                    }
                    val remainingTime = if (code.statusEnum == CodeStatus.PENDING) {
                        calculateRemainingTime(code.createdAt)
                    } else {
                        ""
                    }

                    ReservationUiModel(
                        id = code.id,
                        code = code.value,
                        productId = code.productId,
                        productName = code.productName ?: productFallback,
                        businessName = code.businessName ?: businessFallback,
                        businessAddress = code.businessAddress ?: "",
                        businessAddressUrl = code.businessAddressUrl ?: "",
                        status = status,
                        remainingTime = if (status == CodeStatus.PENDING.value) remainingTime else "",
                        createdAt = code.createdAt
                    )
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        reservations = reservationsFromBackend
                    )
                }
            } finally {
                hasLoadedOnce = true
            }
        }
    }

    private suspend fun loadFallbackReservations(
        userId: String
    ): List<com.good4.code.data.repository.CodeWithDetails>? {
        return when (val fallbackResult = codeRepository.getCodesByUserId(userId)) {
            is Result.Success -> fallbackResult.data
                .sortedByDescending { code -> code.usedAt ?: code.createdAt ?: 0L }
                .take(MAX_RESERVATIONS.toInt())

            is Result.Error -> null
        }
    }

    fun cancelReservation(reservationId: String) {
        val userId = authRepository.currentUser?.uid ?: return
        val reservation = _state.value.reservations.firstOrNull { it.id == reservationId }
        optimisticCancelledReservationIds += reservationId
        _state.update { state ->
            state.copy(
                reservations = state.reservations.map { reservation ->
                    if (reservation.id == reservationId) {
                        reservation.copy(
                            status = CodeStatus.CANCELLED.value,
                            remainingTime = ""
                        )
                    } else {
                        reservation
                    }
                },
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val cancelResult = codeRepository.markCodeAsCancelled(reservationId)) {
                is Result.Success -> {
                    userRepository.incrementUserCredit(userId)
                    reservation?.productId?.takeIf { it.isNotBlank() }?.let { productId ->
                        productRepository.incrementProductPendingCount(productId, 1)
                    }
                    optimisticCancelledReservationIds -= reservationId
                    loadReservations()
                }
                is Result.Error -> {
                    optimisticCancelledReservationIds -= reservationId
                    _state.update { it.copy(errorMessage = cancelResult.error.message) }
                    loadReservations(showLoading = false)
                }
            }
        }
    }

    fun refresh(showLoading: Boolean = !hasLoadedOnce) {
        loadReservations(showLoading = showLoading)
    }
}
