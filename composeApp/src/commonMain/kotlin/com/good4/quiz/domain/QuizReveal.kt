package com.good4.quiz.domain

import kotlinx.serialization.Serializable

@Serializable
data class QuizReveal(
    val index: Int = 0,
    val correctIndex: Int = 0,
    val distribution: List<Int> = emptyList(),
    val unanswered: Int = 0,
    val top5: List<QuizTopPlayer> = emptyList()
)

@Serializable
data class QuizTopPlayer(
    val nickname: String = "",
    val score: Int = 0,
    val rank: Int = 0
)
