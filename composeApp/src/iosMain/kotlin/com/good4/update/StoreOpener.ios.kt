package com.good4.update

import config.StoreLinks
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openStoreAppPage() {
    val configured = StoreLinks.APP_STORE_URL.trim()
    val url = NSURL.URLWithString(configured) ?: return
    UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
}
