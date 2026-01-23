package com.good4.config.domain

import com.good4.code.data.dto.DEFAULT_EXPIRATION_MINUTES
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class AppConfig(
    val reservationExpirationDuration: Duration,
    val creditResetIntervalDays: Int,
    val studentWeeklyCredit: Int
) {
    companion object {
        val DEFAULT = AppConfig(
            reservationExpirationDuration = DEFAULT_EXPIRATION_MINUTES.minutes,
            creditResetIntervalDays = 7,
            studentWeeklyCredit = 1
        )
    }
}
