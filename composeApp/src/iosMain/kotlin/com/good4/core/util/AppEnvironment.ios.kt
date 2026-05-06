package com.good4.core.util

import platform.Foundation.NSProcessInfo
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual object AppEnvironment {
    actual val isEmailVerificationRequired: Boolean
        get() = !isStaging()

    actual val isDebug: Boolean
        get() = Platform.isDebugBinary

    private fun isStaging(): Boolean {
        val env = NSProcessInfo.processInfo.environment["GOOD4_ENV"] as? String
        return env?.lowercase() == "staging"
    }
}
