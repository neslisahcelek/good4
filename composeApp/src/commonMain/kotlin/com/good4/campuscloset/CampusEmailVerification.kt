package com.good4.campuscloset

import com.good4.auth.data.repository.AuthRepository
import com.good4.eduverification.EduCodeRequestResult
import com.good4.eduverification.EduCodeConfirmResult
import com.good4.core.network.callV2Function
import io.ktor.http.Url
import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

const val CAMPUS_EMAIL_DOMAIN = "ogr.akdeniz.edu.tr"
const val CAMPUS_EMAIL_LINK_HOST = "good4tr-v2.firebaseapp.com"

fun isCampusStudentEmail(email: String): Boolean {
    val normalized = email.trim().lowercase()
    return normalized.length <= 254 && Regex("^[^\\s@/<>]+@ogr\\.akdeniz\\.edu\\.tr$").matches(normalized)
}

@Serializable
data class PendingCampusEmailVerification(
    val uid: String,
    val email: String,
    val requestId: String,
    val expiresAtMillis: Long
)

data class CampusEmailLink(val requestId: String, val firebaseLink: String)

/** Only our Hosting action links with an opaque, account-bound verification request are accepted. */
fun parseCampusEmailLink(raw: String): CampusEmailLink? = runCatching {
    if (raw.length > 16_384) return null
    val hosts = setOf(CAMPUS_EMAIL_LINK_HOST, "good4tr-v2.web.app")
    val fields = setOf("link", "mode", "oobCode", "apiKey", "continueUrl", "requestId")
    fun trusted(url: Url): Boolean = url.protocol.name == "https" && url.host in hosts && url.port == 443
        && url.user.isNullOrEmpty() && url.password.isNullOrEmpty()
        && fields.none { (url.parameters.getAll(it)?.size ?: 0) > 1 }
    var action = Url(raw)
    repeat(3) {
        if (!trusted(action)) return null
        val nested = action.parameters["link"]
        if (nested != null) action = Url(nested)
    }
    if (!trusted(action) || action.parameters["link"] != null) return null
    val landing = action.encodedPath == "/campus-email-verification"
    if ((!landing && (action.host != CAMPUS_EMAIL_LINK_HOST
            || action.encodedPath !in setOf("/__/auth/action", "/__/auth/links")))
        || action.parameters["mode"] != "signIn" || action.parameters["oobCode"].isNullOrBlank()
        || action.parameters["apiKey"].isNullOrBlank()) return null
    val continueUrl = action.parameters["continueUrl"]?.let(::Url)
    if (continueUrl != null && (!trusted(continueUrl)
            || continueUrl.encodedPath != "/campus-email-verification")) return null
    if (!landing && continueUrl == null) return null
    val requestId = continueUrl?.parameters?.get("requestId") ?: if (landing) action.parameters["requestId"] else null
    if (requestId == null) return null
    if (!Regex("^[a-f0-9]{64}$").matches(requestId)) return null
    if (action.parameters["requestId"]?.let { it != requestId } == true) return null
    val firebaseLink = if (landing) URLBuilder(action).apply {
        host = CAMPUS_EMAIL_LINK_HOST
        encodedPath = "/__/auth/action"
        parameters.remove("requestId")
        parameters["continueUrl"] = continueUrl?.toString()
            ?: "https://$CAMPUS_EMAIL_LINK_HOST/campus-email-verification?requestId=$requestId"
    }.buildString() else action.toString()
    CampusEmailLink(requestId, firebaseLink)
}.getOrNull()

object CampusEmailVerificationLinks {
    private val incoming = MutableStateFlow<CampusEmailLink?>(null)
    val pending = incoming.asStateFlow()
    fun receive(url: String): Boolean {
        val link = parseCampusEmailLink(url) ?: return false
        incoming.value = link
        return true
    }
    fun clear(link: CampusEmailLink) {
        if (incoming.value == link) incoming.value = null
    }
}

expect suspend fun sendCampusEmailLink(email: String, continueUrl: String)
expect suspend fun campusEmailTokenForLink(email: String, link: String): String
expect fun signOutCampusEmailAuth()
expect fun loadPendingCampusEmailVerification(): PendingCampusEmailVerification?
expect fun savePendingCampusEmailVerification(value: PendingCampusEmailVerification?)

sealed interface CampusEmailRequestResult {
    data class Ready(val pending: PendingCampusEmailVerification, val resendAfterSeconds: Int) : CampusEmailRequestResult
    data class AlreadyVerified(val email: String) : CampusEmailRequestResult
}

interface CampusEmailCodeSource {
    suspend fun requestCode(email: String): EduCodeRequestResult
    suspend fun confirmCode(code: String): EduCodeConfirmResult
    suspend fun verifiedEmail(): String?
}

class CampusEmailVerificationRepository(private val auth: AuthRepository) : CampusEmailCodeSource {
    override suspend fun requestCode(email: String): EduCodeRequestResult {
        require(isCampusStudentEmail(email)) { "CAMPUS_EMAIL_INVALID" }
        val uid = auth.currentUser?.uid ?: error("AUTHENTICATION_REQUIRED")
        val response = callV2Function("requestCampusEmailCode", buildJsonObject { put("email", email.trim()) })
        check(auth.currentUser?.uid == uid) { "CAMPUS_EMAIL_ACCOUNT_MISMATCH" }
        val sentTo = response["email"]?.jsonPrimitive?.content ?: error("EDU_EMAIL_SEND_FAILED")
        return when (response["outcome"]?.jsonPrimitive?.content) {
            "already_verified" -> EduCodeRequestResult.AlreadyVerified(sentTo)
            "sent" -> EduCodeRequestResult.Sent(sentTo, response["resendAfterSeconds"]?.jsonPrimitive?.int ?: 60)
            else -> error("EDU_EMAIL_SEND_FAILED")
        }
    }

