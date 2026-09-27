package com.good4.config.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppConfigDto(
    @SerialName("reservationExpirationMinutes")
    val reservationExpirationMinutes: Long? = null,
    @SerialName("studentWeeklyCredit")
    val studentWeeklyCredit: Int? = null
)
