package com.good4.core.util

import com.good4.BuildConfig

actual object AppEnvironment {
    @Suppress("KotlinConstantConditions")
    actual val isEmailVerificationRequired: Boolean
        get() = BuildConfig.EMAIL_VERIFICATION_REQUIRED

    actual val isDebug: Boolean
        get() = BuildConfig.DEBUG
}
