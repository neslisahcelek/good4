package com.good4.student.presentation.reservations

import com.good4.code.domain.CodeStatus

val ReservationUiModel.statusEnum: CodeStatus
    get() = CodeStatus.fromValue(status)
