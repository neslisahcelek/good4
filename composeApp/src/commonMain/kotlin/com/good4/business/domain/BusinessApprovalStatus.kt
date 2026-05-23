package com.good4.business.domain

enum class BusinessApprovalStatus(val value: String) {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    companion object {
        fun fromValue(value: String?): BusinessApprovalStatus {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}
