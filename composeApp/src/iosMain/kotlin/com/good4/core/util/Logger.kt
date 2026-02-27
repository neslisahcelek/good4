package com.good4.core.util

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import platform.Foundation.NSLog

@OptIn(ExperimentalNativeApi::class)
actual object Logger {
    actual fun d(tag: String, message: String) {
        if (Platform.isDebugBinary) {
            NSLog("D/$tag: $message")
        }
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        if (Platform.isDebugBinary) {
            val details = throwable?.message?.let { " | cause=$it" }.orEmpty()
            NSLog("E/$tag: $message$details")
        }
    }
}
