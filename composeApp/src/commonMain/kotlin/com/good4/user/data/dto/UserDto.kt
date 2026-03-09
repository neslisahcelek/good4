package com.good4.user.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("email")
    val email: String? = null,
    @SerialName("fullName")
    val fullName: String? = null,
    @SerialName("phoneNumber")
    val phoneNumber: String? = null,
    @SerialName("role")
    val role: String? = null,
    @SerialName("verified")
    val verified: Boolean? = null,
    @SerialName("university")
    val university: String? = null,
    @SerialName("major")
    val major: String? = null,
    @SerialName("educationLevel")
    val educationLevel: String? = null,
    @SerialName("credit")
    val credit: Int? = null,
    @SerialName("weeklyCreditOverride")
    val weeklyCreditOverride: Int? = null,
    @SerialName("lastCreditResetAt")
    val lastCreditResetAt: Long? = null,
    @SerialName("registrationDate")
    val registrationDate: Long? = null,
    @SerialName("createdAt")
    val createdAt: Long? = null,
    @SerialName("totalDonations")
    val totalDonations: Int? = null,
    @SerialName("totalMeals")
    val totalMeals: Int? = null
)

