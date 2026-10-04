package com.good4.dining

import com.good4.dining.domain.DailyMeal
import com.good4.dining.domain.MealRating
import com.good4.dining.domain.MealVote
import com.good4.dining.domain.ratingOpensAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MealRatingTest {
    @Test
    fun percentagesAppearOnlyAfterTenVotesAndAreRounded() {
        assertNull(MealRating(good = 5, okay = 2, bad = 2).percentages())
        assertEquals(
            mapOf(MealVote.GOOD to 64, MealVote.OKAY to 24, MealVote.BAD to 12),
            MealRating(good = 37, okay = 14, bad = 7).percentages()
        )
    }

    @Test
    fun mealsOpenForRatingWhenTheyStart() {
        assertFalse(DailyMeal.KYK_BREAKFAST.ratingOpensAt(5))
        assertTrue(DailyMeal.KYK_BREAKFAST.ratingOpensAt(6))
        assertFalse(DailyMeal.CAFETERIA.ratingOpensAt(10))
        assertTrue(DailyMeal.CAFETERIA.ratingOpensAt(11))
        assertFalse(DailyMeal.KYK_DINNER.ratingOpensAt(15))
        assertTrue(DailyMeal.KYK_DINNER.ratingOpensAt(16))
    }

    @Test
    fun votesRoundTripThroughTheirServerKeys() {
        MealVote.entries.forEach { assertEquals(it, MealVote.fromKey(it.key)) }
        assertNull(MealVote.fromKey("5"))
    }
}
