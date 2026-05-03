package com.good4.student.presentation.home

import com.good4.code.domain.CodeStatus
import com.good4.product.presentation.product_list.ProductListState
import com.good4.student.presentation.reservations.ReservationUiModel
import kotlinx.datetime.Clock

internal fun ProductListState.toPendingReservationUiModel(): ReservationUiModel {
    val activeReservation = requireNotNull(activeReservation)
    val createdAt = reservationExpirationMinutes?.let { expirationMinutes ->
        activeReservation.expiryTime.epochSeconds - (expirationMinutes * 60)
    } ?: Clock.System.now().epochSeconds

    return ReservationUiModel(
        id = activeReservation.codeId,
        code = activeReservation.code,
        productName = activeReservation.product.name,
        businessName = activeReservation.product.storeName,
        businessAddress = activeReservation.product.address,
        businessAddressUrl = activeReservation.product.addressUrl,
        status = CodeStatus.PENDING.value,
        remainingTime = "",
        createdAt = createdAt
    )
}
