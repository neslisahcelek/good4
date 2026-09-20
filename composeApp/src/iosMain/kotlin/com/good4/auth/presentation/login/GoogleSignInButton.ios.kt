package com.good4.auth.presentation.login

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
actual fun GoogleSignInButton(
    enabled: Boolean,
    onToken: (idToken: String, accessToken: String?) -> Unit,
    onError: (String) -> Unit
) {
    var busy by remember { mutableStateOf(false) }
    OutlinedButton(enabled = enabled && !busy, modifier = Modifier.fillMaxWidth(), onClick = {
        val launcher = GoogleSignInBridge.launcher
        if (launcher == null) onError("Google ile giriş henüz etkinleştirilmedi. Lütfen Good4 ekibiyle iletişime geçin.")
        else {
            busy = true
            launcher.launch(object : GoogleSignInCallback {
                override fun complete(idToken: String?, accessToken: String?, error: String?) {
                    busy = false
                    if (idToken != null && accessToken != null) onToken(idToken, accessToken)
                    else if (error != null) onError(error)
                }
            })
        }
    }) { Text(if (busy) "Google açılıyor…" else "Google ile devam et") }
}
