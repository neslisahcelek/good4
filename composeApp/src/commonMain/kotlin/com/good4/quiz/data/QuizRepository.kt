package com.good4.quiz.data

import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import com.good4.core.network.callV2Function
import com.good4.quiz.domain.QuizJoinResult
import com.good4.quiz.domain.QuizReport
import com.good4.quiz.domain.QuizReportQuestionSummary
import com.good4.quiz.domain.QuizReportRankingItem
import com.good4.quiz.domain.QuizSession
import com.good4.quiz.domain.QuizSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlin.random.Random

class QuizRepository(
    private val json: Json = Json { ignoreUnknownKeys = true; coerceInputValues = true },
    private val gateway: QuizGateway = NativeQuizGateway
) {
    /**
     * Observes the active quiz session for a community in real-time.
     */
    fun observeActiveQuizSession(organizationId: String): Flow<QuizSession?> =
        gateway.observeActiveQuizSession(organizationId)

    /**
     * Joins a quiz session with the given nickname.
     * Retries up to 2 times with jitter if aborted / contention occurs.
     */
    suspend fun joinQuizSession(sessionId: String, nickname: String): Result<QuizJoinResult, NetworkError> {
        var attempts = 0
        var lastError = "İşlem başarısız."
        while (attempts < 3) {
            attempts++
            try {
                val response = callV2Function(
                    "joinQuizSession",
                    buildJsonObject {
                        put("sessionId", sessionId.trim())
                        put("nickname", nickname.trim())
                    }
                )
                val retSessionId = response["sessionId"]?.jsonPrimitive?.content ?: sessionId
                val retNickname = response["nickname"]?.jsonPrimitive?.content ?: nickname
                val serverTimeMs = response["serverTimeMs"]?.jsonPrimitive?.longOrNull ?: 0L
                return Result.Success(QuizJoinResult(retSessionId, retNickname, serverTimeMs))
            } catch (e: Exception) {
                lastError = e.message ?: "İşlem başarısız."
                if (lastError.contains("aborted", ignoreCase = true) && attempts < 3) {
                    delay(Random.nextLong(100, 300))
                    continue
                }
                return Result.Error(NetworkError(lastError))
            }
        }
        return Result.Error(NetworkError(lastError))
    }

    suspend fun submitQuizAnswer(sessionId: String, index: Int, option: Int): Result<Boolean, NetworkError> {
        return try {
            val response = callV2Function(
                "submitQuizAnswer",
                buildJsonObject {
                    put("sessionId", sessionId.trim())
                    put("index", index)
                    put("option", option)
                }
            )
            val accepted = response["accepted"]?.jsonPrimitive?.booleanOrNull ?: true
            Result.Success(accepted)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Cevap gönderilemedi."))
        }
    }

    suspend fun revealQuizQuestion(sessionId: String, index: Int): Result<Boolean, NetworkError> {
        return try {
            val response = callV2Function(
                "revealQuizQuestion",
                buildJsonObject {
                    put("sessionId", sessionId.trim())
                    put("index", index)
                }
            )
            val alreadyRevealed = response["alreadyRevealed"]?.jsonPrimitive?.booleanOrNull ?: false
            Result.Success(!alreadyRevealed)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Soru sonlandırılamadı."))
        }
    }

    suspend fun startQuizSession(organizationId: String, quizId: String): Result<String, NetworkError> {
        return try {
            val response = callV2Function(
                "startQuizSession",
                buildJsonObject {
                    put("organizationId", organizationId.trim())
                    put("quizId", quizId.trim())
                }
            )
            val sessionId = response["sessionId"]?.jsonPrimitive?.content
                ?: return Result.Error(NetworkError("Oturum kimliği alınamadı."))
            Result.Success(sessionId)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Oturum başlatılamadı."))
        }
    }

    suspend fun startQuizGame(sessionId: String): Result<Boolean, NetworkError> {
        return try {
            callV2Function(
                "startQuizGame",
                buildJsonObject {
                    put("sessionId", sessionId.trim())
                }
            )
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Oyun başlatılamadı."))
        }
    }

    suspend fun setQuizLock(sessionId: String, locked: Boolean): Result<Boolean, NetworkError> {
        return try {
            val response = callV2Function(
                "setQuizLock",
                buildJsonObject {
                    put("sessionId", sessionId.trim())
                    put("locked", locked)
                }
            )
            val isLocked = response["locked"]?.jsonPrimitive?.booleanOrNull ?: locked
            Result.Success(isLocked)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Kilit durumu değiştirilemedi."))
        }
    }

    suspend fun nextQuizQuestion(sessionId: String): Result<String, NetworkError> {
        return try {
            val response = callV2Function(
                "nextQuizQuestion",
                buildJsonObject {
                    put("sessionId", sessionId.trim())
                }
            )
            val status = response["status"]?.jsonPrimitive?.content ?: "question"
            Result.Success(status)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Sonraki soruya geçilemedi."))
        }
    }

    suspend fun kickQuizPlayer(sessionId: String, playerId: String): Result<Boolean, NetworkError> {
        return try {
            val response = callV2Function(
                "kickQuizPlayer",
                buildJsonObject {
                    put("sessionId", sessionId.trim())
                    put("playerId", playerId.trim())
                }
            )
            val kicked = response["kicked"]?.jsonPrimitive?.booleanOrNull ?: true
            Result.Success(kicked)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Oyuncu çıkarılamadı."))
        }
    }

    suspend fun cancelQuizSession(sessionId: String): Result<Boolean, NetworkError> {
        return try {
            callV2Function(
                "cancelQuizSession",
                buildJsonObject {
                    put("sessionId", sessionId.trim())
                }
            )
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Oturum iptal edilemedi."))
        }
    }

    suspend fun getQuizReport(sessionId: String): Result<QuizReport, NetworkError> {
        return try {
            val response = callV2Function(
                "getQuizReport",
                buildJsonObject {
                    put("sessionId", sessionId.trim())
                }
            )
            val ranking = response["ranking"]?.jsonArray?.map { item ->
                val obj = item.jsonObject
                QuizReportRankingItem(
                    rank = obj["rank"]?.jsonPrimitive?.intOrNull ?: 0,
                    nickname = obj["nickname"]?.jsonPrimitive?.content ?: "",
                    score = obj["score"]?.jsonPrimitive?.intOrNull ?: 0,
                    correctCount = obj["correctCount"]?.jsonPrimitive?.intOrNull ?: 0
                )
            } ?: emptyList()

            val questions = response["questions"]?.jsonArray?.map { item ->
                val obj = item.jsonObject
                QuizReportQuestionSummary(
                    index = obj["index"]?.jsonPrimitive?.intOrNull ?: 0,
                    text = obj["text"]?.jsonPrimitive?.content ?: "",
                    answered = obj["answered"]?.jsonPrimitive?.intOrNull ?: 0,
                    correctPercent = obj["correctPercent"]?.jsonPrimitive?.intOrNull ?: 0,
                    avgElapsedMs = obj["avgElapsedMs"]?.jsonPrimitive?.intOrNull ?: 0
                )
            } ?: emptyList()

            val report = QuizReport(
                sessionId = response["sessionId"]?.jsonPrimitive?.content ?: sessionId,
                title = response["title"]?.jsonPrimitive?.content ?: "",
                playerCount = response["playerCount"]?.jsonPrimitive?.intOrNull ?: 0,
                totalQuestions = response["totalQuestions"]?.jsonPrimitive?.intOrNull ?: 0,
                ranking = ranking,
                questions = questions,
                finishedAtMs = response["finishedAtMs"]?.jsonPrimitive?.longOrNull ?: 0L
            )
            Result.Success(report)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Rapor yüklenemedi."))
        }
    }

    suspend fun listQuizzes(organizationId: String): Result<List<QuizSummary>, NetworkError> {
        return try {
            val response = callV2Function(
                "listQuizzes",
                buildJsonObject {
                    put("organizationId", organizationId.trim())
                }
            )
            val list = response["quizzes"]?.jsonArray?.map { item ->
                val obj = item.jsonObject
                QuizSummary(
                    quizId = obj["quizId"]?.jsonPrimitive?.content ?: "",
                    title = obj["title"]?.jsonPrimitive?.content ?: "",
                    status = obj["status"]?.jsonPrimitive?.content ?: "draft",
                    questionCount = obj["questionCount"]?.jsonPrimitive?.intOrNull ?: 0,
                    updatedAtMs = obj["updatedAtMs"]?.jsonPrimitive?.longOrNull ?: 0L
                )
            } ?: emptyList()
            Result.Success(list)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Quiz listesi yüklenemedi."))
        }
    }
}
