package com.good4.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.analytics.AnalyticsConsentPrompt
import com.good4.core.presentation.*
import com.good4.core.presentation.components.StandardButtonHeight
import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun rememberHomePermissionSuggestionCard(
    enabled: Boolean,
    viewModel: HomePermissionSuggestionsViewModel = koinViewModel()
): (@Composable () -> Unit)? {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launcher = rememberNotificationPermissionLauncher()
    LifecycleResumeEffect(viewModel, enabled) {
        if (enabled) viewModel.onHomeEntered()
        onPauseOrDispose { viewModel.onHomeLeft() }
    }
    com.good4.review.ReviewModalBlocker(state.saving)
    AnalyticsConsentPrompt(show = enabled && state.analyticsDialogOpen, onDismissed = viewModel::dismissAnalyticsDialog)
    if (!state.notificationCardVisible || state.analyticsDialogOpen) return null
    return {
        NotificationHomeCard(
            saving = state.saving,
            error = state.error?.asString(),
            onDismiss = viewModel::dismiss,
            onEnable = {
                viewModel.enableNotifications { onComplete -> launcher.request(onComplete) }
            },
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun NotificationHomeCard(
    saving: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onEnable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val actionColor = if (saving) TextSecondary else accent
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(PistachioGreen, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.notification_home_title),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(Res.string.notification_home_description),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
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
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !saving, role = Role.Button, onClick = onEnable)
                        .heightIn(min = StandardButtonHeight)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.notification_enable),
                        color = actionColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                            color = actionColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !saving, role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.notification_close),
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun NotificationHomeCardPreview() {
    Good4Theme {
        NotificationHomeCard(onDismiss = {}, onEnable = {}, modifier = Modifier.padding(12.dp))
    }
}

@Preview(widthDp = 360)
@Composable
private fun NotificationHomeCardDarkPreview() {
    Good4Theme(darkTheme = true) {
        NotificationHomeCard(onDismiss = {}, onEnable = {}, modifier = Modifier.padding(12.dp))
    }
}

@Preview(widthDp = 320)
@Composable
private fun NotificationHomeCardSavingPreview() {
    Good4Theme {
        NotificationHomeCard(saving = true, onDismiss = {}, onEnable = {}, modifier = Modifier.padding(12.dp))
    }
}
