package com.good4.dining.domain

data class AkdenizDiningMenu(
    val weekLabel: String,
    val weekStart: String,
    val weekEnd: String,
    val days: List<AkdenizDiningMenuDay>
)

data class AkdenizDiningMenuDay(
    val date: String,
    val dayName: String,
    val meals: List<String>,
    val calories: Int?
)

data class KykMenuDay(
    val date: String,
    val breakfast: List<String>,
    val dinner: List<String>
)

/** The three meals shown on the home widget and the daily menu page, in order of the day. */
enum class DailyMeal { KYK_BREAKFAST, CAFETERIA, KYK_DINNER }
