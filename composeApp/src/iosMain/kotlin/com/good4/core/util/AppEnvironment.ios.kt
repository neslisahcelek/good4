package com.good4.core.util

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual object AppEnvironment {
    actual val isEmailVerificationRequired: Boolean
        get() = firebaseBackend == FirebaseBackend.V2

    actual val isDebug: Boolean
        get() = Platform.isDebugBinary

    actual val firebaseBackend: FirebaseBackend
        get() = when (NSBundle.mainBundle.bundleIdentifier) {
            "com.good4.iosApp" -> FirebaseBackend.V2
            "com.good4.iosApp.test" -> FirebaseBackend.LEGACY_TEST
            else -> error("Unsupported application bundle: ${NSBundle.mainBundle.bundleIdentifier}")
        }
}
