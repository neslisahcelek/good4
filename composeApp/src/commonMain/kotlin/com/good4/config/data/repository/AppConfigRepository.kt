package com.good4.config.data.repository

import com.good4.config.data.dto.AppConfigDto
import com.good4.config.data.dto.UniversitiesConfigDto
import com.good4.config.domain.AppConfig
import com.good4.config.domain.AppDefaults
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.minutes

class AppConfigRepository(
    private val firestoreRepository: FirestoreRepository
) {
    private val universitiesFallback = normalizeUniversityNames(
        listOf(
            "Akdeniz Üniversitesi",
            "Antalya Bilim Üniversitesi",
            "Alaaddin Keykubat Üniversitesi",
            "Süleyman Demirel Üniversitesi",
            "Pamukkale Üniversitesi",
            "Işık Üniversitesi",
            "Uşak Üniversitesi"
        )
    )

    private val _config = MutableStateFlow(AppConfig.DEFAULT)
    val config: StateFlow<AppConfig> = _config.asStateFlow()
    private val _universities = MutableStateFlow<List<String>>(emptyList())
    val universities: StateFlow<List<String>> = _universities.asStateFlow()

    suspend fun loadConfig() {
        when (val result = firestoreRepository.getDocument(
            collectionPath = "app_config",
            documentId = "global",
            clazz = AppConfigDto::class
        )) {
            is Result.Success -> {
                val dto = result.data
                val expirationMinutes = dto.reservationExpirationMinutes ?: AppDefaults.RESERVATION_EXPIRATION_MINUTES
                val studentWeeklyCredit = dto.studentWeeklyCredit
                    ?: AppConfig.DEFAULT.studentWeeklyCredit

                _config.value = AppConfig(
                    reservationExpirationDuration = expirationMinutes.minutes,
                    studentWeeklyCredit = studentWeeklyCredit
                )
            }
            is Result.Error -> {
                _config.value = AppConfig.DEFAULT
            }
        }
    }

    suspend fun loadUniversities() {
        when (val result = firestoreRepository.getDocument(
            collectionPath = "app_config",
            documentId = "universities",
            clazz = UniversitiesConfigDto::class
        )) {
            is Result.Success -> {
                val remoteUniversities = normalizeUniversityNames(result.data.items.orEmpty())
                _universities.value = if (remoteUniversities.isNotEmpty()) {
                    remoteUniversities
                } else {
                    universitiesFallback
                }
            }
            is Result.Error -> {
                _universities.value = universitiesFallback
            }
        }
    }

    fun getExpirationDuration(): kotlin.time.Duration {
        return _config.value.reservationExpirationDuration
    }

    fun getStudentWeeklyCredit(): Int {
        return _config.value.studentWeeklyCredit
    }

    fun getUniversities(): List<String> {
        return _universities.value
    }

    private fun normalizeUniversityNames(universities: List<String>): List<String> {
        return universities
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
    }
}
