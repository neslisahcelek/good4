package com.good4.quiz.domain

import kotlinx.serialization.Serializable

@Serializable
data class QuizReport(
    val sessionId: String = "",
    val title: String = "",
    val playerCount: Int = 0,
    val totalQuestions: Int = 0,
    val ranking: List<QuizReportRankingItem> = emptyList(),
    val questions: List<QuizReportQuestionSummary> = emptyList(),
    val finishedAtMs: Long = 0
)

@Serializable
data class QuizReportRankingItem(
    val rank: Int = 0,
    val nickname: String = "",
    val score: Int = 0,
    val correctCount: Int = 0
)

@Serializable
data class QuizReportQuestionSummary(
    val index: Int = 0,
    val text: String = "",
    val answered: Int = 0,
    val correctPercent: Int = 0,
    val avgElapsedMs: Int = 0
)
