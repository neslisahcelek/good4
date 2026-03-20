package com.good4.core.util

private val directUrlPrefixes = listOf(
    "http://",
    "https://",
    "maps://",
    "comgooglemaps://",
    "geo:"
)

fun isDirectMapUrl(value: String): Boolean {
    val normalized = value.trim()
    return directUrlPrefixes.any { prefix ->
        normalized.startsWith(prefix, ignoreCase = true)
    }
}

fun toDisplayAddress(rawAddress: String, mapsFallbackLabel: String): String {
    val normalized = rawAddress.trim()
    if (normalized.isBlank()) return ""
    if (!isDirectMapUrl(normalized)) return normalized

    return extractAddressFromMapUrl(normalized)
        ?.takeIf { it.isNotBlank() }
        ?: mapsFallbackLabel
}

private fun extractAddressFromMapUrl(url: String): String? {
    val queryPart = url.substringAfter('?', "")
    if (queryPart.isNotBlank()) {
        parseQueryParams(queryPart).forEach { (key, value) ->
            if (key.equals("q", ignoreCase = true)
                || key.equals("query", ignoreCase = true)
                || key.equals("daddr", ignoreCase = true)
                || key.equals("destination", ignoreCase = true)
            ) {
                val decoded = decodeUrlPart(value)
                if (decoded.isNotBlank()) return decoded
            }
        }
    }

    val placeSegment = url.substringAfter("/place/", "")
        .substringBefore('/')
        .takeIf { it.isNotBlank() }
    if (placeSegment != null) {
        val decoded = decodeUrlPart(placeSegment)
        if (decoded.isNotBlank()) return decoded
    }

    return null
}

private fun parseQueryParams(query: String): List<Pair<String, String>> {
    return query
        .split('&')
        .mapNotNull { part ->
            if (part.isBlank()) {
                null
            } else {
                val key = part.substringBefore('=', "")
                val value = part.substringAfter('=', "")
                key to value
            }
        }
}

private fun decodeUrlPart(value: String): String {
    val result = StringBuilder(value.length)
    val byteBuffer = mutableListOf<Byte>()
    var index = 0

    fun flushBytes() {
        if (byteBuffer.isEmpty()) return
        val byteArray = ByteArray(byteBuffer.size) { position -> byteBuffer[position] }
        result.append(byteArray.decodeToString())
        byteBuffer.clear()
    }

    while (index < value.length) {
        val char = value[index]
        if (char == '%' && index + 2 < value.length) {
            val hex = value.substring(index + 1, index + 3)
            val decoded = hex.toIntOrNull(16)
            if (decoded != null) {
                byteBuffer.add(decoded.toByte())
                index += 3
                continue
            }
        }

        flushBytes()
        result.append(if (char == '+') ' ' else char)
        index += 1
    }

    flushBytes()
    return result.toString().trim()
}
