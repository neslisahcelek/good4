package com.good4.core.util

import platform.Foundation.NSProcessInfo

actual object AppEnvironment {
    actual val isEmailVerificationRequired: Boolean
        get() = !isStaging()

    private fun isStaging(): Boolean {
        val env = NSProcessInfo.processInfo.environment["GOOD4_ENV"] as? String
        return env?.lowercase() == "staging"
    }
}
