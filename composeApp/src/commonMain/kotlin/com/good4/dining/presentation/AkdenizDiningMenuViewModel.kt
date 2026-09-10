package com.good4.dining.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.domain.Result
import com.good4.dining.data.repository.AkdenizDiningMenuRepository
import com.good4.dining.domain.AkdenizDiningMenu
import com.good4.dining.domain.AkdenizDiningMenuDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AkdenizDiningMenuViewModel(
    private val repository: AkdenizDiningMenuRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AkdenizDiningMenuState())
    val state = _state.asStateFlow()

    fun loadMenu() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getCurrentMenu()) {
                is Result.Success -> {
                    val menu = result.data.takeIf { it.days.isNotEmpty() && it.isCurrentWeek() }
                    _state.value = AkdenizDiningMenuState(
                        menu = menu ?: currentWeekFallback(),
                        isLoading = false
                    )
                }

                is Result.Error -> {
                    _state.value = AkdenizDiningMenuState(
                        menu = currentWeekFallback(),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun AkdenizDiningMenu.isCurrentWeek(): Boolean {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
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
