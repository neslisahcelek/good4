package com.good4.user

import com.good4.user.domain.UserRole
import kotlinx.datetime.Instant

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val phoneNumber: String? = null,
    val role: UserRole,
    val verified: Boolean,
    val university: String? = null,
    val major: String? = null,
    val educationLevel: String? = null,
    val credit: Int? = null,
    val registrationDate: Instant? = null
)

