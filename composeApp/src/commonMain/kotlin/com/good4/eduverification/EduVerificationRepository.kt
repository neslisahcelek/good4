package com.good4.eduverification

import com.good4.auth.data.repository.AuthRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import com.good4.core.network.callV2Function
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Only the server-written eligibility fields of `users/{uid}`. */
@Serializable
data class EduStatusDto(
    val eduEmail: String? = null,
    val eduVerified: Boolean? = null
)


sealed interface EduCodeRequestResult {
    data class Sent(val email: String, val resendAfterSeconds: Int, val sessionId: String? = null) : EduCodeRequestResult
    data class AlreadyVerified(val email: String) : EduCodeRequestResult
}

sealed interface EduCodeConfirmResult {
    data class Verified(val email: String) : EduCodeConfirmResult
    data class InvalidCode(val attemptsLeft: Int) : EduCodeConfirmResult
    data object Expired : EduCodeConfirmResult
    data object TooManyAttempts : EduCodeConfirmResult
}

interface EduVerificationSource {
    val currentUid: String?
    suspend fun status(): EduStatus
    suspend fun requestCode(email: String): EduCodeRequestResult
    suspend fun confirmCode(code: String, sessionId: String? = null): EduCodeConfirmResult
    suspend fun disconnectTest()
}

class EduVerificationRepository(
    private val store: FirestoreRepository,
    private val auth: AuthRepository
) : EduVerificationSource {
    private val access = CampusAccessRepository(auth)
    override val currentUid: String? get() = auth.currentUser?.uid
    override suspend fun status(): EduStatus {
        if (com.good4.core.util.AppEnvironment.firebaseBackend == com.good4.core.util.FirebaseBackend.V2) return access.status()
        val uid = currentUid ?: return EduStatus(false, null)
        return when (val result = store.getDocument("users", uid, EduStatusDto::class)) {
            is Result.Success -> EduStatus(result.data.eduVerified == true && !result.data.eduEmail.isNullOrBlank(), result.data.eduEmail)
            is Result.Error -> error("EDU_STATUS_LOAD_FAILED")
        }
    }
    override suspend fun disconnectTest() = access.disconnectTest()

    override suspend fun requestCode(email: String): EduCodeRequestResult {
        val uid = currentUid ?: error("AUTHENTICATION_REQUIRED")
        val revision = CampusAccessSession.revision(uid)
        val response = callV2Function("requestEduVerification", buildJsonObject { put("email", email.trim()) })
        check(currentUid == uid && CampusAccessSession.revision(uid) == revision) { "CAMPUS_EMAIL_ACCOUNT_MISMATCH" }
        val sentTo = response["email"]?.jsonPrimitive?.content ?: email.trim().lowercase()
        return when (response["outcome"]?.jsonPrimitive?.content) {
            "already_verified" -> EduCodeRequestResult.AlreadyVerified(sentTo)
            else -> EduCodeRequestResult.Sent(
                email = sentTo,
                resendAfterSeconds = response["resendAfterSeconds"]?.jsonPrimitive?.int ?: 60,
                sessionId = response["sessionId"]?.jsonPrimitive?.content
            )
        }
    }

    override suspend fun confirmCode(code: String, sessionId: String?): EduCodeConfirmResult {
        val uid = currentUid ?: error("AUTHENTICATION_REQUIRED")
        val revision = CampusAccessSession.revision(uid)
        val response = callV2Function("confirmEduVerification", buildJsonObject { put("code", code); sessionId?.let { put("sessionId", it) } })
        check(currentUid == uid && CampusAccessSession.revision(uid) == revision) { "CAMPUS_EMAIL_ACCOUNT_MISMATCH" }
        if (response["outcome"]?.jsonPrimitive?.content == "verified") CampusAccessSession.changed(uid)
        return when (response["outcome"]?.jsonPrimitive?.content) {
            "verified" -> EduCodeConfirmResult.Verified(response["email"]?.jsonPrimitive?.content.orEmpty())
            "expired" -> EduCodeConfirmResult.Expired
            "too_many_attempts" -> EduCodeConfirmResult.TooManyAttempts
            else -> EduCodeConfirmResult.InvalidCode(response["attemptsLeft"]?.jsonPrimitive?.int ?: 0)
        }
    }
}
