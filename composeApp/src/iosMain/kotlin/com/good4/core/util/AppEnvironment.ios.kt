package com.good4.core.util

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual object AppEnvironment {
    actual val isEmailVerificationRequired: Boolean
        get() = firebaseBackend == FirebaseBackend.V2

    actual val isDebug: Boolean
        get() = Platform.isDebugBinary

    actual val useFirebaseEmulators: Boolean get() = isDebug && NSBundle.mainBundle.bundleIdentifier == "com.good4.iosApp.test"
    actual val firebaseEmulatorHost: String get() = "127.0.0.1"
    actual val firebaseProjectId: String
        get() = if (useFirebaseEmulators) "demo-good4-v2" else when (NSBundle.mainBundle.bundleIdentifier) {
            "com.good4.iosApp" -> "good4tr-v2"
            "com.good4.iosApp.test" -> "good4tr-test"
            else -> error("Unsupported application bundle: ${NSBundle.mainBundle.bundleIdentifier}")
        }

    actual val firebaseBackend: FirebaseBackend
        get() = when (firebaseProjectId) {
            "good4tr-v2", "good4tr-test", "demo-good4-v2" -> FirebaseBackend.V2
            else -> error("Unsupported Firebase project: $firebaseProjectId")
        }
}
