package com.good4.campuscloset

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.SurfaceDefault

@Composable
fun CampusEmailVerificationCard(
    state: CampusEmailVerificationState,
    onEmailChange: (String) -> Unit,
    onSendLink: () -> Unit,
    onChangeEmail: () -> Unit,
    onRetryLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val busy = state.isSending || state.isConfirming
    val numberFocus = remember { FocusRequester() }
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDefault)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (state.verifiedEmail != null) "E-postan onaylandı" else "İlan vermek için okul e-postanı doğrula",
                fontWeight = FontWeight.SemiBold)
            if (state.verifiedEmail == null) {
                Text("Doğrulama e-postana Outlook üzerinden ulaşabilirsin. " +
                    "Gereksiz E-posta/Spam klasörünü kontrol etmeyi unutma.")
            }
            if (state.verifiedEmail != null) {
                Text("İlan verebilirsin.", color = MaterialTheme.colorScheme.primary)
            } else if (state.sentTo == null) {
                OutlinedTextField(
                    value = state.email.substringBefore('@').filter { it in '0'..'9' },
                    onValueChange = { input ->
                        // A pasted full Akdeniz address is also accepted; another
                        // domain must not silently become a different recipient.
                        val local = if ('@' in input) {
                            if (isCampusStudentEmail(input)) input.trim().substringBefore('@') else null
                        } else input
                        if (local != null) {
                            val number = local.filter { it in '0'..'9' }.take(64)
                            onEmailChange(if (number.isEmpty()) "" else "$number@$CAMPUS_EMAIL_DOMAIN")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(numberFocus),
                    label = { Text("Öğrenci numaran") }, placeholder = { Text("Numaran") },
                    trailingIcon = {
                        Text("@$CAMPUS_EMAIL_DOMAIN",
                            modifier = Modifier.padding(end = 12.dp).clickable(enabled = !busy) { numberFocus.requestFocus() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(onClick = onSendLink, enabled = !busy && state.resendSeconds == 0 && state.email.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(if (state.isSending) "Gönderiliyor…" else if (state.resendSeconds > 0)
                        "Tekrar dene (${state.resendSeconds} sn)" else "Doğrulama e-postası gönder")
                }
            } else {
                Text("Doğrulamadan sonra ilan verebilirsin.")
                if (state.isConfirming) {
                    CircularProgressIndicator()
                    Text("E-postan doğrulanıyor…")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onChangeEmail, enabled = !busy) { Text("E-postayı değiştir") }
                    TextButton(onClick = onSendLink, enabled = !busy && state.resendSeconds == 0) {
                        Text(if (state.resendSeconds > 0) "Tekrar gönder (${state.resendSeconds} sn)" else "Tekrar gönder")
                    }
                }
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                if (state.hasReceivedLink && !busy) {
                    TextButton(onClick = onRetryLink) { Text("Tekrar dene") }
                }
            }
        }
    }
}
