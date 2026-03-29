package com.good4.core.util

import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_email_invalid_format
import good4.composeapp.generated.resources.error_email_must_be_edu_tr
import good4.composeapp.generated.resources.error_email_required
import org.jetbrains.compose.resources.StringResource

private const val EMAIL_REGEX_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"

fun String.normalizeForEmail(): String {
    return this
        .replace("\u00A0", "") // non-breaking space
        .replace(Regex("[\u200B-\u200D\u2060\uFEFF]"), "") // zero-width chars
        .trim()
}

fun String.validateEmail(): StringResource? {
    val normalizedEmail = this.normalizeForEmail()

    if (normalizedEmail.isBlank()) {
        return Res.string.error_email_required
    }

    if (!normalizedEmail.matches(EMAIL_REGEX_PATTERN.toRegex())) {
        return Res.string.error_email_invalid_format
    }

    return null
}

fun String.validateStudentEmail(): StringResource? {
    val emailValidation = this.validateEmail()
    if (emailValidation != null) {
        return emailValidation
    }

    val normalizedEmail = this.normalizeForEmail()
    if (!normalizedEmail.endsWith("edu.tr", ignoreCase = true)) {
        return Res.string.error_email_must_be_edu_tr
    }

    return null
}

fun String.toInitials(): String {
    val parts = trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    if (parts.isEmpty()) return ""

    return when (parts.size) {
        1 -> parts.first().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
        else -> {
            val firstInitial = parts.first().firstOrNull()?.uppercaseChar()
            val lastInitial = parts.last().firstOrNull()?.uppercaseChar()

            buildString {
                if (firstInitial != null) append(firstInitial)
                if (lastInitial != null) append(lastInitial)
            }
        }
    }
}
