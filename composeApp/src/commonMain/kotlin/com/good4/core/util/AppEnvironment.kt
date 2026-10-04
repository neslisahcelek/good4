package com.good4.core.util

enum class FirebaseBackend {
    /** The separate good4tr-test project, which still uses the legacy data model. */
    LEGACY_TEST,
    /** The current production project good4tr-v2 and its data model, not a build flavor. */
    V2
}

expect object AppEnvironment {
    val isEmailVerificationRequired: Boolean
    val isDebug: Boolean
    val firebaseBackend: FirebaseBackend
}