    override suspend fun confirmCode(code: String): EduCodeConfirmResult {
        val uid = auth.currentUser?.uid ?: error("AUTHENTICATION_REQUIRED")
        val response = callV2Function("confirmCampusEmailCode", buildJsonObject { put("code", code) })
        check(auth.currentUser?.uid == uid) { "CAMPUS_EMAIL_ACCOUNT_MISMATCH" }
        return when (response["outcome"]?.jsonPrimitive?.content) {
            "verified" -> EduCodeConfirmResult.Verified(response["email"]?.jsonPrimitive?.content ?: error("CAMPUS_EMAIL_PROOF_INVALID"))
            "invalid_code" -> EduCodeConfirmResult.InvalidCode(response["attemptsLeft"]?.jsonPrimitive?.int ?: 0)
            "expired" -> EduCodeConfirmResult.Expired
            "too_many_attempts" -> EduCodeConfirmResult.TooManyAttempts
            else -> error("CAMPUS_EMAIL_PROOF_INVALID")
        }
    }

    fun pending(): PendingCampusEmailVerification? {
        val value = loadPendingCampusEmailVerification() ?: return null
        return value.takeIf { it.uid == auth.currentUser?.uid && it.expiresAtMillis > Clock.System.now().toEpochMilliseconds() }
    }

    suspend fun request(email: String): CampusEmailRequestResult {
        require(isCampusStudentEmail(email)) { "CAMPUS_EMAIL_INVALID" }
        val uid = auth.currentUser?.uid ?: error("AUTHENTICATION_REQUIRED")
        val response = callV2Function("beginCampusEmailVerification", buildJsonObject { put("email", email.trim()) })
        val normalizedEmail = response["email"]?.jsonPrimitive?.content ?: error("CAMPUS_EMAIL_SEND_FAILED")
        if (response["outcome"]?.jsonPrimitive?.content == "already_verified") {
            savePendingCampusEmailVerification(null)
            return CampusEmailRequestResult.AlreadyVerified(normalizedEmail)
        }
        val pending = PendingCampusEmailVerification(
            uid, normalizedEmail,
            response["requestId"]?.jsonPrimitive?.content ?: error("CAMPUS_EMAIL_SEND_FAILED"),
            response["expiresAtMillis"]?.jsonPrimitive?.long ?: error("CAMPUS_EMAIL_SEND_FAILED")
        )
        check(auth.currentUser?.uid == uid) { "CAMPUS_EMAIL_ACCOUNT_MISMATCH" }
        savePendingCampusEmailVerification(pending)
        signOutCampusEmailAuth()
        sendCampusEmailLink(normalizedEmail, response["continueUrl"]?.jsonPrimitive?.content ?: error("CAMPUS_EMAIL_SEND_FAILED"))
        return CampusEmailRequestResult.Ready(pending, response["resendAfterSeconds"]?.jsonPrimitive?.int ?: 300)
    }

    suspend fun complete(link: CampusEmailLink): String {
        val pending = loadPendingCampusEmailVerification() ?: error("CAMPUS_EMAIL_NO_LOCAL_REQUEST")
        check(pending.uid == auth.currentUser?.uid) { "CAMPUS_EMAIL_ACCOUNT_MISMATCH" }
        check(pending.requestId == link.requestId) { "CAMPUS_EMAIL_REQUEST_MISMATCH" }
        check(pending.expiresAtMillis > Clock.System.now().toEpochMilliseconds()) { "CAMPUS_EMAIL_LINK_EXPIRED" }
        val token = campusEmailTokenForLink(pending.email, link.firebaseLink)
        check(pending.uid == auth.currentUser?.uid) { "CAMPUS_EMAIL_ACCOUNT_MISMATCH" }
        val response = callV2Function("completeCampusEmailVerification", buildJsonObject {
            put("requestId", pending.requestId)
            put("universityIdToken", token)
        })
        check(response["outcome"]?.jsonPrimitive?.content == "verified") { "CAMPUS_EMAIL_PROOF_INVALID" }
        savePendingCampusEmailVerification(null)
        signOutCampusEmailAuth()
        return response["email"]?.jsonPrimitive?.content ?: pending.email
    }

    override suspend fun verifiedEmail(): String? {
        val uid = auth.currentUser?.uid ?: return null
        val response = callV2Function("getCampusEmailVerificationStatus", buildJsonObject { })
        check(auth.currentUser?.uid == uid) { "CAMPUS_EMAIL_ACCOUNT_MISMATCH" }
        if (response["verified"]?.jsonPrimitive?.booleanOrNull != true) return null
        val email = response["email"]?.jsonPrimitive?.content ?: return null
        check(isCampusStudentEmail(email)) { "CAMPUS_EMAIL_PROOF_INVALID" }
        forgetPending()
        return email
    }

    fun forgetPending() {
        savePendingCampusEmailVerification(null)
        signOutCampusEmailAuth()
    }
}

// The iOS application installs its native Firebase Auth implementation here.
interface CampusEmailAuthCallback { fun complete(token: String?, error: String?) }
interface CampusEmailAuthLauncher {
    fun send(email: String, continueUrl: String, completion: CampusEmailAuthCallback)
    fun verify(email: String, link: String, completion: CampusEmailAuthCallback)
    fun signOut()
}
object CampusEmailAuthBridge { var launcher: CampusEmailAuthLauncher? = null }
