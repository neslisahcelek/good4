package com.good4.core.util

import android.util.Log
import com.good4.BuildConfig

actual object Logger {
    actual fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
}
