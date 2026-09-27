package com.good4.auth.domain

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isEmailVerified: Boolean,
    /** Firebase provider ids such as "password", "google.com", "apple.com". */
    val providerIds: List<String> = emptyList()
) {
    /** Only e-mail + password accounts can use a password reset link. */
    val hasPasswordSignIn: Boolean get() = "password" in providerIds
}

