package com.good4.auth.presentation.login

import androidx.compose.runtime.*
import com.good4.auth.presentation.components.GoogleButtonContent
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
actual fun GoogleSignInButton(enabled: Boolean, loading: Boolean, onToken: (String, String?) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    GoogleButtonContent(text = if (busy) "Google açılıyor…" else "Google ile devam et", enabled = enabled && !busy, loading = loading, onClick = {
        scope.launch {
            busy = true
            try {
                val resource = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                if (resource == 0) {
                    android.util.Log.e("GoogleSignIn", "default_web_client_id resource not found in R.string")
                    onError("Google ile giriş henüz etkinleştirilmedi. Lütfen Good4 ekibiyle iletişime geçin.")
                } else {
                    val option = GetSignInWithGoogleOption.Builder(context.getString(resource)).build()
                    val result = CredentialManager.create(context).getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
                    onToken(GoogleIdTokenCredential.createFrom(result.credential.data).idToken, null)
                }
            } catch (e: GetCredentialCancellationException) {
                android.util.Log.w("GoogleSignIn", "Google Sign-In cancelled: ${e.message}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("GoogleSignIn", "Google Sign-In failed: ${e.message}", e)
                onError("Google hesabı açılamadı. Bağlantınızı kontrol edip tekrar deneyin.")
            } finally {
                busy = false
            }
        }
    })
}
