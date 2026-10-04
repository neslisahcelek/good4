package com.good4.dining.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.domain.Result
import com.good4.dining.data.repository.AkdenizDiningMenuRepository
import com.good4.dining.data.repository.KykMenuRepository
import com.good4.dining.data.repository.MealRatingRepository
import com.good4.dining.domain.AkdenizDiningMenu
import com.good4.dining.domain.AkdenizDiningMenuDay
import com.good4.dining.domain.DailyMeal
import com.good4.dining.domain.MealVote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AkdenizDiningMenuViewModel(
    private val repository: AkdenizDiningMenuRepository,
    private val kykRepository: KykMenuRepository,
    private val ratingRepository: MealRatingRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AkdenizDiningMenuState())
    val state = _state.asStateFlow()

    fun loadMenu() {
        val today = todayInIstanbul()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val kykDay = async { kykRepository.getDayOrNext(today) }
            val menu = when (val result = repository.getCurrentMenu()) {
                is Result.Success -> result.data.takeIf { it.days.isNotEmpty() && it.isCurrentWeek() }
                is Result.Error -> null
            }
            _state.value = AkdenizDiningMenuState(
                menu = menu ?: currentWeekFallback(),
                kykDay = kykDay.await(),
                loadedDate = today,
                isLoading = false
            )
            loadRatings()
        }
    }

    /** Ratings are read only for meals that were published today (the KYK fallback day is not today). */
    private suspend fun loadRatings() {
        val state = _state.value
        val published = buildList {
            if (state.kykDay?.date == state.loadedDate) {
                if (state.kykDay.breakfast.isNotEmpty()) add(DailyMeal.KYK_BREAKFAST)
                if (state.kykDay.dinner.isNotEmpty()) add(DailyMeal.KYK_DINNER)
            }
            if (state.cafeteriaToday != null) add(DailyMeal.CAFETERIA)
        }
        published.forEach { meal ->
            val rating = runCatching { ratingRepository.load(state.loadedDate, meal) }.getOrNull() ?: return@forEach
            _state.update { it.copy(ratings = it.ratings + (meal to rating)) }
        }
    }

    fun rate(meal: DailyMeal, vote: MealVote) {
        val current = _state.value
        if (meal in current.ratingInFlight || current.ratings[meal]?.myVote == vote) return
        val before = current.ratings[meal]
        // Show the chosen face at once; the server's counters replace it, or the old vote returns on failure.
        _state.update {
            it.copy(
                ratings = it.ratings + (meal to (before ?: com.good4.dining.domain.MealRating()).copy(myVote = vote)),
                ratingInFlight = it.ratingInFlight + meal,
                ratingError = null
            )
        }
        viewModelScope.launch {
            val result = runCatching { ratingRepository.rate(meal, vote) }
            _state.update { state ->
                result.fold(
                    onSuccess = { state.copy(ratings = state.ratings + (meal to it), ratingInFlight = state.ratingInFlight - meal) },
                    onFailure = { error ->
                        state.copy(
                            ratings = if (before == null) state.ratings - meal else state.ratings + (meal to before),
                            ratingInFlight = state.ratingInFlight - meal,
                            ratingError = ratingErrorMessage(error.message)
                        )
                    }
                )
            }
        }
    }

    fun dismissRatingError() = _state.update { it.copy(ratingError = null) }

    private fun ratingErrorMessage(code: String?): String = when {
        code == null -> "Oyun kaydedilemedi. Tekrar dene."
        "MEAL_NOT_STARTED" in code -> "Bu öğün henüz başlamadı."
        "EDU_VERIFICATION_REQUIRED" in code -> "Puan vermek için üniversite e-postanı doğrulamalısın."
        "STUDENT_REQUIRED" in code -> "Yalnızca öğrenciler puan verebilir."
        "RATE_LIMIT" in code || "resource-exhausted" in code -> "Çok hızlı denedin, biraz sonra tekrar dene."
        else -> "Oyun kaydedilemedi. Tekrar dene."
    }

    /** Reloads when the calendar day has changed since the last load, so the page always shows today. */
    fun refreshIfDayChanged() {
        if (!_state.value.isLoading && _state.value.loadedDate != todayInIstanbul()) loadMenu()
    }

    private fun AkdenizDiningMenu.isCurrentWeek(): Boolean {
        val today = todayInIstanbul()
        return weekStart.isNotBlank() && weekEnd.isNotBlank() && today in weekStart..weekEnd
    }

    private fun currentWeekFallback(): AkdenizDiningMenu? {
        val menu = AkdenizDiningMenu(
            weekLabel = "7–11 Eylül 2026",
            weekStart = "2026-09-07",
            weekEnd = "2026-09-11",
            days = listOf(
                AkdenizDiningMenuDay(
                    date = "2026-09-07",
                    dayName = "Pazartesi",
                    meals = listOf(
                        "Tarhana Çorbası",
                        "Fırın Tavuk",
                        "Çiftlik Pilavı",
                        "Yoğurt"
                    ),
                    calories = 1105
                ),
                AkdenizDiningMenuDay(
                    date = "2026-09-08",
                    dayName = "Salı",
                    meals = listOf(
                        "Etli Kuru Fasulye",
                        "Tel Şehriyeli Bulgur Pilavı",
                        "Turşu",
                        "Kakaolu Puding"
                    ),
                    calories = 1095
                ),
                AkdenizDiningMenuDay(
                    date = "2026-09-09",
                    dayName = "Çarşamba",
                    meals = listOf(
                        "Ezogelin Çorbası",
                        "Sebzeli Tavuk",
                        "Marul Lahana Salata",
                        "Yoğurt"
                    ),
                    calories = 924
                ),
                AkdenizDiningMenuDay(
                    date = "2026-09-10",
                    dayName = "Perşembe",
                    meals = listOf(
                        "Yalancı Paça Çorbası",
                        "Yoğurtlu Karışık Kızartma",
                        "Etli Arpa Şehriye Pilavı",
                        "Meyve"
                    ),
                    calories = 1072
                ),
                AkdenizDiningMenuDay(
                    date = "2026-09-11",
                    dayName = "Cuma",
                    meals = listOf(
                        "Domatesli Tel Şehriye Çorbası",
                        "Püreli Kebap",
                        "Havuç Salata",
                        "Yoğurt"
                    ),
                    calories = 854
                )
            )
        )
        return menu.takeIf { it.isCurrentWeek() }
    }
}

/** Menus are published for Antalya, so "today" follows Istanbul time rather than the phone's zone. */
fun todayInIstanbul(): String =
    Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Istanbul")).date.toString()
