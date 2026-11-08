package com.good4.quiz.data

import com.good4.quiz.domain.QuizLastResult
import com.good4.quiz.domain.QuizPlayer
import com.good4.quiz.domain.QuizQuestion
import com.good4.quiz.domain.QuizReveal
import com.good4.quiz.domain.QuizSession
import com.good4.quiz.domain.QuizTopPlayer
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

actual fun observeQuizSession(sessionId: String): Flow<QuizSession?> = Firebase.firestore
    .collection("quizSessions")
    .document(sessionId)
    .snapshots
    .map { doc ->
        if (doc.exists) parseIosQuizSession(doc) else null
    }
    .catch { emit(null) }

actual fun observeActiveQuizSessionForOrg(organizationId: String): Flow<QuizSession?> = Firebase.firestore
    .collection("quizSessions")
    .where { "organizationId" equalTo organizationId }
    .limit(5)
    .snapshots
    .map { snapshot ->
        snapshot.documents.map { parseIosQuizSession(it) }.firstOrNull { it.isActive }
    }
    .catch { emit(null) }

actual fun observeQuizPlayer(sessionId: String, userId: String): Flow<QuizPlayer?> = Firebase.firestore
    .collection("quizSessions")
    .document(sessionId)
    .collection("players")
    .document(userId)
    .snapshots
    .map { doc ->
        if (doc.exists) parseIosQuizPlayer(doc) else null
    }
    .catch { emit(null) }

actual fun observeQuizPlayers(sessionId: String): Flow<List<QuizPlayer>> = Firebase.firestore
    .collection("quizSessions")
    .document(sessionId)
    .collection("players")
    .limit(50)
    .snapshots
    .map { snapshot ->
        snapshot.documents.map { parseIosQuizPlayer(it) }
    }
    .catch { emit(emptyList()) }

actual fun observeQuizAnswerCount(sessionId: String, questionIndex: Int): Flow<Int> = Firebase.firestore
    .collection("quizSessions")
    .document(sessionId)
    .collection("counters")
    .limit(10)
    .snapshots
    .map { snapshot ->
        var total = 0
        for (doc in snapshot.documents) {
            if (doc.id.startsWith("s")) {
                val count = doc.getAsInt("a$questionIndex") ?: 0
                total += count
            }
        }
        total
    }
    .catch { emit(0) }

private fun DocumentSnapshot.getAsInt(field: String): Int? {
    getOrNull<Int>(field)?.let { return it }
    getOrNull<Long>(field)?.toInt()?.let { return it }
    getOrNull<Double>(field)?.toInt()?.let { return it }
    getOrNull<String>(field)?.toIntOrNull()?.let { return it }
    return null
}

private inline fun <reified T> DocumentSnapshot.getOrNull(field: String): T? {
    return try {
        get(field)
    } catch (_: Exception) {
        null
    }
}

private fun DocumentSnapshot.getEpochMillis(field: String): Long? {
    return try {
        getOrNull<Timestamp>(field)?.let { it.seconds * 1000 + it.nanoseconds / 1_000_000 }
            ?: getOrNull<Long>(field)
    } catch (_: Exception) {
        null
    }
}

private fun parseIosQuizSession(doc: DocumentSnapshot): QuizSession {
    val qMap = doc.getOrNull<Map<String, Any>>("currentQuestion")
    val currentQuestion = if (qMap != null) {
        QuizQuestion(
            index = (qMap["index"] as? Number)?.toInt() ?: 0,
            type = qMap["type"] as? String ?: "multiple_choice",
            text = qMap["text"] as? String ?: "",
            options = (qMap["options"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
            timeLimitSec = (qMap["timeLimitSec"] as? Number)?.toInt() ?: 20
        )
    } else null

    val rMap = doc.getOrNull<Map<String, Any>>("reveal")
    val reveal = if (rMap != null) {
        QuizReveal(
            index = (rMap["index"] as? Number)?.toInt() ?: 0,
            correctIndex = (rMap["correctIndex"] as? Number)?.toInt() ?: 0,
            distribution = (rMap["distribution"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
            unanswered = (rMap["unanswered"] as? Number)?.toInt() ?: 0,
            top5 = (rMap["top5"] as? List<*>)?.mapNotNull { item ->
                val p = item as? Map<*, *> ?: return@mapNotNull null
                QuizTopPlayer(
                    nickname = p["nickname"] as? String ?: "",
                    score = (p["score"] as? Number)?.toInt() ?: 0,
                    rank = (p["rank"] as? Number)?.toInt() ?: 0
                )
            } ?: emptyList()
        )
    } else null

    val podium = (doc.getOrNull<List<Any>>("podium"))?.mapNotNull { item ->
        val p = item as? Map<*, *> ?: return@mapNotNull null
        QuizTopPlayer(
            nickname = p["nickname"] as? String ?: "",
            score = (p["score"] as? Number)?.toInt() ?: 0,
            rank = (p["rank"] as? Number)?.toInt() ?: 0
        )
    }

    return QuizSession(
        id = doc.id,
        organizationId = doc.getOrNull<String>("organizationId").orEmpty(),
        quizId = doc.getOrNull<String>("quizId").orEmpty(),
        hostUid = doc.getOrNull<String>("hostUid").orEmpty(),
        title = doc.getOrNull<String>("title").orEmpty(),
        status = doc.getOrNull<String>("status") ?: "lobby",
        locked = doc.getOrNull<Boolean>("locked") ?: false,
        totalQuestions = doc.getAsInt("totalQuestions") ?: 0,
        currentIndex = doc.getAsInt("currentIndex") ?: -1,
        questionStartsAt = doc.getEpochMillis("questionStartsAt"),
        questionEndsAt = doc.getEpochMillis("questionEndsAt"),
        currentQuestion = currentQuestion,
        reveal = reveal,
        podium = podium
    )
}

private fun parseIosQuizPlayer(doc: DocumentSnapshot): QuizPlayer {
    val lMap = doc.getOrNull<Map<String, Any>>("lastResult")
    val lastResult = if (lMap != null) {
        QuizLastResult(
            index = (lMap["index"] as? Number)?.toInt() ?: 0,
            correct = lMap["correct"] as? Boolean ?: false,
            points = (lMap["points"] as? Number)?.toInt() ?: 0,
            selectedOption = (lMap["selectedOption"] as? Number)?.toInt() ?: -1
        )
    } else null

    return QuizPlayer(
        userId = doc.getOrNull<String>("userId") ?: doc.id,
        nickname = doc.getOrNull<String>("nickname").orEmpty(),
        status = doc.getOrNull<String>("status") ?: "active",
        score = doc.getAsInt("score") ?: 0,
        rank = doc.getAsInt("rank") ?: 0,
        streak = doc.getAsInt("streak") ?: 0,
        correctCount = doc.getAsInt("correctCount") ?: 0,
        lastResult = lastResult
    )
}
