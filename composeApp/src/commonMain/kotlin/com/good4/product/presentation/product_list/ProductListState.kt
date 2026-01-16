package com.good4.product.presentation.product_list

import com.good4.core.presentation.UiText
import com.good4.product.Product
import kotlinx.datetime.Instant

data class ProductListState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isReserving: Boolean = false,
    val activeReservation: ReservationInfo? = null,
    val errorMessage: UiText? = null
)

data class ReservationInfo(
    val code: String,
    val product: Product,
    val expiryTime: Instant,
    val codeId: String
)
