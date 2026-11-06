package com.good4.quiz.domain

import kotlinx.serialization.Serializable

@Serializable
enum class QuizPhase(val value: String) {
    LOBBY("lobby"),
    QUESTION("question"),
    REVEAL("reveal"),
    FINISHED("finished"),
    CANCELLED("cancelled");

    companion object {
        fun fromString(value: String?): QuizPhase =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: LOBBY
    }
}
