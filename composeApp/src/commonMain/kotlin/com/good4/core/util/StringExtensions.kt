package com.good4.core.util

import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_email_invalid_format
import good4.composeapp.generated.resources.error_email_must_be_edu_tr
import good4.composeapp.generated.resources.error_email_required
import org.jetbrains.compose.resources.StringResource

private const val EMAIL_REGEX_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"

fun String.validateEmail(): StringResource? {
    val trimmedEmail = this.trim()
    
    if (trimmedEmail.isBlank()) {
        return Res.string.error_email_required
    }

    if (!trimmedEmail.matches(EMAIL_REGEX_PATTERN.toRegex())) {
        return Res.string.error_email_invalid_format
    }

    return null
}

fun String.validateStudentEmail(): StringResource? {
    val emailValidation = this.validateEmail()
    if (emailValidation != null) {
        return emailValidation
    }

    val trimmedEmail = this.trim()
    if (!trimmedEmail.endsWith("@edu.tr", ignoreCase = true)) {
        return Res.string.error_email_must_be_edu_tr
    }

    return null
}
