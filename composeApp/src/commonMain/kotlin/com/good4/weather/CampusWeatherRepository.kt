package com.good4.weather

import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import kotlinx.serialization.Serializable

/** Written every 30 minutes by the refreshCampusWeather function (source: MET Norway). */
@Serializable
data class CampusWeatherDto(
    val temperature: Double? = null,
    val label: String? = null,
    val source: String? = null,
    val updatedAtMillis: Long? = null
)

class CampusWeatherRepository(private val store: FirestoreRepository) {
    suspend fun current(): CampusWeatherDto? =
        (store.getDocument("app_config", "campus_weather", CampusWeatherDto::class) as? Result.Success)?.data
}
