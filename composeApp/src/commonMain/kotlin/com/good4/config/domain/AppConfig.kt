package com.good4.config.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class AppConfig(
    val reservationExpirationDuration: Duration,
    val creditResetIntervalDays: Int,
    val studentWeeklyCredit: Int
) {
    companion object {
        val DEFAULT = AppConfig(
            reservationExpirationDuration = AppDefaults.RESERVATION_EXPIRATION_MINUTES.minutes,
            creditResetIntervalDays = AppDefaults.CREDIT_RESET_INTERVAL_DAYS,
            studentWeeklyCredit = AppDefaults.STUDENT_WEEKLY_CREDIT
        )
    }
}
