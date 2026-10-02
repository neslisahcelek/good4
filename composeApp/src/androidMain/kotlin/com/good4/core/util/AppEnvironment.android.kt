package com.good4.core.util

import com.good4.BuildConfig

actual object AppEnvironment {
    @Suppress("KotlinConstantConditions")
    actual val isEmailVerificationRequired: Boolean
        get() = BuildConfig.EMAIL_VERIFICATION_REQUIRED

    actual val isDebug: Boolean
        get() = BuildConfig.DEBUG

    actual val useFirebaseEmulators: Boolean get() = BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS
    actual val firebaseEmulatorHost: String get() = BuildConfig.FIREBASE_EMULATOR_HOST
    actual val firebaseProjectId: String
        get() = if (useFirebaseEmulators) "demo-good4-v2" else when (BuildConfig.FIREBASE_ENVIRONMENT) {
            "good4tr-v2", "good4tr-test" -> BuildConfig.FIREBASE_ENVIRONMENT
            else -> error("Unsupported Firebase environment: ${BuildConfig.FIREBASE_ENVIRONMENT}")
        }

    actual val firebaseBackend: FirebaseBackend
        get() = when (firebaseProjectId) {
            "good4tr-v2", "good4tr-test", "demo-good4-v2" -> FirebaseBackend.V2
            else -> error("Unsupported Firebase project: $firebaseProjectId")
        }
}
