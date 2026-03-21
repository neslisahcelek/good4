package com.good4.business.presentation.profile

data class BusinessProfileState(
    val isLoading: Boolean = true,
    val businessName: String = "",
    val ownerName: String = "",
    val address: String = "",
    val addressUrl: String = "",
    val phone: String = "",
    val errorMessage: String? = null
)
