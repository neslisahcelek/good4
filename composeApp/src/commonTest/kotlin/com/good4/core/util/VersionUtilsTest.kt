package com.good4.core.util

import com.good4.config.domain.UpdateNotice
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionUtilsTest {

    @Test
    fun testSemanticVersionComparison() {
        // Strictly older versions
        assertTrue(isSemanticVersionOlder("1.1.4", "1.1.5"))
        assertTrue(isSemanticVersionOlder("1.1.4", "1.2.0"))
        assertTrue(isSemanticVersionOlder("1.0.9", "1.1.0"))
        assertTrue(isSemanticVersionOlder("0.9.9", "1.0.0"))
        assertTrue(isSemanticVersionOlder("1.1", "1.1.1"))

        // Equal versions
        assertFalse(isSemanticVersionOlder("1.1.5", "1.1.5"))
        assertFalse(isSemanticVersionOlder("1.0.0", "1.0.0"))
        assertFalse(isSemanticVersionOlder("2.0.0", "2.0"))

        // Newer versions
        assertFalse(isSemanticVersionOlder("1.1.6", "1.1.5"))
        assertFalse(isSemanticVersionOlder("1.2.0", "1.1.5"))
        assertFalse(isSemanticVersionOlder("2.0.0", "1.9.9"))

        // Invalid formats fail gracefully to false
        assertFalse(isSemanticVersionOlder("invalid", "1.1.5"))
        assertFalse(isSemanticVersionOlder("1.1.4", "invalid"))
    }

    @Test
    fun testForceUpdateDisabled() {
        val notice = UpdateNotice(
            forceUpdate = false,
            minVersionCodeAndroid = 999,
            minVersionIos = "9.9.9"
        )

        val androidVersion = AppVersionInfo(PlatformType.ANDROID, versionName = "1.1.4", versionCode = 17)
        val iosVersion = AppVersionInfo(PlatformType.IOS, versionName = "1.1.4", versionCode = 17)

        assertFalse(notice.isForceUpdateRequired(androidVersion))
        assertFalse(notice.isForceUpdateRequired(iosVersion))
    }

    @Test
    fun testAndroidForceUpdateLogic() {
        val activeNotice = UpdateNotice(
            forceUpdate = true,
            minVersionCodeAndroid = 18
        )

        val olderAndroid = AppVersionInfo(PlatformType.ANDROID, versionName = "1.1.4", versionCode = 17)
        val sameAndroid = AppVersionInfo(PlatformType.ANDROID, versionName = "1.1.5", versionCode = 18)
        val newerAndroid = AppVersionInfo(PlatformType.ANDROID, versionName = "1.1.6", versionCode = 19)

        assertTrue(activeNotice.isForceUpdateRequired(olderAndroid))
        assertFalse(activeNotice.isForceUpdateRequired(sameAndroid))
        assertFalse(activeNotice.isForceUpdateRequired(newerAndroid))

        // Null minVersionCodeAndroid should never block
        val unconfiguredNotice = UpdateNotice(forceUpdate = true, minVersionCodeAndroid = null)
        assertFalse(unconfiguredNotice.isForceUpdateRequired(olderAndroid))
    }

    @Test
    fun testIosForceUpdateLogic() {
        val activeNotice = UpdateNotice(
            forceUpdate = true,
            minVersionIos = "1.1.5"
        )

        val olderIos = AppVersionInfo(PlatformType.IOS, versionName = "1.1.4", versionCode = 17)
        val sameIos = AppVersionInfo(PlatformType.IOS, versionName = "1.1.5", versionCode = 18)
        val newerIos = AppVersionInfo(PlatformType.IOS, versionName = "1.2.0", versionCode = 19)

        assertTrue(activeNotice.isForceUpdateRequired(olderIos))
        assertFalse(activeNotice.isForceUpdateRequired(sameIos))
        assertFalse(activeNotice.isForceUpdateRequired(newerIos))

        // Null or blank minVersionIos should never block
        val nullIosNotice = UpdateNotice(forceUpdate = true, minVersionIos = null)
        assertFalse(nullIosNotice.isForceUpdateRequired(olderIos))

        val blankIosNotice = UpdateNotice(forceUpdate = true, minVersionIos = "   ")
        assertFalse(blankIosNotice.isForceUpdateRequired(olderIos))
    }
}
