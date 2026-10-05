package com.good4.dining.presentation

import com.good4.core.presentation.UiText
import com.good4.dining.domain.AkdenizDiningMenu
import com.good4.dining.domain.AkdenizDiningMenuDay
import com.good4.dining.domain.DailyMeal
import com.good4.dining.domain.KykMenuDay
import com.good4.dining.domain.MealRating
import kotlinx.datetime.LocalDate

data class AkdenizDiningMenuState(
    val menu: AkdenizDiningMenu? = null,
    /** Today's KYK menu, or the next published day when today has none. */
    val kykDay: KykMenuDay? = null,
    /** The Istanbul date (yyyy-MM-dd) the menus were loaded for; a new day triggers a reload. */
    val loadedDate: String = "",
    val isLoading: Boolean = true,
    /** Today's 😋/😐/😕 counters and own vote per meal; missing until loaded. */
    val ratings: Map<DailyMeal, MealRating> = emptyMap(),
    val ratingInFlight: Set<DailyMeal> = emptySet(),
    val ratingLoadFailed: Boolean = false,
    val ratingError: UiText? = null
) {
    val cafeteriaToday: AkdenizDiningMenuDay?
        get() = menu?.days?.firstOrNull { it.date == loadedDate }

    /** Null when [kykDay] is today; otherwise "Yarın" or a short date such as "28 Eylül". */
    val kykDayLabel: String?
        get() {
            val day = kykDay ?: return null
            if (day.date == loadedDate) return null
            val shown = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return day.date
            val today = runCatching { LocalDate.parse(loadedDate) }.getOrNull()
            if (today != null && shown.toEpochDays() - today.toEpochDays() == 1) return "Yarın"
            val months = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
                "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
            return "${shown.dayOfMonth} ${months[shown.monthNumber - 1]}"
        }
}
