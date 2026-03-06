package com.good4.product.presentation.product_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.code.data.dto.CodeDto
import com.good4.code.data.repository.CodeRepository
import com.good4.code.domain.CodeStatus
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.product.data.repository.FirestoreProductRepository
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.already_have_reservation
import good4.composeapp.generated.resources.error_no_credit
import good4.composeapp.generated.resources.error_reservation_exception_prefix
import good4.composeapp.generated.resources.error_reservation_failed_prefix
import good4.composeapp.generated.resources.error_user_info_fetch_failed
import good4.composeapp.generated.resources.error_user_not_logged_in
import good4.composeapp.generated.resources.product_out_of_stock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

class ProductListViewModel(
    private val productRepository: FirestoreProductRepository,
    private val codeRepository: CodeRepository,
    private val authRepository: AuthRepository,
    private val configRepository: AppConfigRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProductListState())
    val state = _state.asStateFlow()

    private var isLoaded: Boolean = false

    fun refresh() {
        isLoaded = false
        loadProducts()
        loadActiveReservation()
        loadStudentInfo()
    }

    fun loadStudentInfo() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            val deliveryTimeMinutes =
                configRepository.getExpirationDuration().inWholeMinutes.toInt()

            when (val result = userRepository.getUser(userId)) {
                is Result.Success -> {
                    val user = result.data
                    val now = Clock.System.now()
                    val nextMondayMidnight = nextMondayMidnightInstant()
                    val duration = nextMondayMidnight - now
                    val validDuration =
                        if (duration.isPositive()) duration else kotlin.time.Duration.ZERO

                    _state.update {
                        it.copy(
                            userName = user.fullName.split(" ").firstOrNull() ?: user.fullName,
                            remainingCredits = user.credit,
                            creditRenewalDuration = validDuration,
                            deliveryTimeMinutes = deliveryTimeMinutes
                        )
                    }
                }

                is Result.Error -> {}
            }
        }
    }

    fun loadActiveReservation(): Unit {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            val expirationMinutes = configRepository.getExpirationDuration().inWholeMinutes
            when (val result = codeRepository.getPendingCodeByUserId(userId)) {
                is Result.Success -> {
                    val pendingCode = result.data
                    if (pendingCode == null) {
                        _state.update {
                            it.copy(
                                activeReservation = null,
                                reservationExpirationMinutes = expirationMinutes
                            )
                        }
                        return@launch
                    }
                    val expiryTime = pendingCode.expiresAt
                        ?.let { kotlinx.datetime.Instant.fromEpochSeconds(it) }
                        ?: return@launch

                    val product =
                        _state.value.products.firstOrNull { it.documentId == pendingCode.productId }
                            ?: run {
                                when (val productResult =
                                    productRepository.getProductById(pendingCode.productId ?: "")) {
                                    is Result.Success -> productResult.data
                                    is Result.Error -> return@launch
                                }
                            }

                    val codeId = when (val idResult =
                        codeRepository.getCodeIdByValue(pendingCode.value ?: "")) {
                        is Result.Success -> idResult.data
                        is Result.Error -> return@launch
                    }

                    _state.update {
                        it.copy(
                            activeReservation = ReservationInfo(
                                code = pendingCode.value ?: "",
                                product = product,
                                expiryTime = expiryTime,
                                codeId = codeId
                            ),
                            reservationExpirationMinutes = expirationMinutes
                        )
                    }
                }

                is Result.Error -> {}
            }
        }
    }

    fun onAction(action: ProductListAction) {
        when (action) {
            is ProductListAction.OnSearchQueryChange -> {
                _state.update {
                    it.copy(
                        searchQuery = action.query
                    )
                }
            }

            is ProductListAction.OnReserveProduct -> {
                if (_state.value.activeReservation != null) {
                    _state.update {
                        it.copy(
                            errorMessage = UiText.StringResourceId(Res.string.already_have_reservation)
                        )
                    }
                } else {
                    reserveProduct(action.product)
                }
            }

            is ProductListAction.OnDismissError -> {
                _state.update {
                    it.copy(errorMessage = null)
                }
            }

            is ProductListAction.OnReservationExpired -> {
                viewModelScope.launch {
                    codeRepository.markCodeAsExpired(action.codeId)

                    _state.update {
                        it.copy(activeReservation = null)
                    }
                }
            }
        }
    }

    private fun reserveProduct(product: com.good4.product.Product) {
        val userId = authRepository.currentUser?.uid
        if (userId == null) {
            _state.update {
                it.copy(
                    isReserving = false,
                    errorMessage = UiText.StringResourceId(Res.string.error_user_not_logged_in)
                )
            }
            return
        }

        viewModelScope.launch {
            when (val userResult = userRepository.refreshStudentCreditIfNeeded(userId)) {
                is Result.Success -> {
                    val credit = userResult.data.credit ?: 0
                    if (credit <= 0) {
                        _state.update {
                            it.copy(
                                isReserving = false,
                                errorMessage = UiText.StringResourceId(Res.string.error_no_credit)
                            )
                        }
                        return@launch
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isReserving = false,
                            errorMessage = UiText.StringResourceId(
                                Res.string.error_user_info_fetch_failed
                            )
                        )
                    }
                    return@launch
                }
            }

            if (product.amount <= 0) {
                _state.update {
                    it.copy(
                        isReserving = false,
                        errorMessage = UiText.StringResourceId(Res.string.product_out_of_stock)
                    )
                }
                return@launch
            }

            _state.update { it.copy(isReserving = true, errorMessage = null) }

            try {
                val codeValue = (100000..999999).random().toString()
                val now = Clock.System.now()
                val expirationDuration = configRepository.getExpirationDuration()
                val expiryTime = now + expirationDuration

                val codeDto = CodeDto(
                    value = codeValue,
                    businessId = product.businessId,
                    productId = product.documentId,
                    userId = userId,
                    status = CodeStatus.PENDING.value,
                    createdAt = now.epochSeconds,
                    expiresAt = expiryTime.epochSeconds,
                    usedAt = null
                )

                when (val result = codeRepository.createCode(codeDto)) {
                    is Result.Success -> {
                        userRepository.decrementUserCredit(userId)
                        val expirationMinutes =
                            configRepository.getExpirationDuration().inWholeMinutes
                        _state.update {
                            it.copy(
                                isReserving = false,
                                activeReservation = ReservationInfo(
                                    code = codeValue,
                                    product = product,
                                    expiryTime = expiryTime,
                                    codeId = result.data
                                ),
                                reservationExpirationMinutes = expirationMinutes
                            )
                        }
                    }

                    is Result.Error -> {
                        val prefix = getString(Res.string.error_reservation_failed_prefix)
                        _state.update {
                            it.copy(
                                isReserving = false,
                                errorMessage = UiText.DynamicString(
                                    prefix + (result.error.message ?: "")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                val prefix = getString(Res.string.error_reservation_exception_prefix)
                _state.update {
                    it.copy(
                        isReserving = false,
                        errorMessage = UiText.DynamicString(
                            prefix + (e.message ?: "")
                        )
                    )
                }
            }
        }
    }

    fun loadProductsIfNeeded() {
        if (!isLoaded && !_state.value.isLoading) {
            loadProducts()
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            when (val result = productRepository.getProducts()) {
                is Result.Success -> {
                    isLoaded = true
                    _state.update {
                        it.copy(
                            products = result.data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }

                is Result.Error -> {
                    isLoaded = true
                    _state.update {
                        it.copy(
                            products = emptyList(),
                            isLoading = false,
                            errorMessage = UiText.DynamicString(result.error.message ?: "")
                        )
                    }
                }
            }
        }
    }
}

private fun nextMondayMidnightInstant(): kotlinx.datetime.Instant {
    val now = Clock.System.now()
    val todayUtc = now.toLocalDateTime(TimeZone.UTC)
    val daysSinceMonday = todayUtc.dayOfWeek.ordinal // MONDAY=0, ..., SUNDAY=6
    val lastMonday = todayUtc.date.minus(daysSinceMonday, DateTimeUnit.DAY)
    val lastMondayMidnightSecs = LocalDateTime(
        lastMonday.year, lastMonday.monthNumber, lastMonday.dayOfMonth, 0, 0, 0
    ).toInstant(TimeZone.UTC).epochSeconds
    return kotlinx.datetime.Instant.fromEpochSeconds(lastMondayMidnightSecs + 7 * 86_400L)
}
