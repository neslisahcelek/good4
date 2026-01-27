package com.good4.code.domain

enum class CodeStatus(val value: String) {
    PENDING("pending"),
    USED("used"),
    EXPIRED("expired"),
    CANCELLED("cancelled");

    companion object {
        fun fromValue(value: String?): CodeStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}
