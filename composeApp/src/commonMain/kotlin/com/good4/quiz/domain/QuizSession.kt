package com.good4.quiz.domain

import kotlinx.serialization.Serializable

@Serializable
data class QuizSession(
    val id: String = "",
    val organizationId: String = "",
    val quizId: String = "",
    val hostUid: String = "",
    val title: String = "",
    val status: String = "lobby",
    val locked: Boolean = false,
    val totalQuestions: Int = 0,
    val currentIndex: Int = -1,
    val questionStartsAt: Long? = null,
    val questionEndsAt: Long? = null,
    val currentQuestion: QuizQuestion? = null,
    val reveal: QuizReveal? = null,
    val podium: List<QuizTopPlayer>? = null
) {
    val phase: QuizPhase get() = QuizPhase.fromString(status)
    val isLobby: Boolean get() = phase == QuizPhase.LOBBY
    val isQuestion: Boolean get() = phase == QuizPhase.QUESTION
    val isReveal: Boolean get() = phase == QuizPhase.REVEAL
    val isFinished: Boolean get() = phase == QuizPhase.FINISHED
    val isCancelled: Boolean get() = phase == QuizPhase.CANCELLED
}

@Serializable
data class QuizSummary(
    val quizId: String = "",
    val title: String = "",
    val status: String = "draft",
    val questionCount: Int = 0,
    val updatedAtMs: Long = 0
)

@Serializable
data class QuizJoinResult(
    val sessionId: String = "",
    val nickname: String = "",
    val serverTimeMs: Long = 0L
)
