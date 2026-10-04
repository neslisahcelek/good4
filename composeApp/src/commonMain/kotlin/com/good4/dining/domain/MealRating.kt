package com.good4.dining.domain

/** The three faces a student can give a meal; [key] is what the rateMeal function stores. */
enum class MealVote(val key: String, val emoji: String) {
    GOOD("good", "😋"), OKAY("okay", "😐"), BAD("bad", "😕");

    companion object {
        fun fromKey(key: String?): MealVote? = entries.firstOrNull { it.key == key }
    }
}

/** Today's anonymous counters for one meal and the signed-in student's own vote, if any. */
data class MealRating(val good: Int = 0, val okay: Int = 0, val bad: Int = 0, val myVote: MealVote? = null) {
    val total: Int get() = good + okay + bad

    /** Percentages are shown only once enough people voted, so a handful of votes cannot mislead. */
    fun percentages(minimumVotes: Int = MEAL_RATING_MIN_VOTES): Map<MealVote, Int>? {
        if (total < minimumVotes) return null
        return mapOf(MealVote.GOOD to good, MealVote.OKAY to okay, MealVote.BAD to bad)
            .mapValues { (_, count) -> (count * 100 + total / 2) / total }
    }
}

const val MEAL_RATING_MIN_VOTES = 10

val DailyMeal.ratingKey: String
    get() = when (this) {
        DailyMeal.KYK_BREAKFAST -> "kyk_breakfast"
        DailyMeal.CAFETERIA -> "cafeteria"
        DailyMeal.KYK_DINNER -> "kyk_dinner"
    }

/** Same opening hours (Istanbul) the rateMeal function enforces: a meal is rated only once it has started. */
fun DailyMeal.ratingOpensAt(hour: Int): Boolean = hour >= when (this) {
    DailyMeal.KYK_BREAKFAST -> 6
    DailyMeal.CAFETERIA -> 11
    DailyMeal.KYK_DINNER -> 16
}
