package com.good4.product.presentation.product_list

import com.good4.core.presentation.UiText
import com.good4.product.Product
import kotlinx.datetime.Instant
import kotlin.time.Duration

data class ProductListState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isReserving: Boolean = false,
    val reservingProductId: String? = null,
    val activeReservation: ReservationInfo? = null,
    val errorMessage: UiText? = null,
    val reservationExpirationMinutes: Long? = null,
    val userName: String? = null,
    val remainingCredits: Int? = null,
    val creditRenewalDuration: Duration? = null,
    val deliveryTimeMinutes: Int? = null
)

data class ReservationInfo(
    val code: String,
    val product: Product,
    val expiryTime: Instant,
    val codeId: String
)
