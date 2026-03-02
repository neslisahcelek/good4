package com.good4.admin.presentation.dashboard

data class ActiveProductStock(
    val id: String,
    val name: String,
    val stock: Int
)

data class AdminDashboardState(
    val isLoading: Boolean = true,
    val totalProducts: Int = 0,
    val totalBusinesses: Int = 0,
    val totalCampaigns: Int = 0,
    val totalUsers: Int = 0,
    val activeProducts: List<ActiveProductStock> = emptyList(),
    val errorMessage: String? = null
)

