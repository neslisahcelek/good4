package com.good4.config.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class AppConfig(
    val reservationExpirationDuration: Duration,
    val studentWeeklyCredit: Int
) {
    companion object {
        val DEFAULT = AppConfig(
            reservationExpirationDuration = AppDefaults.RESERVATION_EXPIRATION_MINUTES.minutes,
            studentWeeklyCredit = AppDefaults.STUDENT_WEEKLY_CREDIT
        )
    }
}
