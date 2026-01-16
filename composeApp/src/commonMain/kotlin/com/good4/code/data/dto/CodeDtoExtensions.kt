package com.good4.code.data.dto

import com.good4.code.domain.CodeStatus
import kotlinx.datetime.Clock

val CodeDto.statusEnum: CodeStatus
    get() = CodeStatus.fromValue(status)

fun CodeDto.isExpired(): Boolean {
    val expiryTime = this.expiresAt ?: return true
    return Clock.System.now() >= expiryTime
}

const val DEFAULT_EXPIRATION_MINUTES = 45L
