package com.good4.core.util

actual object Logger {
    actual fun d(tag: String, message: String) {
        println("$tag: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        val details = throwable?.message?.let { " - $it" }.orEmpty()
        println("$tag: $message$details")
    }
}
