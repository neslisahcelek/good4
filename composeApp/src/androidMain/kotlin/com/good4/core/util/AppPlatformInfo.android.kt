package com.good4.core.util

import com.good4.BuildConfig

actual fun getAppVersionInfo(): AppVersionInfo = AppVersionInfo(
    platform = PlatformType.ANDROID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE
)
