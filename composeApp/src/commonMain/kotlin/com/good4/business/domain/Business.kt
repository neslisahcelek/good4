package com.good4.business.domain

data class Business(
    val id: String,
    val name: String,
    val ownerId: String,
    val phone: String,
    val address: String,
    val addressUrl: String = "",
    val city: String,
    val district: String
)
