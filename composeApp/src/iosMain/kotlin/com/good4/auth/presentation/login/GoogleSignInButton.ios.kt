package com.good4.auth.presentation.login

import androidx.compose.runtime.*
import com.good4.auth.presentation.components.GoogleButtonContent

@Composable
actual fun GoogleSignInButton(enabled: Boolean, loading: Boolean, onToken: (String, String?) -> Unit, onError: (String) -> Unit) {
    var busy by remember { mutableStateOf(false) }
    GoogleButtonContent(text = if (busy) "Google açılıyor…" else "Google ile devam et", enabled = enabled && !busy, loading = loading, onClick = {
        val launcher = GoogleSignInBridge.launcher
        if (launcher == null) onError("Google ile giriş henüz etkinleştirilmedi. Lütfen Good4 ekibiyle iletişime geçin.")
        else {
            busy = true
            launcher.launch(object : GoogleSignInCallback {
                override fun complete(token: String?, accessToken: String?, error: String?) {
                busy = false
                if (token != null) onToken(token, accessToken)
                else if (error != null) onError(error)
                }
            })
        }
    })
}
