package com.good4.config.data.repository

import com.good4.config.data.dto.AppConfigDto
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
    private val _config = MutableStateFlow(AppConfig.DEFAULT)
    val config: StateFlow<AppConfig> = _config.asStateFlow()

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

    fun getExpirationDuration(): kotlin.time.Duration {
        return _config.value.reservationExpirationDuration
    }

    fun getStudentWeeklyCredit(): Int {
        return _config.value.studentWeeklyCredit
    }
}
