package com.good4.quiz.data

import com.good4.quiz.domain.QuizPlayer
import com.good4.quiz.domain.QuizSession
import kotlinx.coroutines.flow.Flow

interface QuizGateway {
    fun observeSession(sessionId: String): Flow<QuizSession?>
    fun observePlayer(sessionId: String, userId: String): Flow<QuizPlayer?>
    fun observePlayers(sessionId: String): Flow<List<QuizPlayer>>
    fun observeAnswerCount(sessionId: String, questionIndex: Int): Flow<Int>
}

object NativeQuizGateway : QuizGateway {
    override fun observeSession(sessionId: String): Flow<QuizSession?> = observeQuizSession(sessionId)
    override fun observePlayer(sessionId: String, userId: String): Flow<QuizPlayer?> = observeQuizPlayer(sessionId, userId)
    override fun observePlayers(sessionId: String): Flow<List<QuizPlayer>> = observeQuizPlayers(sessionId)
    override fun observeAnswerCount(sessionId: String, questionIndex: Int): Flow<Int> = observeQuizAnswerCount(sessionId, questionIndex)
}

expect fun observeQuizSession(sessionId: String): Flow<QuizSession?>
expect fun observeQuizPlayer(sessionId: String, userId: String): Flow<QuizPlayer?>
expect fun observeQuizPlayers(sessionId: String): Flow<List<QuizPlayer>>
expect fun observeQuizAnswerCount(sessionId: String, questionIndex: Int): Flow<Int>
