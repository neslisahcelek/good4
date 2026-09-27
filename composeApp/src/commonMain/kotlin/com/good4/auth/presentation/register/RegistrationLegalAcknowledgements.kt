package com.good4.auth.presentation.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.auth.presentation.components.AuthAccent
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import config.LegalDocumentVersions
import config.LegalLinks
import good4.composeapp.generated.resources.Res
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Composable
fun RegistrationLegalAcknowledgements(
    userAgreementAccepted: Boolean,
    kvkkNoticeAcknowledged: Boolean,
    onUserAgreementToggle: () -> Unit,
    onKvkkNoticeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNotice by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LegalAcknowledgementRow(
            checked = userAgreementAccepted,
            onCheckedChange = onUserAgreementToggle,
            label = buildAnnotatedString {
                withLink(
                    LinkAnnotation.Url(
                        url = LegalLinks.TERMS,
                        styles = legalLinkStyles()
                    )
                ) {
                    append("Kullanıcı Sözleşmesi")
                }
                append("’ni okudum ve kabul ediyorum.")
            }
        )

        LegalAcknowledgementRow(
            checked = kvkkNoticeAcknowledged,
            onCheckedChange = onKvkkNoticeToggle,
            label = buildAnnotatedString {
                append("Kişisel Verilerin İşlenmesine İlişkin ")
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "kvkk-notice",
                        styles = legalLinkStyles(),
                        linkInteractionListener = LinkInteractionListener { showNotice = true }
                    )
                ) {
                    append("Aydınlatma Metni")
                }
                append("’ni okudum ve bilgi edindim.")
            }
        )
    }

    if (showNotice) {
        LegalDocumentSheet(
            title = "Kişisel Verilerin İşlenmesine İlişkin Aydınlatma Metni",
            version = LegalDocumentVersions.KVKK_NOTICE_VERSION,
            resourcePath = "files/kvkk-aydinlatma-metni-v1.md",
            onOpenWebVersion = { uriHandler.openUri(LegalLinks.PRIVACY) },
            onDismiss = { showNotice = false },
        )
    }
    TextButton(onClick = { showPrivacyPolicy = true }) {
        Text(
            text = "Gizlilik Politikası · Sürüm ${LegalDocumentVersions.PRIVACY_POLICY_VERSION}",
            color = AuthAccent
        )
    }
    if (showPrivacyPolicy) {
        LegalDocumentSheet(
            title = "Good4 Gizlilik Politikası",
            version = LegalDocumentVersions.PRIVACY_POLICY_VERSION,
            resourcePath = "files/good4-gizlilik-politikasi-v1.3.md",
            onOpenWebVersion = { uriHandler.openUri(LegalLinks.PRIVACY_POLICY) },
            onDismiss = { showPrivacyPolicy = false },
        )
    }
}

@Composable
private fun LegalAcknowledgementRow(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    label: androidx.compose.ui.text.AnnotatedString
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = CheckboxDefaults.colors(checkedColor = AuthAccent)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                color = TextSecondary
            ),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
private fun LegalDocumentSheet(
    title: String,
    version: String,
    resourcePath: String,
    onOpenWebVersion: () -> Unit,
    onDismiss: () -> Unit,
) {
    var noticeText by remember { mutableStateOf<String?>(null) }
    var readFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            noticeText = Res.readBytes(resourcePath)
                .decodeToString()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            readFailed = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onDismiss) {
                    Text("Kapat")
                }
            }
            Text(
                text = "Sürüm $version",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            when {
                noticeText != null -> Text(
                    text = noticeText!!.toReadableLegalText(),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = TextPrimary
                )

                readFailed -> {
                    Text(
                        text = "Aydınlatma Metni şu anda açılamadı.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    TextButton(onClick = onOpenWebVersion) {
                        Text("Metni web üzerinde aç")
                    }
                }

                else -> CircularProgressIndicator(color = AuthAccent)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun String.toReadableLegalText(): String = lines().joinToString("\n") { line ->
    val trimmed = line.trim()
    when {
        trimmed.startsWith("#") -> trimmed.trimStart('#').trim()
        trimmed.startsWith("* ") -> "• ${trimmed.removePrefix("* ").removeMarkdownEmphasis()}"
        trimmed == "---" -> ""
        else -> line.removeMarkdownEmphasis()
    }
}

private fun String.removeMarkdownEmphasis(): String =
    Regex("\\*\\*(.*?)\\*\\*").replace(this) { it.groupValues[1] }

@Composable
private fun legalLinkStyles() = TextLinkStyles(
    style = SpanStyle(
        color = AuthAccent,
        textDecoration = TextDecoration.Underline,
        fontWeight = FontWeight.Medium
    )
)
