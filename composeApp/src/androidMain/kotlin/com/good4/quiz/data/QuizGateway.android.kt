package com.good4.quiz.data

import com.good4.quiz.domain.QuizLastResult
import com.good4.quiz.domain.QuizPlayer
import com.good4.quiz.domain.QuizQuestion
import com.good4.quiz.domain.QuizReveal
import com.good4.quiz.domain.QuizSession
import com.good4.quiz.domain.QuizTopPlayer
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual fun observeQuizSession(sessionId: String): Flow<QuizSession?> = callbackFlow {
    val listener = FirebaseFirestore.getInstance()
        .collection("quizSessions")
        .document(sessionId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null && snapshot.exists()) {
                trySend(parseAndroidQuizSession(snapshot))
            } else {
                trySend(null)
            }
        }
    awaitClose { listener.remove() }
}

actual fun observeActiveQuizSessionForOrg(organizationId: String): Flow<QuizSession?> = callbackFlow {
    val listener = FirebaseFirestore.getInstance()
        .collection("quizSessions")
        .whereEqualTo("organizationId", organizationId)
        .whereIn("status", listOf("lobby", "question", "reveal"))
        .limit(1)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
            } else if (snapshot != null && !snapshot.isEmpty) {
                trySend(parseAndroidQuizSession(snapshot.documents[0]))
            } else {
                trySend(null)
            }
        }
    awaitClose { listener.remove() }
}

actual fun observeQuizPlayer(sessionId: String, userId: String): Flow<QuizPlayer?> = callbackFlow {
    val listener = FirebaseFirestore.getInstance()
        .collection("quizSessions")
        .document(sessionId)
        .collection("players")
        .document(userId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null && snapshot.exists()) {
                trySend(parseAndroidQuizPlayer(snapshot))
            } else {
                trySend(null)
            }
        }
    awaitClose { listener.remove() }
}

actual fun observeQuizPlayers(sessionId: String): Flow<List<QuizPlayer>> = callbackFlow {
    val listener = FirebaseFirestore.getInstance()
        .collection("quizSessions")
        .document(sessionId)
        .collection("players")
        .limit(50)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null) {
                val players = snapshot.documents.map { parseAndroidQuizPlayer(it) }
                trySend(players)
            }
        }
    awaitClose { listener.remove() }
}

actual fun observeQuizAnswerCount(sessionId: String, questionIndex: Int): Flow<Int> = callbackFlow {
    val listener = FirebaseFirestore.getInstance()
        .collection("quizSessions")
        .document(sessionId)
        .collection("counters")
        .limit(10)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null) {
                var total = 0
                for (doc in snapshot.documents) {
                    if (doc.id.startsWith("s")) {
                        total += (doc.getLong("a$questionIndex") ?: 0L).toInt()
                    }
                }
                trySend(total)
            }
        }
    awaitClose { listener.remove() }
}

private fun parseAndroidQuizSession(doc: DocumentSnapshot): QuizSession {
    val qMap = doc.get("currentQuestion") as? Map<*, *>
    val currentQuestion = if (qMap != null) {
        QuizQuestion(
            index = (qMap["index"] as? Number)?.toInt() ?: 0,
            type = qMap["type"] as? String ?: "multiple_choice",
            text = qMap["text"] as? String ?: "",
            options = (qMap["options"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
            timeLimitSec = (qMap["timeLimitSec"] as? Number)?.toInt() ?: 20
        )
    } else null

    val rMap = doc.get("reveal") as? Map<*, *>
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

    val podium = (doc.get("podium") as? List<*>)?.mapNotNull { item ->
        val p = item as? Map<*, *> ?: return@mapNotNull null
        QuizTopPlayer(
            nickname = p["nickname"] as? String ?: "",
            score = (p["score"] as? Number)?.toInt() ?: 0,
            rank = (p["rank"] as? Number)?.toInt() ?: 0
        )
    }

    val startsAt = doc.getTimestamp("questionStartsAt")?.toDate()?.time
        ?: (doc.get("questionStartsAt") as? Number)?.toLong()
    val endsAt = doc.getTimestamp("questionEndsAt")?.toDate()?.time
        ?: (doc.get("questionEndsAt") as? Number)?.toLong()

    return QuizSession(
        id = doc.id,
        organizationId = doc.getString("organizationId").orEmpty(),
        quizId = doc.getString("quizId").orEmpty(),
        hostUid = doc.getString("hostUid").orEmpty(),
        title = doc.getString("title").orEmpty(),
        status = doc.getString("status") ?: "lobby",
        locked = doc.getBoolean("locked") ?: false,
        totalQuestions = doc.getLong("totalQuestions")?.toInt() ?: 0,
        currentIndex = doc.getLong("currentIndex")?.toInt() ?: -1,
        questionStartsAt = startsAt,
        questionEndsAt = endsAt,
        currentQuestion = currentQuestion,
        reveal = reveal,
        podium = podium
    )
}

private fun parseAndroidQuizPlayer(doc: DocumentSnapshot): QuizPlayer {
    val lMap = doc.get("lastResult") as? Map<*, *>
    val lastResult = if (lMap != null) {
        QuizLastResult(
            index = (lMap["index"] as? Number)?.toInt() ?: 0,
            correct = lMap["correct"] as? Boolean ?: false,
            points = (lMap["points"] as? Number)?.toInt() ?: 0,
            selectedOption = (lMap["selectedOption"] as? Number)?.toInt() ?: -1
        )
    } else null

    return QuizPlayer(
        userId = doc.getString("userId") ?: doc.id,
        nickname = doc.getString("nickname").orEmpty(),
        status = doc.getString("status") ?: "active",
        score = doc.getLong("score")?.toInt() ?: 0,
        rank = doc.getLong("rank")?.toInt() ?: 0,
        streak = doc.getLong("streak")?.toInt() ?: 0,
        correctCount = doc.getLong("correctCount")?.toInt() ?: 0,
        lastResult = lastResult
    )
}
