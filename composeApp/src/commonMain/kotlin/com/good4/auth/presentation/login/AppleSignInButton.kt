package com.good4.auth.presentation.login

import androidx.compose.runtime.Composable

@Composable
expect fun AppleSignInButton(
    enabled: Boolean,
    loading: Boolean,
    onCredential: (idToken: String, rawNonce: String) -> Unit,
    onError: (String) -> Unit
)

interface AppleSignInCallback {
    fun complete(idToken: String?, rawNonce: String?, error: String?)
}

interface AppleSignInLauncher {
    fun launch(completion: AppleSignInCallback)
}

object AppleSignInBridge {
    var launcher: AppleSignInLauncher? = null
}
