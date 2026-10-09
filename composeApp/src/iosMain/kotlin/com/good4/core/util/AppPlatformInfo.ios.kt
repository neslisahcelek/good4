package com.good4.core.util

import platform.Foundation.NSBundle

actual fun getAppVersionInfo(): AppVersionInfo {
    val versionName = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0.0"
    val buildNumberStr = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "1"
    val versionCode = buildNumberStr.toIntOrNull() ?: 1
    return AppVersionInfo(
        platform = PlatformType.IOS,
        versionName = versionName,
        versionCode = versionCode
    )
}
