package com.good4.campuscloset

import com.good4.core.presentation.components.StandardButtonHeight
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary

/**
 * Two steps: enter the student number and send the link; then a "check your e-mail" step that tells
 * the student the school mail takes a minute or two, may be opened on any device, and waits for it.
 */
@Composable
fun CampusEmailVerificationCard(
    state: CampusEmailVerificationState,
    onEmailChange: (String) -> Unit,
    onSendLink: () -> Unit,
    onChangeEmail: () -> Unit,
    onRetryLink: () -> Unit,
    modifier: Modifier = Modifier,
    /** Feature wording; Kampüs Dolabı keeps its existing wording by default. */
    title: String? = null,
    readyText: String? = null,
    afterVerifyText: String? = null
) {
    val busy = state.isSending || state.isConfirming
    val numberFocus = remember { FocusRequester() }
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
                    ClosetSectionHeading(
                        title ?: stringResource(Res.string.campus_email_title), Icons.Outlined.School,
                        stringResource(Res.string.campus_email_intro)
                    )
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
                        modifier = Modifier.fillMaxWidth().height(StandardButtonHeight), shape = RoundedCornerShape(14.dp)) {
                        Text(if (state.isSending) stringResource(Res.string.campus_closet_gonderiliyor) else if (state.resendSeconds > 0)
                            stringResource(Res.string.campus_closet_tekrar_dene_sn, minutesAndSeconds(state.resendSeconds)) else stringResource(Res.string.campus_email_send_link),
                            fontWeight = FontWeight.SemiBold)
                    }
                    Text(emphasizeOutlook(stringResource(Res.string.campus_email_send_hint)), color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                }
                else -> {
                    ClosetSectionHeading(
                        stringResource(Res.string.campus_email_check_title), Icons.Outlined.MarkEmailUnread,
                        stringResource(Res.string.campus_email_sent_to, state.sentTo)
                    )
                    afterVerifyText?.let { Text(it) }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        VerificationTip(Icons.Outlined.Schedule, stringResource(Res.string.campus_email_tip_delay))
                        VerificationTip(Icons.Outlined.Devices, emphasizeOutlook(stringResource(Res.string.campus_email_tip_outlook)))
                        VerificationTip(Icons.Outlined.Report, stringResource(Res.string.campus_email_tip_junk))
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceMuted, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (state.isConfirming) stringResource(Res.string.campus_closet_e_postan_dogrulaniyor)
                                else stringResource(Res.string.campus_email_waiting),
                                color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onChangeEmail, enabled = !busy) { Text(stringResource(Res.string.campus_closet_e_postayi_degistir)) }
                        TextButton(onClick = onSendLink, enabled = !busy && state.resendSeconds == 0) {
                            Text(if (state.resendSeconds > 0) stringResource(Res.string.campus_closet_tekrar_gonder_sn, minutesAndSeconds(state.resendSeconds)) else stringResource(Res.string.campus_closet_tekrar_gonder))
                        }
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

@Composable
private fun VerificationTip(icon: ImageVector, text: String) {
    VerificationTip(icon, AnnotatedString(text))
}

@Composable
private fun VerificationTip(icon: ImageVector, text: AnnotatedString) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TiltedIcon(icon, PrimaryGreen, size = 28, iconSize = 15)
        Spacer(Modifier.width(12.dp))
        Text(text, color = TextPrimary, fontSize = 14.sp, lineHeight = 19.sp)
    }
}

/** Students look for the mail in Outlook, so the word stands out wherever the copy mentions it. */
private fun emphasizeOutlook(text: String): AnnotatedString = buildAnnotatedString {
    val start = text.indexOf("Outlook")
    if (start < 0) { append(text); return@buildAnnotatedString }
    append(text.substring(0, start))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) { append("Outlook") }
    append(text.substring(start + "Outlook".length))
}

/** 300 → "5:00", 59 → "0:59". */
internal fun minutesAndSeconds(seconds: Int): String =
    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
