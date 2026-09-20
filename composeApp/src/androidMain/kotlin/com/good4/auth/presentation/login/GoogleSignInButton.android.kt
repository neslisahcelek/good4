package com.good4.auth.presentation.login

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
actual fun GoogleSignInButton(
    enabled: Boolean,
    onToken: (idToken: String, accessToken: String?) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    OutlinedButton(enabled = enabled && !busy, modifier = Modifier.fillMaxWidth(), onClick = {
        scope.launch {
            busy = true
            try {
                val resource = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                if (resource == 0) {
                    onError("Google ile giriş henüz etkinleştirilmedi. Lütfen Good4 ekibiyle iletişime geçin.")
                } else {
                    val option = GetSignInWithGoogleOption.Builder(context.getString(resource)).build()
                    val result = CredentialManager.create(context).getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
                    onToken(GoogleIdTokenCredential.createFrom(result.credential.data).idToken, null)
                }
            } catch (_: GetCredentialCancellationException) { /* Account chooser dismissed. */ }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { onError("Google hesabı açılamadı. Bağlantınızı kontrol edip tekrar deneyin.") }
            finally { busy = false }
        }
    }) { Text(if (busy) "Google açılıyor…" else "Google ile devam et") }
}
