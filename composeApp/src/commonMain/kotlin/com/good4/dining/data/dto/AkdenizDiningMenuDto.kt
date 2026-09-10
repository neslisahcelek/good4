package com.good4.dining.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AkdenizDiningMenuDto(
    @SerialName("weekLabel")
    val weekLabel: String? = null,
    @SerialName("weekStart")
    val weekStart: String? = null,
    @SerialName("weekEnd")
    val weekEnd: String? = null,
    @SerialName("days")
    val days: List<AkdenizDiningMenuDayDto> = emptyList()
)

@Serializable
data class AkdenizDiningMenuDayDto(
    @SerialName("date")
    val date: String? = null,
    @SerialName("dayName")
    val dayName: String? = null,
    @SerialName("meals")
    val meals: List<String> = emptyList(),
    @SerialName("calories")
    val calories: Int? = null
)
