package com.good4.order.domain

enum class OrderStatus(val value: String) {
    PENDING("pending"),
    CONFIRMED("confirmed"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    companion object {
        fun fromValue(value: String?): OrderStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}
