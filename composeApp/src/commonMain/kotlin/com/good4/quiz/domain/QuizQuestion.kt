package com.good4.quiz.domain

import kotlinx.serialization.Serializable

@Serializable
data class QuizQuestion(
    val index: Int = 0,
    val type: String = "multiple_choice",
    val text: String = "",
    val options: List<String> = emptyList(),
    val timeLimitSec: Int = 20,
    val correctIndex: Int? = null
) {
    val isTrueFalse: Boolean get() = type == "true_false"
}
