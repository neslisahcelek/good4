package com.good4.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import config.StoreLinks
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import kotlin.coroutines.resume
import kotlin.experimental.ExperimentalNativeApi

actual fun loadReviewHistory(): String? = NSUserDefaults.standardUserDefaults.stringForKey("store_review_history")
actual fun saveReviewHistory(value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, "store_review_history")
}
actual fun reviewAppVersion(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
@OptIn(ExperimentalNativeApi::class)
actual fun automaticReviewEnabled(): Boolean =
    Platform.isDebugBinary || NSBundle.mainBundle.bundleIdentifier == "com.good4.iosApp"

actual suspend fun openReviewStore(): StoreOpenResult {
    val configured = StoreLinks.APP_STORE_URL.trim()
    if (configured.isBlank()) return StoreOpenResult.NOT_CONFIGURED
    val url = NSURL.URLWithString(configured) ?: return StoreOpenResult.FAILED
    if (url.scheme != "https" || url.host != "apps.apple.com") return StoreOpenResult.FAILED
    val base = configured.substringBefore('#')
    val reviewUrl = NSURL.URLWithString(
        if (base.contains("action=write-review")) base
        else base + (if ('?' in base) "&" else "?") + "action=write-review"
    ) ?: return StoreOpenResult.FAILED
    return suspendCancellableCoroutine { continuation ->
        UIApplication.sharedApplication.openURL(reviewUrl, options = emptyMap<Any?, Any>()) { opened ->
            if (continuation.isActive) continuation.resume(if (opened) StoreOpenResult.OPENED else StoreOpenResult.FAILED)
        }
    }
}

@Composable
actual fun rememberStoreReviewLauncher(): StoreReviewLauncher = remember {
    object : StoreReviewLauncher {
        override fun requestReview(approve: () -> Boolean) {
            StoreReviewBridge.launcher?.launch(object : NativeReviewApproval {
                override fun approve(): Boolean = approve.invoke()
            })
        }
    }
}
