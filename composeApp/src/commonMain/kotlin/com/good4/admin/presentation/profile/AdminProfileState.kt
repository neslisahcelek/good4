package com.good4.admin.presentation.profile

data class AdminProfileState(
    val isLoading: Boolean = true,
    val adminName: String = "",
    val adminEmail: String = "",
    val errorMessage: String? = null
)

