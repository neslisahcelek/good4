package com.good4.auth.data.repository

interface AppleTokenRevocationCallback {
    fun complete(error: String?)
}

interface AppleTokenRevocationLauncher {
    fun revokeIfNeeded(completion: AppleTokenRevocationCallback)
}

object AppleTokenRevocationBridge {
    var launcher: AppleTokenRevocationLauncher? = null
}

/** Revokes Sign in with Apple before the V2 server removes the account. */
expect suspend fun revokeAppleTokenIfNeeded()
