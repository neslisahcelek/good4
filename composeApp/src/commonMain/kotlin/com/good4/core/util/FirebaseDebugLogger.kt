package com.good4.core.util

object FirebaseDebugLogger {
    private const val tag = "Good4Firebase"
    private const val maxMessageLength = 1000
    private const val logRequestsAndSuccesses = false

    fun request(operation: String, path: String, detail: String? = null) {
        if (!logRequestsAndSuccesses) return
        Logger.d(tag, buildMessage(stage = "REQUEST", operation = operation, path = path, detail = detail))
    }

    fun success(operation: String, path: String, detail: String? = null) {
        if (!logRequestsAndSuccesses) return
        Logger.d(tag, buildMessage(stage = "SUCCESS", operation = operation, path = path, detail = detail))
    }

    fun error(operation: String, path: String, throwable: Throwable? = null, detail: String? = null) {
        Logger.e(
            tag,
            buildMessage(stage = "ERROR", operation = operation, path = path, detail = detail),
            throwable
        )
    }

    private fun buildMessage(stage: String, operation: String, path: String, detail: String?): String {
        val rawMessage = buildString {
            append(stage)
            append(" | op=")
            append(operation)
            append(" | path=")
            append(path)
            if (!detail.isNullOrBlank()) {
                append(" | ")
                append(detail)
            }
        }
        return if (rawMessage.length <= maxMessageLength) {
            rawMessage
        } else {
            rawMessage.take(maxMessageLength) + "...(truncated)"
        }
    }
}
