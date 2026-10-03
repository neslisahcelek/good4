package com.good4.campuscloset

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.koin.core.context.GlobalContext

private const val APP_NAME = "good4-campus-email-verification"
private const val PENDING_KEY = "campus_email_verification"

/** This Auth object is never passed to the main application's repositories or auth-state listener. */
private val campusAuth: FirebaseAuth by lazy {
    val primary = FirebaseApp.getInstance()
    val app = FirebaseApp.getApps(primary.applicationContext).firstOrNull { it.name == APP_NAME }
        ?: FirebaseApp.initializeApp(primary.applicationContext, primary.options, APP_NAME)
    FirebaseAuth.getInstance(app)
}

actual suspend fun sendCampusEmailLink(email: String, continueUrl: String) {
    try {
        val settings = ActionCodeSettings.newBuilder()
            .setUrl(continueUrl)
            .setHandleCodeInApp(true)
            // No mobile package: open the one-click web confirmation first.
            // Firebase selects its default Hosting domain.
            .build()
        campusAuth.setLanguageCode("tr")
        campusAuth.sendSignInLinkToEmail(email, settings).await()
    } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
    } catch (_: Exception) {
        error("CAMPUS_EMAIL_SEND_FAILED")
    }
}

actual suspend fun campusEmailTokenForLink(email: String, link: String): String {
    check(campusAuth.isSignInWithEmailLink(link)) { "CAMPUS_EMAIL_SIGN_IN_FAILED" }
    val user = try {
        campusAuth.signInWithEmailLink(email, link).await().user
    } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
    } catch (_: Exception) {
        // Keep a completed university session available if only the backend confirmation failed.
        campusAuth.currentUser?.takeIf { it.email.equals(email, ignoreCase = true) && it.isEmailVerified }
    } ?: error("CAMPUS_EMAIL_SIGN_IN_FAILED")
    return user.getIdToken(true).await().token ?: error("CAMPUS_EMAIL_SIGN_IN_FAILED")
}

actual fun signOutCampusEmailAuth() { campusAuth.signOut() }

private fun pendingPreferences() = GlobalContext.get().get<Context>()
    .getSharedPreferences("good4_campus_verification", Context.MODE_PRIVATE)

actual fun loadPendingCampusEmailVerification(): PendingCampusEmailVerification? = runCatching {
    pendingPreferences().getString(PENDING_KEY, null)?.let { Json.decodeFromString<PendingCampusEmailVerification>(it) }
}.getOrNull()

actual fun savePendingCampusEmailVerification(value: PendingCampusEmailVerification?) {
    pendingPreferences().edit().apply {
        if (value == null) remove(PENDING_KEY) else putString(PENDING_KEY, Json.encodeToString(value))
    }.apply()
}
