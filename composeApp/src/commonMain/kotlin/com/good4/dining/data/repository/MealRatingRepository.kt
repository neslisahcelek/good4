package com.good4.dining.data.repository

import com.good4.auth.data.repository.AuthRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import com.good4.core.domain.Error
import com.good4.core.network.callV2Function
import com.good4.dining.domain.DailyMeal
import com.good4.dining.domain.MealRating
import com.good4.dining.domain.MealVote
import com.good4.dining.domain.ratingKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class MealRatingSummaryDto(val good: Int = 0, val okay: Int = 0, val bad: Int = 0)

@Serializable
data class MealRatingVoteDto(val rating: String? = null)

/** Reads meal_ratings/{date}_{meal} and the student's own vote; votes go through the rateMeal function. */
class MealRatingRepository(
    private val firestoreRepository: FirestoreRepository,
    private val authRepository: AuthRepository
) {
    val authStateFlow get() = authRepository.authStateFlow
    val currentUserId get() = authRepository.currentUser?.uid

    private fun <T> Result<T, Error>.orMissing(): T? = when (this) {
        is Result.Success -> data
        is Result.Error -> if (error.message == "Document not found") null else throw IllegalStateException(error.message)
    }

    suspend fun load(date: String, meal: DailyMeal): MealRating {
        val id = "${date}_${meal.ratingKey}"
        val summary = firestoreRepository.getDocument("meal_ratings", id, MealRatingSummaryDto::class).orMissing()
        val uid = authRepository.currentUser?.uid
        val vote = uid?.let {
            firestoreRepository.getDocument("meal_ratings/$id/votes", it, MealRatingVoteDto::class).orMissing()
        }
        return MealRating(
            good = summary?.good ?: 0, okay = summary?.okay ?: 0, bad = summary?.bad ?: 0,
            myVote = MealVote.fromKey(vote?.rating)
        )
    }

    suspend fun rate(date: String, meal: DailyMeal, vote: MealVote): MealRating {
        val response = callV2Function("rateMeal", buildJsonObject {
            put("date", date)
            put("meal", meal.ratingKey)
            put("rating", vote.key)
        })
        return MealRating(
            good = response["good"]?.jsonPrimitive?.int ?: 0,
            okay = response["okay"]?.jsonPrimitive?.int ?: 0,
            bad = response["bad"]?.jsonPrimitive?.int ?: 0,
            myVote = vote
        )
    }
}
