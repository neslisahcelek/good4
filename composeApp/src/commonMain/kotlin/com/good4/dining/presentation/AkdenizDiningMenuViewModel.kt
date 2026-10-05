package com.good4.dining.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.dining.domain.MealRating
import com.good4.dining.domain.ratingOpensAt
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import good4.composeapp.generated.resources.*
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

    private var menuJob: Job? = null
    private val voteJobs = mutableMapOf<DailyMeal, Job>()
    private val voteVersions = mutableMapOf<DailyMeal, Int>()
    private var generation = 0
    private var observedUserId = ratingRepository.currentUserId

    init {
        viewModelScope.launch {
            ratingRepository.authStateFlow.collect { user ->
                if (observedUserId != user?.uid) {
                    observedUserId = user?.uid
                    generation++
                    menuJob?.cancel()
                    voteJobs.values.forEach { it.cancel() }
                    voteJobs.clear()
                    _state.update { AkdenizDiningMenuState(isLoading = user != null) }
                    if (user != null) loadMenu()
                }
            }
        }
    }

    fun loadMenu() {
        val today = todayInIstanbul()
        val requestGeneration = ++generation
        menuJob?.cancel()
        voteJobs.values.forEach { it.cancel() }
        voteJobs.clear()
        voteVersions.clear()
        _state.update { AkdenizDiningMenuState(isLoading = true) }
        menuJob = viewModelScope.launch {
            val kykDay = async { kykRepository.getDayOrNext(today) }
            val menu = when (val result = repository.getCurrentMenu()) {
                is Result.Success -> result.data.takeIf { it.days.isNotEmpty() && it.isCurrentWeek() }
                is Result.Error -> null
            }
            val loaded = AkdenizDiningMenuState(
                menu = menu ?: currentWeekFallback(),
                kykDay = kykDay.await(),
                loadedDate = today,
                isLoading = false
            )
            if (generation != requestGeneration) return@launch
            _state.update { loaded }
            loadRatings(loaded, requestGeneration)
        }
    }

    /** Ratings are read only for meals that were published today (the KYK fallback day is not today). */
    private suspend fun loadRatings(loaded: AkdenizDiningMenuState, requestGeneration: Int) {
        publishedMeals(loaded).forEach { meal ->
            if (meal in _state.value.ratingInFlight) return@forEach
            val version = voteVersions[meal] ?: 0
            try {
                val rating = ratingRepository.load(loaded.loadedDate, meal)
                if (generation == requestGeneration && (voteVersions[meal] ?: 0) == version) {
                    _state.update { it.copy(ratings = it.ratings + (meal to rating)) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == requestGeneration && (voteVersions[meal] ?: 0) == version) _state.update {
                    it.copy(ratingLoadFailed = true, ratingError = UiText.StringResourceId(Res.string.meal_rating_load_error))
                }
            }
        }
    }

    private fun publishedMeals(loaded: AkdenizDiningMenuState): Set<DailyMeal> = buildSet {
        loaded.kykDay?.takeIf { it.date == loaded.loadedDate }?.let { day ->
            if (day.breakfast.isNotEmpty()) add(DailyMeal.KYK_BREAKFAST)
            if (day.dinner.isNotEmpty()) add(DailyMeal.KYK_DINNER)
        }
        if (!loaded.cafeteriaToday?.meals.isNullOrEmpty()) add(DailyMeal.CAFETERIA)
    }

    fun rate(meal: DailyMeal, vote: MealVote) {
        val current = _state.value
        val now = Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Istanbul"))
        if (current.loadedDate != now.date.toString()) { refreshIfDayChanged(); return }
        if (current.isLoading || meal !in publishedMeals(current) || !meal.ratingOpensAt(now.hour)) return
        if (meal in current.ratingInFlight || current.ratings[meal]?.myVote == vote) return
        val requestGeneration = generation
        voteVersions[meal] = (voteVersions[meal] ?: 0) + 1
        val before = current.ratings[meal]
        _state.update {
            it.copy(
                ratings = it.ratings + (meal to (before ?: MealRating()).copy(myVote = vote)),
                ratingInFlight = it.ratingInFlight + meal,
                ratingLoadFailed = false,
                ratingError = null
            )
        }
        voteJobs[meal] = viewModelScope.launch {
            try {
                val rating = ratingRepository.rate(current.loadedDate, meal, vote)
                if (generation == requestGeneration) _state.update {
                    it.copy(ratings = it.ratings + (meal to rating), ratingInFlight = it.ratingInFlight - meal)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == requestGeneration) _state.update {
                    it.copy(
                        ratings = if (before == null) it.ratings - meal else it.ratings + (meal to before),
                        ratingInFlight = it.ratingInFlight - meal,
                        ratingError = ratingErrorMessage(error.message)
                    )
                }
            }
        }
    }

    fun retryRatings() {
        val loaded = _state.value
        if (loaded.isLoading) return
        menuJob?.cancel()
        val requestGeneration = generation
        _state.update { it.copy(ratingLoadFailed = false, ratingError = null) }
        menuJob = viewModelScope.launch { loadRatings(loaded, requestGeneration) }
    }

    fun dismissRatingError() = _state.update { it.copy(ratingLoadFailed = false, ratingError = null) }

    private fun ratingErrorMessage(code: String?): UiText = UiText.StringResourceId(when {
        code?.contains("MEAL_NOT_STARTED") == true -> Res.string.meal_rating_not_started
        code?.contains("MEAL_DATE_CHANGED") == true -> Res.string.meal_rating_date_changed
        code?.contains("MEAL_NOT_PUBLISHED") == true -> Res.string.meal_rating_not_published
        code?.contains("ACCOUNT_NOT_ACTIVE") == true -> Res.string.meal_rating_inactive
        code?.contains("RATE_LIMIT") == true || code?.contains("resource-exhausted") == true -> Res.string.meal_rating_rate_limit
        else -> Res.string.meal_rating_save_error
    })

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
