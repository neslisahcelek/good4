package com.good4.config.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.good4.config.data.dto.AppConfigDto
import com.good4.config.data.dto.HomeBannerDto
import com.good4.config.data.dto.UniversitiesConfigDto
import com.good4.config.domain.AppConfig
import com.good4.config.domain.AppDefaults
import com.good4.config.domain.HomeBanner
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend
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

    // V2 rules expose only the dining menu, home banner and weather documents; the legacy
    // global and universities documents would fail with permission-denied, so use defaults.
    private val legacyConfigAvailable get() = AppEnvironment.firebaseBackend != FirebaseBackend.V2

    suspend fun loadConfig() {
        if (!legacyConfigAvailable) {
            _config.value = AppConfig.DEFAULT
            return
        }
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
        if (!legacyConfigAvailable) {
            _universities.value = universitiesFallback
            return
        }
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

    /** Active slider banners in slot order (home_banner, home_banner_2 … home_banner_4). */
    suspend fun getActiveHomeBanners(today: String): List<HomeBanner> = coroutineScope {
        HOME_BANNER_DOCUMENTS.map { documentId -> async { getActiveHomeBanner(documentId, today) } }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun getActiveHomeBanner(documentId: String, today: String): HomeBanner? {
        return when (val result = firestoreRepository.getDocument(
            collectionPath = "app_config",
            documentId = documentId,
            clazz = HomeBannerDto::class
        )) {
            is Result.Success -> {
                val banner = result.data
                val imageUrl = banner.imageUrl.orEmpty()
                val startsOn = banner.startsOn.orEmpty()
                val endsOn = banner.endsOn.orEmpty()
                if (banner.active != true || imageUrl.isBlank() || startsOn.isBlank() || endsOn.isBlank()
                    || today < startsOn || today > endsOn
                ) null else HomeBanner(
                    imageUrl = imageUrl,
                    advertiserName = banner.advertiserName.orEmpty(),
                    targetUrl = banner.targetUrl.orEmpty(),
                    startsOn = startsOn,
                    endsOn = endsOn
                )
            }
            is Result.Error -> null
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

private val HOME_BANNER_DOCUMENTS = listOf("home_banner", "home_banner_2", "home_banner_3", "home_banner_4")
