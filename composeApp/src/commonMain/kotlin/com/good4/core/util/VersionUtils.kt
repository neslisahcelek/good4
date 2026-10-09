package com.good4.core.util

/**
 * Returns true if [current] semantic version is strictly older than [minRequired].
 * e.g.
 * - "1.1.4" older than "1.1.5" -> true
 * - "1.1.3" older than "1.2.0" -> true
 * - "1.1.5" older than "1.1.5" -> false
 * - "1.2.0" older than "1.1.5" -> false
 *
 * If parsing fails on invalid formats, returns false for safety so clients are not erroneously blocked.
 */
fun isSemanticVersionOlder(current: String, minRequired: String): Boolean {
    val currentParts = current.split('.').map { it.trim().toIntOrNull() ?: return false }
    val minParts = minRequired.split('.').map { it.trim().toIntOrNull() ?: return false }
    for (index in 0 until maxOf(currentParts.size, minParts.size)) {
        val currentPart = currentParts.getOrElse(index) { 0 }
        val minPart = minParts.getOrElse(index) { 0 }
        if (currentPart < minPart) return true
        if (currentPart > minPart) return false
    }
    return false
}
