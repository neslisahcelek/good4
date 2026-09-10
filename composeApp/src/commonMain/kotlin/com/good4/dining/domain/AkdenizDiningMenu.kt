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
