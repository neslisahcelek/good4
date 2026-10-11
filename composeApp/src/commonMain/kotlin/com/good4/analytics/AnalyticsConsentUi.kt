package com.good4.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.Good4Theme
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.UiText
import com.good4.core.presentation.components.StandardButtonHeight
import config.LegalLinks
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.analytics_allow
import good4.composeapp.generated.resources.analytics_consent_description
import good4.composeapp.generated.resources.analytics_consent_details
import good4.composeapp.generated.resources.analytics_consent_title
import good4.composeapp.generated.resources.analytics_decline
import good4.composeapp.generated.resources.analytics_details_collapsed
import good4.composeapp.generated.resources.analytics_details_expanded
import good4.composeapp.generated.resources.analytics_details_title
import good4.composeapp.generated.resources.analytics_preference_error
import good4.composeapp.generated.resources.analytics_privacy_link
import good4.composeapp.generated.resources.analytics_settings_description
import good4.composeapp.generated.resources.analytics_settings_title
import good4.composeapp.generated.resources.notification_close
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

class AnalyticsInteractionViewModel(private val tracker: AnalyticsTracker) : ViewModel() {
    fun click(interaction: Interaction, target: AnalyticsTarget? = null) {
        tracker.click(interaction, target)
    }
}

class AnalyticsConsentViewModel(private val tracker: AnalyticsTracker) : ViewModel() {
    val consent = tracker.consent
    val promptDismissed = tracker.promptDismissed
    private val mutableError = MutableStateFlow<UiText?>(null)
    val error = mutableError.asStateFlow()
    private val mutableDetailsExpanded = MutableStateFlow(false)
    val detailsExpanded = mutableDetailsExpanded.asStateFlow()
    fun toggleDetails() = mutableDetailsExpanded.update { !it }
    fun choose(enabled: Boolean) {
        val saved =
            tracker.setConsent(if (enabled) AnalyticsConsent.GRANTED else AnalyticsConsent.DENIED)
        mutableError.update { if (saved) null else UiText.StringResourceId(Res.string.analytics_preference_error) }
    }

    fun dismiss() = tracker.dismissPrompt()
}

@Composable
fun AnalyticsConsentPrompt(
    viewModel: AnalyticsConsentViewModel = koinViewModel(),
    show: Boolean = true,
    onDismissed: (() -> Unit)? = null
) {
    val consent by viewModel.consent.collectAsStateWithLifecycle()
    val dismissed by viewModel.promptDismissed.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val detailsExpanded by viewModel.detailsExpanded.collectAsStateWithLifecycle()
    if (show && consent == AnalyticsConsent.UNKNOWN && (onDismissed != null || !dismissed)) {
        com.good4.review.ReviewModalBlocker(true)
        AnalyticsConsentDialog(
            onChoose = viewModel::choose,
            onDismiss = { if (onDismissed != null) onDismissed() else viewModel.dismiss() },
            error = error?.asString(),
            detailsExpanded = detailsExpanded,
            onToggleDetails = viewModel::toggleDetails
        )
    }
}

@Composable
private fun AnalyticsConsentDialog(
    onChoose: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    error: String? = null,
    detailsExpanded: Boolean = false,
    onToggleDetails: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnalyticsConsentCard(
            onChoose = onChoose,
            onDismiss = onDismiss,
            error = error,
            detailsExpanded = detailsExpanded,
            onToggleDetails = onToggleDetails,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun AnalyticsConsentCard(
    onChoose: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    detailsExpanded: Boolean = false,
    onToggleDetails: () -> Unit = {}
) {
    val shape = RoundedCornerShape(28.dp)
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .shadow(16.dp, shape)
            .clip(shape)
            .background(SurfaceDefault)
            .border(1.dp, BorderMuted.copy(alpha = 0.18f), shape)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.analytics_consent_title),
                    color = TextPrimary,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).semantics { heading() }
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, stringResource(Res.string.notification_close), tint = TextSecondary)
                }
            }
            Text(
                text = stringResource(Res.string.analytics_consent_description),
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            val detailsState = stringResource(
                if (detailsExpanded) Res.string.analytics_details_expanded else Res.string.analytics_details_collapsed
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onToggleDetails)
                    .semantics { stateDescription = detailsState }
                    .heightIn(min = StandardButtonHeight)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.analytics_details_title),
                    color = accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (detailsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            AnimatedVisibility(visible = detailsExpanded) {
                Text(
                    text = stringResource(Res.string.analytics_consent_details),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceMuted, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                )
            }
        }
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalyticsConsentAction(
                label = stringResource(Res.string.analytics_decline),
                onClick = { onChoose(false) },
                background = SurfaceMuted,
                contentColor = TextPrimary,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            AnalyticsConsentAction(
                label = stringResource(Res.string.analytics_allow),
                onClick = { onChoose(true) },
                background = accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun AnalyticsConsentAction(
    label: String,
    onClick: () -> Unit,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = StandardButtonHeight)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AnalyticsPreference(viewModel: AnalyticsConsentViewModel = koinViewModel()) {
    val consent by viewModel.consent.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.analytics_settings_title)) },
            supportingContent = { Text(stringResource(Res.string.analytics_settings_description)) },
            trailingContent = {
                Switch(
                    checked = consent == AnalyticsConsent.GRANTED,
                    onCheckedChange = viewModel::choose
                )
            }
        )
        TextButton(onClick = { uriHandler.openUri(LegalLinks.PRIVACY_POLICY) }) {
            Text(stringResource(Res.string.analytics_privacy_link))
        }
        error?.let {
            Text(
                it.asString(),
                Modifier.padding(16.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun AnalyticsConsentPreview() {
    Good4Theme {
        AnalyticsConsentCard(
            onChoose = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun AnalyticsConsentDarkPreview() {
    Good4Theme(darkTheme = true) {
        AnalyticsConsentCard(
            onChoose = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun AnalyticsConsentDetailsPreview() {
    Good4Theme {
        AnalyticsConsentCard(
            onChoose = {},
            onDismiss = {},
            detailsExpanded = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
