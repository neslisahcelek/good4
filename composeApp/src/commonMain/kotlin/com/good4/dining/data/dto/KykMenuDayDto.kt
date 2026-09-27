package com.good4.dining.data.dto

import kotlinx.serialization.Serializable

/** One day of the monthly KYK dorm menu, written by the saveKykMenu function (kyk_menu_days/{date}). */
@Serializable
data class KykMenuDayDto(
    val date: String? = null,
    val breakfast: List<String> = emptyList(),
    val dinner: List<String> = emptyList()
)
