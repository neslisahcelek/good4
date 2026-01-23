package com.good4.business.presentation.dashboard

data class BusinessDashboardState(
    val isLoading: Boolean = true,
    val businessName: String = "",
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val totalProducts: Int = 0,
    val recentCodes: List<RecentCodeUiModel> = emptyList(),
    val errorMessage: String? = null
)

