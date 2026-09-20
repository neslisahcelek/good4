package com.good4.auth.presentation.login

import androidx.compose.runtime.Composable

@Composable
expect fun GoogleSignInButton(
    enabled: Boolean,
    onToken: (idToken: String, accessToken: String?) -> Unit,
    onError: (String) -> Unit
)

// Installed by the Swift application using the official Google Sign-In SDK.
interface GoogleSignInCallback {
    fun complete(idToken: String?, accessToken: String?, error: String?)
}
interface GoogleSignInLauncher { fun launch(completion: GoogleSignInCallback) }
object GoogleSignInBridge { var launcher: GoogleSignInLauncher? = null }
