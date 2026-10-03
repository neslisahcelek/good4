package com.good4.campuscloset

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import platform.Foundation.NSUserDefaults
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val PENDING_KEY = "good4_campus_email_verification"

private suspend fun nativeCampusAuth(action: (CampusEmailAuthLauncher, CampusEmailAuthCallback) -> Unit): String? =
    suspendCancellableCoroutine { continuation ->
        val launcher = CampusEmailAuthBridge.launcher
        if (launcher == null) continuation.resumeWithException(IllegalStateException("CAMPUS_EMAIL_SEND_FAILED"))
        else action(launcher, object : CampusEmailAuthCallback {
            override fun complete(token: String?, error: String?) {
                if (!continuation.isActive) return
                if (error != null) continuation.resumeWithException(IllegalStateException(error))
                else continuation.resume(token)
            }
        })
    }

actual suspend fun sendCampusEmailLink(email: String, continueUrl: String) {
    nativeCampusAuth { launcher, callback -> launcher.send(email, continueUrl, callback) }
}

actual suspend fun campusEmailTokenForLink(email: String, link: String): String =
    nativeCampusAuth { launcher, callback -> launcher.verify(email, link, callback) }
        ?: error("CAMPUS_EMAIL_SIGN_IN_FAILED")

actual fun signOutCampusEmailAuth() { CampusEmailAuthBridge.launcher?.signOut() }

actual fun loadPendingCampusEmailVerification(): PendingCampusEmailVerification? = runCatching {
    NSUserDefaults.standardUserDefaults.stringForKey(PENDING_KEY)?.let {
        Json.decodeFromString<PendingCampusEmailVerification>(it)
    }
}.getOrNull()

actual fun savePendingCampusEmailVerification(value: PendingCampusEmailVerification?) {
    if (value == null) NSUserDefaults.standardUserDefaults.removeObjectForKey(PENDING_KEY)
    else NSUserDefaults.standardUserDefaults.setObject(Json.encodeToString(value), PENDING_KEY)
}
