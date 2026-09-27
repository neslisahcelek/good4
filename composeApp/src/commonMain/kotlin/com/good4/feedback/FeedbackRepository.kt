package com.good4.feedback

import com.good4.auth.data.repository.AuthRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import com.good4.core.network.callV2Function
import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class FeedbackSubmissionDto(
    val userId: String,
    val userEmail: String = "",
    val userDisplayName: String = "",
    val subject: String,
    val message: String,
    val source: String = "mobile",
    val environment: String,
    val status: String = "new",
    val createdAt: Long
)

class FeedbackRepository(
    private val store: FirestoreRepository,
    private val auth: AuthRepository
) {
    suspend fun submit(
        subject: String,
        message: String,
        reportCommunityId: String? = null,
        reportEntryId: String? = null,
        reportEntryKind: String? = null
    ) {
        val normalizedSubject = subject.trim()
        val normalizedMessage = message.trim()
        require(normalizedSubject.length in 3..120) { "Konu 3–120 karakter olmalıdır." }
        require(normalizedMessage.length in 10..2000) { "Geri bildirim 10–2000 karakter olmalıdır." }

        val user = requireNotNull(auth.currentUser) { "Geri bildirim göndermek için giriş yapmalısınız." }
        if (AppEnvironment.firebaseBackend == FirebaseBackend.V2) {
            callV2Function("submitFeedback", buildJsonObject {
                put("subject", normalizedSubject)
                put("message", normalizedMessage)
                if (reportCommunityId != null && reportEntryId != null && reportEntryKind != null) {
                    put("reportCommunityId", reportCommunityId)
                    put("reportEntryId", reportEntryId)
                    put("reportEntryKind", reportEntryKind)
                }
            })
            return
        }

        val environment = when (AppEnvironment.firebaseBackend) {
            FirebaseBackend.LEGACY_TEST -> "legacyTest"
            FirebaseBackend.PRODUCTION -> "production"
            FirebaseBackend.V2 -> "v2"
        }
        val result = store.addDocument(
            collectionPath = "feedbackSubmissions",
            data = FeedbackSubmissionDto(
                userId = user.uid,
                userEmail = user.email.orEmpty(),
                userDisplayName = user.displayName.orEmpty(),
                subject = normalizedSubject,
                message = normalizedMessage,
                environment = environment,
                createdAt = Clock.System.now().epochSeconds
            )
        )
        check(result is Result.Success) { "Geri bildirim gönderilemedi. Lütfen tekrar deneyin." }
    }
}
