package com.good4.business.domain

data class Business(
    val id: String,
    val name: String,
    val ownerId: String,
    val phone: String,
    val address: String,
    val city: String,
    val district: String
) {
    val fullAddress: String
        get() = "$address, $district/$city"
}

