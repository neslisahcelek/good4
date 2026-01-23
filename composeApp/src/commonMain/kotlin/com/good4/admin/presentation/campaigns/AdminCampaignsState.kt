package com.good4.admin.presentation.campaigns

import com.good4.campaign.domain.Campaign

data class AdminCampaignsState(
    val isLoading: Boolean = true,
    val campaigns: List<Campaign> = emptyList(),
    val campaignImageUrl: String = "",
    val isAddLoading: Boolean = false,
    val addSuccess: Boolean = false,
    val errorMessage: String? = null
)

