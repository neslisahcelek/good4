package com.good4.auth.presentation.login

import androidx.compose.runtime.Composable

@Composable
actual fun AppleSignInButton(
    enabled: Boolean,
    loading: Boolean,
    onCredential: (idToken: String, rawNonce: String) -> Unit,
    onError: (String) -> Unit
) = Unit
