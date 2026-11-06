package com.good4.quiz.domain

import kotlinx.serialization.Serializable

@Serializable
data class QuizPlayer(
    val userId: String = "",
    val nickname: String = "",
    val status: String = "active",
    val score: Int = 0,
    val rank: Int = 0,
    val streak: Int = 0,
    val correctCount: Int = 0,
    val lastResult: QuizLastResult? = null
) {
    val isActive: Boolean get() = status == "active"
    val isKicked: Boolean get() = status == "kicked"
}

@Serializable
data class QuizLastResult(
    val index: Int = 0,
    val correct: Boolean = false,
    val points: Int = 0,
    val selectedOption: Int = -1
)
