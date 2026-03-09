package com.good4.supportactivity.domain

enum class SupportActivityStatus(val value: String) {
    ACTIVE("active"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    EXPIRED("expired");

    companion object {
        fun fromValue(value: String?): SupportActivityStatus {
            return entries.find { it.value == value } ?: ACTIVE
        }
    }
}
