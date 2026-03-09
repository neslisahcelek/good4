package com.good4.user.domain

enum class UserRole(val value: String) {
    ADMIN("admin"),
    BUSINESS("business"),
    STUDENT("student"),
    SUPPORTER("supporter");

    companion object {
        fun fromValue(value: String?): UserRole {
            return entries.find { it.value == value } ?: STUDENT
        }
    }
}

