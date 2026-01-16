package com.good4.student.presentation.reservations

data class StudentReservationsState(
    val isLoading: Boolean = false,
    val reservations: List<ReservationUiModel> = emptyList(),
    val errorMessage: String? = null
)

