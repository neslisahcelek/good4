package com.good4.eduverification

import com.good4.auth.data.repository.AuthRepository
import com.good4.core.network.callV2Function
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class EduStatus(
    val verified: Boolean,
    val email: String?,
    val campusAccessAllowed: Boolean = verified,
    val accessSource: String = if (verified) "edu_email" else "none",
    val testAvailable: Boolean = false,
    val testEmail: String? = null,
    val resendAfterSeconds: Int = 0
)

/** Invalidation is account-scoped; cached feature data is never an authorization source. */
object CampusAccessSession {
    data class Snapshot(val uid: String? = null, val revision: Long = 0, val status: EduStatus? = null)
    private val mutable = MutableStateFlow(Snapshot())
    val changes = mutable.asStateFlow()
    fun revision(uid: String?): Long = mutable.value.takeIf { it.uid == uid }?.revision ?: 0
    fun status(uid: String?): EduStatus? = mutable.value.takeIf { it.uid == uid }?.status
    fun changed(uid: String) { mutable.update { Snapshot(uid, it.revision + 1) } }
    fun received(uid: String, status: EduStatus) {
        mutable.update { old ->
            val previous = old.status
            val accessChanged = old.uid == uid && previous != null &&
                (previous.campusAccessAllowed != status.campusAccessAllowed || previous.accessSource != status.accessSource || previous.testAvailable != status.testAvailable)
            Snapshot(uid, old.revision + if (accessChanged) 1 else 0, status)
        }
    }
}

class CampusAccessRepository(private val auth: AuthRepository) {
    val currentUid: String? get() = auth.currentUser?.uid
    suspend fun status(): EduStatus {
        val uid = currentUid ?: return EduStatus(false, null)
        val revision = CampusAccessSession.revision(uid)
        val response = callV2Function("getCampusEmailVerificationStatus", JsonObject(emptyMap()))
        check(currentUid == uid && CampusAccessSession.revision(uid) == revision) { "CAMPUS_EMAIL_ACCOUNT_MISMATCH" }
        val verified = response["verified"]?.jsonPrimitive?.booleanOrNull == true
        return EduStatus(
            verified = verified, email = response["email"]?.jsonPrimitive?.content,
            campusAccessAllowed = response["campusAccessAllowed"]?.jsonPrimitive?.booleanOrNull ?: verified,
            accessSource = response["accessSource"]?.jsonPrimitive?.content ?: if (verified) "edu_email" else "none",
            testAvailable = response["testAvailable"]?.jsonPrimitive?.booleanOrNull == true,
            testEmail = response["testEmail"]?.jsonPrimitive?.content?.takeUnless { it == "null" },
            resendAfterSeconds = response["resendAfterSeconds"]?.jsonPrimitive?.intOrNull ?: 0
        ).also { CampusAccessSession.received(uid, it) }
    }
    suspend fun disconnectTest() {
        val uid = currentUid ?: error("AUTHENTICATION_REQUIRED")
        callV2Function("disconnectCampusTest", JsonObject(emptyMap()))
        check(currentUid == uid) { "CAMPUS_EMAIL_ACCOUNT_MISMATCH" }
        CampusAccessSession.changed(uid)
        com.good4.campuscloset.saveCampusClosetFeedCache(null)
        com.good4.campuscloset.savePendingCampusEmailVerification(null)
        com.good4.campuscloset.signOutCampusEmailAuth()
    }
}
