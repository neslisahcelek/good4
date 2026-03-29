package com.good4.business.presentation.dashboard

import com.good4.order.domain.Order

data class BusinessDashboardState(
    val isLoading: Boolean = true,
    val businessName: String = "",
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val totalProducts: Int = 0,
    val recentCodes: List<RecentCodeUiModel> = emptyList(),
    val supporterPendingCount: Int = 0,
    val supporterConfirmedCount: Int = 0,
    val recentOrders: List<RecentOrderUiModel> = emptyList(),
    val orderDetailSheetVisible: Boolean = false,
    val orderDetailLoading: Boolean = false,
    val isCancellingOrderDetail: Boolean = false,
    val orderDetail: Order? = null,
    val errorMessage: String? = null
)
