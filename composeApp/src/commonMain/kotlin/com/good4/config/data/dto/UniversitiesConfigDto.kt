package com.good4.config.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UniversitiesConfigDto(
    @SerialName("items")
    val items: List<String>? = null
)
