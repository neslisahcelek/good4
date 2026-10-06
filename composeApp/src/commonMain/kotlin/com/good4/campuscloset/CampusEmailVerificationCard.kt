package com.good4.campuscloset

import com.good4.core.presentation.components.StandardButtonHeight
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
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
            Text(if (state.verifiedEmail != null) stringResource(Res.string.campus_closet_e_postan_onaylandi) else stringResource(Res.string.campus_closet_ilan_vermek_icin_okul_e_postani_dogrula),
                fontWeight = FontWeight.SemiBold)
            if (state.verifiedEmail == null) {
                Text(stringResource(Res.string.campus_closet_dogrulama_e_postana_outlook_uzerinden_ulasabilirsin_gereksiz_e_posta))
            }
            if (state.verifiedEmail != null) {
                Text(stringResource(Res.string.campus_closet_ilan_verebilirsin), color = MaterialTheme.colorScheme.primary)
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
                    label = { Text(stringResource(Res.string.campus_closet_ogrenci_numaran)) }, placeholder = { Text(stringResource(Res.string.campus_closet_numaran)) },
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
                    modifier = Modifier.fillMaxWidth().height(StandardButtonHeight)) {
                    Text(if (state.isSending) stringResource(Res.string.campus_closet_gonderiliyor) else if (state.resendSeconds > 0)
                        stringResource(Res.string.campus_closet_tekrar_dene_sn, minutesAndSeconds(state.resendSeconds)) else stringResource(Res.string.campus_closet_dogrulama_e_postasi_gonder))
                }
            } else {
                Text(stringResource(Res.string.campus_closet_dogrulamadan_sonra_ilan_verebilirsin))
                if (state.isConfirming) {
                    CircularProgressIndicator()
                    Text(stringResource(Res.string.campus_closet_e_postan_dogrulaniyor))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onChangeEmail, enabled = !busy) { Text(stringResource(Res.string.campus_closet_e_postayi_degistir)) }
                    TextButton(onClick = onSendLink, enabled = !busy && state.resendSeconds == 0) {
                        Text(if (state.resendSeconds > 0) stringResource(Res.string.campus_closet_tekrar_gonder_sn, minutesAndSeconds(state.resendSeconds)) else stringResource(Res.string.campus_closet_tekrar_gonder))
                    }
                }
            }
            state.error?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.error)
                if (state.hasReceivedLink && !busy) {
                    TextButton(onClick = onRetryLink) { Text(stringResource(Res.string.campus_closet_retry)) }
                }
            }
        }
    }
}

/** 300 → "5:00", 59 → "0:59". */
internal fun minutesAndSeconds(seconds: Int): String =
    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
