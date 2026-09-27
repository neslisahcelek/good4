package com.good4.auth.presentation.login

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.auth.presentation.components.AuthSigningInText

@Composable
actual fun AppleSignInButton(
    enabled: Boolean,
    loading: Boolean,
    onCredential: (idToken: String, rawNonce: String) -> Unit,
    onError: (String) -> Unit
) {
    var busy by remember { mutableStateOf(false) }

    Button(
        enabled = enabled && !busy && !loading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White,
            disabledContainerColor = if (loading) Color.Black else Color.Black.copy(alpha = 0.5f),
            disabledContentColor = if (loading) Color.White else Color.White.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = {
            val launcher = AppleSignInBridge.launcher
            if (launcher == null) {
                onError("Apple ile giriş henüz etkinleştirilmedi. Lütfen Good4 ekibiyle iletişime geçin.")
                return@Button
            }

            busy = true
            launcher.launch(object : AppleSignInCallback {
                override fun complete(idToken: String?, rawNonce: String?, error: String?) {
                    busy = false
                    when {
                        idToken != null && rawNonce != null -> onCredential(idToken, rawNonce)
                        error != null -> onError(error)
                    }
                }
            })
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(text = "", fontSize = 22.sp)
            }
            Text(
                text = when {
                    loading -> AuthSigningInText
                    busy -> "Apple açılıyor…"
                    else -> "Apple ile devam et"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
