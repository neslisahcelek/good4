package com.good4.admin.presentation.dashboard

data class AdminDashboardState(
    val isLoading: Boolean = true,
    val totalProducts: Int = 0,
    val totalBusinesses: Int = 0,
    val totalCampaigns: Int = 0,
    val totalUsers: Int = 0,
    val errorMessage: String? = null
)

