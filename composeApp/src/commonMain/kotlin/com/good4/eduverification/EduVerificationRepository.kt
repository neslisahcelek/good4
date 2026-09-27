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

data class EduStatus(val verified: Boolean, val email: String?)

sealed interface EduCodeRequestResult {
    data class Sent(val email: String, val resendAfterSeconds: Int) : EduCodeRequestResult
    data class AlreadyVerified(val email: String) : EduCodeRequestResult
}

sealed interface EduCodeConfirmResult {
    data class Verified(val email: String) : EduCodeConfirmResult
    data class InvalidCode(val attemptsLeft: Int) : EduCodeConfirmResult
    data object Expired : EduCodeConfirmResult
    data object TooManyAttempts : EduCodeConfirmResult
}

class EduVerificationRepository(
    private val store: FirestoreRepository,
    private val auth: AuthRepository
) {
    suspend fun status(): EduStatus {
        val uid = auth.currentUser?.uid ?: return EduStatus(verified = false, email = null)
        return when (val result = store.getDocument("users", uid, EduStatusDto::class)) {
            is Result.Success -> EduStatus(
                verified = result.data.eduVerified == true && !result.data.eduEmail.isNullOrBlank(),
                email = result.data.eduEmail
            )
            is Result.Error -> error("Doğrulama durumu alınamadı. Bağlantınızı kontrol edip tekrar deneyin.")
        }
    }

    suspend fun requestCode(email: String): EduCodeRequestResult {
        val response = callV2Function("requestEduVerification", buildJsonObject { put("email", email.trim()) })
        val sentTo = response["email"]?.jsonPrimitive?.content ?: email.trim().lowercase()
        return when (response["outcome"]?.jsonPrimitive?.content) {
            "already_verified" -> EduCodeRequestResult.AlreadyVerified(sentTo)
            else -> EduCodeRequestResult.Sent(
                email = sentTo,
                resendAfterSeconds = response["resendAfterSeconds"]?.jsonPrimitive?.int ?: 60
            )
        }
    }

    suspend fun confirmCode(code: String): EduCodeConfirmResult {
        val response = callV2Function("confirmEduVerification", buildJsonObject { put("code", code) })
        return when (response["outcome"]?.jsonPrimitive?.content) {
            "verified" -> EduCodeConfirmResult.Verified(response["email"]?.jsonPrimitive?.content.orEmpty())
            "expired" -> EduCodeConfirmResult.Expired
            "too_many_attempts" -> EduCodeConfirmResult.TooManyAttempts
            else -> EduCodeConfirmResult.InvalidCode(response["attemptsLeft"]?.jsonPrimitive?.int ?: 0)
        }
    }
}

internal fun eduVerificationErrorMessage(error: Throwable): String = when (error.message) {
    "EDU_EMAIL_REQUIRED", "EDU_EMAIL_INVALID" -> "Geçerli bir .edu.tr e-posta adresi girin."
    "EDU_EMAIL_IN_USE" -> "Bu .edu.tr adresi başka bir Good4 hesabıyla doğrulanmış."
    "EDU_CODE_RESEND_TOO_SOON" -> "Yeni kod istemeden önce biraz bekleyin."
    "EDU_CODE_SEND_LIMIT" -> "Çok fazla kod istendi. Bir saat sonra tekrar deneyin."
    "EDU_CODE_FORMAT_INVALID" -> "Kod 6 haneli olmalı."
    "EDU_CODE_NOT_REQUESTED" -> "Önce doğrulama kodu isteyin."
    "ROLE_NOT_ALLOWED" -> "Bu doğrulama yalnızca öğrenci hesapları için."
    "ACCOUNT_NOT_ACTIVE" -> "Hesabınız henüz aktif değil."
    else -> error.message?.takeIf { it.any(Char::isLowerCase) }
        ?: "İşlem tamamlanamadı. Bağlantınızı kontrol edip tekrar deneyin."
}
