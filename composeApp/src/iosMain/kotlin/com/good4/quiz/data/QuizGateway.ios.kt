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
import kotlinx.serialization.Serializable

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
    .where { ("organizationId" equalTo organizationId) and ("status" inArray listOf("lobby", "question", "reveal")) }
    .limit(1)
    .snapshots
    .map { snapshot ->
        snapshot.documents.firstOrNull()?.let { parseIosQuizSession(it) }
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

// gitlive decodes fields through kotlinx.serialization, so nested maps need serializable shapes;
// `Map<String, Any>` has no serializer and always came back null.
@Serializable
private data class IosQuizQuestionDto(
    val index: Int = 0,
    val type: String = "multiple_choice",
    val text: String = "",
    val options: List<String> = emptyList(),
    val timeLimitSec: Int = 20,
)

@Serializable
private data class IosQuizTopPlayerDto(val nickname: String = "", val score: Int = 0, val rank: Int = 0)

@Serializable
private data class IosQuizRevealDto(
    val index: Int = 0,
    val correctIndex: Int = 0,
    val distribution: List<Int> = emptyList(),
    val unanswered: Int = 0,
    val top5: List<IosQuizTopPlayerDto> = emptyList(),
)

@Serializable
private data class IosQuizLastResultDto(
    val index: Int = 0,
    val correct: Boolean = false,
    val points: Int = 0,
    val selectedOption: Int = -1,
)

private fun IosQuizTopPlayerDto.toDomain() = QuizTopPlayer(nickname = nickname, score = score, rank = rank)

private fun parseIosQuizSession(doc: DocumentSnapshot): QuizSession {
    val currentQuestion = doc.getOrNull<IosQuizQuestionDto>("currentQuestion")?.let {
        QuizQuestion(index = it.index, type = it.type, text = it.text, options = it.options, timeLimitSec = it.timeLimitSec)
    }
    val reveal = doc.getOrNull<IosQuizRevealDto>("reveal")?.let {
        QuizReveal(
            index = it.index,
            correctIndex = it.correctIndex,
            distribution = it.distribution,
            unanswered = it.unanswered,
            top5 = it.top5.map { player -> player.toDomain() }
        )
    }
    val podium = doc.getOrNull<List<IosQuizTopPlayerDto>>("podium")?.map { it.toDomain() }

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
    val lastResult = doc.getOrNull<IosQuizLastResultDto>("lastResult")?.let {
        QuizLastResult(index = it.index, correct = it.correct, points = it.points, selectedOption = it.selectedOption)
    }

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
