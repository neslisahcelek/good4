package com.good4.campuscloset

import com.good4.core.presentation.components.StandardButtonHeight
import com.good4.core.util.openCampusOutlook
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextSecondary

@Composable
fun CampusEmailVerificationCard(
    state: CampusEmailVerificationState,
    onEmailChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onChangeEmail: () -> Unit,
    onCodeChange: (String) -> Unit,
    onConfirmCode: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    readyText: String? = null,
    afterVerifyText: String? = null
) {
    val busy = state.isSending || state.isConfirming
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDefault),
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                state.verifiedEmail != null -> {
                    ClosetSectionHeading(stringResource(Res.string.campus_closet_e_postan_onaylandi), Icons.Outlined.School)
                    Text(readyText ?: stringResource(Res.string.campus_closet_ilan_verebilirsin), color = MaterialTheme.colorScheme.primary)
                }
                state.sentTo == null -> {
                    ClosetSectionHeading(title ?: stringResource(Res.string.campus_email_title), Icons.Outlined.School,
                        stringResource(Res.string.campus_email_code_intro))
                    OutlinedTextField(
                        value = state.email, onValueChange = onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.campus_email_address_label)) },
                        placeholder = { Text(stringResource(Res.string.campus_email_address_example)) },
                        singleLine = true, enabled = !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Button(onClick = onSendCode, enabled = !busy && state.resendSeconds == 0 && state.email.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(StandardButtonHeight), shape = RoundedCornerShape(14.dp)) {
                        Text(if (state.isSending) stringResource(Res.string.campus_closet_gonderiliyor)
                            else if (state.resendSeconds > 0) stringResource(Res.string.campus_email_code_resend_in, minutesAndSeconds(state.resendSeconds))
                            else stringResource(Res.string.campus_email_code_send), fontWeight = FontWeight.SemiBold)
                    }
                    Text(stringResource(Res.string.campus_email_code_hint), color = TextSecondary)
                }
                else -> {
                    ClosetSectionHeading(stringResource(Res.string.campus_email_code_title), Icons.Outlined.MarkEmailUnread)
                    Text(stringResource(Res.string.campus_email_code_sent_to, state.sentTo), color = TextSecondary)
                    afterVerifyText?.let { Text(it) }
                    OutlinedTextField(
                        value = state.code, onValueChange = onCodeChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.campus_email_code_label)) },
                        supportingText = { Text(stringResource(Res.string.campus_email_code_auto_confirm)) },
                        singleLine = true, enabled = !busy && !state.codeBlocked,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (state.isConfirming) {
                        Text(stringResource(Res.string.campus_closet_e_postan_dogrulaniyor))
                    } else if (state.code.length == 6 && !state.codeBlocked) {
                        TextButton(onClick = onConfirmCode, enabled = !busy) { Text(stringResource(Res.string.campus_email_code_confirm)) }
                    }
                    TextButton(onClick = { openCampusOutlook() }) { Text(stringResource(Res.string.campus_email_open_outlook)) }
                    TextButton(onClick = onSendCode, enabled = !busy && state.resendSeconds == 0) {
                        Text(if (state.resendSeconds > 0) stringResource(Res.string.campus_email_code_resend_in, minutesAndSeconds(state.resendSeconds))
                            else stringResource(Res.string.campus_closet_tekrar_gonder))
                    }
                    TextButton(onClick = onChangeEmail, enabled = !busy) { Text(stringResource(Res.string.campus_email_change_address)) }
                }
            }
            state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
        }
    }
}

/** 300 → "5:00", 59 → "0:59". */
internal fun minutesAndSeconds(seconds: Int): String =
    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"


@org.jetbrains.compose.ui.tooling.preview.Preview
@Composable
private fun CampusEmailCodePreview() {
    MaterialTheme {
        CampusEmailVerificationCard(
            state = CampusEmailVerificationState(email = "123@ogr.akdeniz.edu.tr", sentTo = "123@ogr.akdeniz.edu.tr", resendSeconds = 60),
            onEmailChange = {}, onSendCode = {}, onChangeEmail = {}, onCodeChange = {}, onConfirmCode = {}
        )
    }
}
