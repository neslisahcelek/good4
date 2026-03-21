package com.good4.core.util

fun userFriendlyErrorMessage(raw: String?, fallback: String): String {
    if (raw.isNullOrBlank()) return fallback
    val lower = raw.lowercase()
    if ("requires an index" in lower) return fallback
    if ("firebase.google.com" in raw) return fallback
    if ("firestoreerrordomain" in lower) return fallback
    return raw
}
