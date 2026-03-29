package com.good4.business.presentation.dashboard

import com.good4.order.domain.OrderStatus

data class RecentCodeUiModel(
    val id: String,
    val codeValue: String,
    val productName: String,
    val status: String
)

data class RecentOrderUiModel(
    val id: String,
    val productName: String,
    val code: String,
    val orderStatus: OrderStatus
)
