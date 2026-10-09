package com.good4.config.domain

import com.good4.core.util.AppVersionInfo
import com.good4.core.util.PlatformType
import com.good4.core.util.isSemanticVersionOlder

data class UpdateNotice(
    val title: String = "",
    val message: String = "",
    val enabled: Boolean = false,
    val minVersionCodeAndroid: Int? = null,
    val minVersionIos: String? = null,
    val forceUpdate: Boolean = false
) {
    /**
     * Determines whether the current client version is blocked and must be force updated.
     * Only triggers when [forceUpdate] is true and a valid minimum version for the current platform is specified.
     */
    fun isForceUpdateRequired(currentVersion: AppVersionInfo): Boolean {
        if (!forceUpdate) return false

        return when (currentVersion.platform) {
            PlatformType.ANDROID -> {
                val minCode = minVersionCodeAndroid ?: return false
                currentVersion.versionCode < minCode
            }
            PlatformType.IOS -> {
                val minIos = minVersionIos?.trim()?.takeIf { it.isNotBlank() } ?: return false
                isSemanticVersionOlder(currentVersion.versionName, minIos)
            }
        }
    }
}
