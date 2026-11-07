package com.good4.quiz.data.local

import kotlinx.datetime.Clock

data class ActiveQuizSession(
    val sessionId: String,
    val role: String, // "player" or "host"
    val title: String,
    val savedAtEpochMillis: Long
)

interface ActiveQuizStore {
    fun saveActiveSession(
        sessionId: String,
        role: String,
        title: String,
        savedAtEpochMillis: Long = Clock.System.now().toEpochMilliseconds()
    )
    fun getActiveSession(): ActiveQuizSession?
    fun clearActiveSession()
}
