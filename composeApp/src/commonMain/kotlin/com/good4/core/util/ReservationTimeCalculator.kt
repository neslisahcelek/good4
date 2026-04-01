package com.good4.core.util

import kotlinx.datetime.Clock
import kotlin.time.Duration

object ReservationTimeCalculator {

    fun isExpiredFromCreatedAt(
        createdAtEpochSeconds: Long?,
        expirationDuration: Duration,
        nowEpochSeconds: Long = Clock.System.now().epochSeconds
    ): Boolean {
        val createdAt = createdAtEpochSeconds ?: return false
        val expiresAt = createdAt + expirationDuration.inWholeSeconds
        return nowEpochSeconds >= expiresAt
    }

    fun formatRemainingTimeFromCreatedAt(
        createdAtEpochSeconds: Long?,
        expirationDuration: Duration,
        minuteSuffix: String,
        secondSuffix: String,
        expiredLabel: String,
        nowEpochSeconds: Long = Clock.System.now().epochSeconds
    ): String {
        val createdAt = createdAtEpochSeconds ?: return ""
        val expiresAt = createdAt + expirationDuration.inWholeSeconds
        return formatRemainingTimeFromExpiry(
            expiresAtEpochSeconds = expiresAt,
            minuteSuffix = minuteSuffix,
            secondSuffix = secondSuffix,
            expiredLabel = expiredLabel,
            nowEpochSeconds = nowEpochSeconds
        ) ?: ""
    }

    fun formatRemainingTimeFromExpiry(
        expiresAtEpochSeconds: Long?,
        minuteSuffix: String,
        secondSuffix: String,
        expiredLabel: String,
        nowEpochSeconds: Long = Clock.System.now().epochSeconds
    ): String? {
        val remainingSeconds = remainingSecondsUntilExpiry(
            expiresAtEpochSeconds = expiresAtEpochSeconds,
            nowEpochSeconds = nowEpochSeconds
        ) ?: return null

        if (remainingSeconds <= 0) return expiredLabel

        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        return "${minutes}${minuteSuffix} ${seconds}${secondSuffix}"
    }

    fun remainingSecondsUntilExpiry(
        expiresAtEpochSeconds: Long?,
        nowEpochSeconds: Long = Clock.System.now().epochSeconds
    ): Long? {
        val expiry = expiresAtEpochSeconds ?: return null
        return expiry - nowEpochSeconds
    }
}
