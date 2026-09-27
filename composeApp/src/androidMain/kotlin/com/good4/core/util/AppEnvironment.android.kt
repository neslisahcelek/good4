package com.good4.core.util

import com.good4.BuildConfig

actual object AppEnvironment {
    @Suppress("KotlinConstantConditions")
    actual val isEmailVerificationRequired: Boolean
        get() = BuildConfig.EMAIL_VERIFICATION_REQUIRED

    actual val isDebug: Boolean
        get() = BuildConfig.DEBUG

    actual val firebaseBackend: FirebaseBackend
        get() = when (BuildConfig.FIREBASE_ENVIRONMENT) {
            "good4tr-v2" -> FirebaseBackend.V2
            "good4tr" -> FirebaseBackend.PRODUCTION
            else -> FirebaseBackend.LEGACY_TEST
        }
}
