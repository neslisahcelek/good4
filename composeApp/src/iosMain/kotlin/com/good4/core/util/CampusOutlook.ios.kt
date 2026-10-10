package com.good4.core.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openCampusOutlook() {
    val app = UIApplication.sharedApplication
    val outlook = NSURL.URLWithString("ms-outlook://") ?: return
    val browser = NSURL.URLWithString("https://outlook.office.com/mail") ?: return
    // Try directly: custom schemes can be hidden from canOpenURL without plist queries.
    app.openURL(outlook, options = emptyMap<Any?, Any>()) { opened ->
        if (!opened) app.openURL(browser, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}
