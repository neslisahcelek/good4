package com.good4.core.util

enum class FirebaseBackend {
    LEGACY_TEST,
    V2,
    PRODUCTION
}

expect object AppEnvironment {
    val isEmailVerificationRequired: Boolean
    val isDebug: Boolean
    val firebaseBackend: FirebaseBackend
}
