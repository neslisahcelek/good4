package com.good4.core.util

enum class FirebaseBackend {
    /** Kept for legacy adapters; supported production and test apps both use V2. */
    LEGACY_TEST,
    /** Shared data model and callable contract for production and test. */
    V2
}

expect object AppEnvironment {
    val isEmailVerificationRequired: Boolean
    val isDebug: Boolean
    val useFirebaseEmulators: Boolean
    val firebaseEmulatorHost: String
    val firebaseProjectId: String
    val firebaseBackend: FirebaseBackend
}
