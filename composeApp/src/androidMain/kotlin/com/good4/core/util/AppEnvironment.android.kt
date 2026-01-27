package com.good4.core.util

import com.good4.BuildConfig

actual object AppEnvironment {
    actual val isEmailVerificationRequired: Boolean
        get() = BuildConfig.FLAVOR != "staging"
}
