package com.good4.core.util

import platform.Foundation.NSProcessInfo
import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual object AppEnvironment {
    actual val isEmailVerificationRequired: Boolean
        get() = !isStaging()

    actual val isDebug: Boolean
        get() = Platform.isDebugBinary

    actual val firebaseBackend: FirebaseBackend
        get() = when (NSBundle.mainBundle.bundleIdentifier) {
            "com.good4.iosApp.v2" -> FirebaseBackend.V2
            "com.good4.iosApp" -> FirebaseBackend.V2
            else -> FirebaseBackend.LEGACY_TEST
        }

    private fun isStaging(): Boolean {
        val env = NSProcessInfo.processInfo.environment["GOOD4_ENV"] as? String
        return env?.lowercase() == "staging" || firebaseBackend == FirebaseBackend.LEGACY_TEST
    }
}
