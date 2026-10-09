package com.good4.core.util

enum class PlatformType {
    ANDROID,
    IOS
}

data class AppVersionInfo(
    val platform: PlatformType,
    val versionName: String,
    val versionCode: Int
)

expect fun getAppVersionInfo(): AppVersionInfo
