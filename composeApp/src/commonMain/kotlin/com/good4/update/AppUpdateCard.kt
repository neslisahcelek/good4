package com.good4.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.components.StandardButtonHeight
import com.good4.review.ReviewModalBlocker
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

/** Each home screen renders this slot in its content, below its own top bar. */
internal val LocalAppUpdateBanner = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

/** Keeps store integration alive while navigating home tabs, without moving their headers. */
@Composable
fun UpdateHomeContent(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val service = rememberAppUpdateService()
    val viewModel: AppUpdateViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(service, viewModel) {
        viewModel.attach(service)
        onDispose { viewModel.detach() }
    }
    LifecycleResumeEffect(service, enabled) {
        if (enabled) viewModel.refresh()
        onPauseOrDispose { }
    }
    val updateAvailable = state.status == UpdateStatus.AVAILABLE ||
        state.status == UpdateStatus.DOWNLOADING ||
        state.status == UpdateStatus.READY
    ReviewModalBlocker(enabled && !state.snoozed && updateAvailable)
    val visible = enabled && !state.snoozed && updateAvailable
    val banner: (@Composable () -> Unit)? = if (visible) {
        { AppUpdateCard(state, viewModel::update, viewModel::later) }
    } else null
    CompositionLocalProvider(LocalAppUpdateBanner provides banner) {
        Box(modifier, content = content)
    }
}

@Composable
private fun AppUpdateCard(state: AppUpdateState, onUpdate: () -> Unit, onLater: () -> Unit) {
    val downloading = state.status == UpdateStatus.DOWNLOADING
    val ready = state.status == UpdateStatus.READY
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderMuted),
        colors = CardDefaults.cardColors(containerColor = SurfaceDefault),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(40.dp).background(PistachioGreen, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (downloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.SystemUpdateAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (ready) stringResource(Res.string.app_update_ready_title) else state.title.asString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        when {
                            ready -> stringResource(Res.string.app_update_ready_body)
                            downloading -> stringResource(Res.string.app_update_downloading)
                            else -> state.message.asString()
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            if (state.error) {
                Text(
                    stringResource(Res.string.app_update_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            val fontScale = LocalDensity.current.fontScale
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Keep labels readable on smaller iPhones and with accessibility text sizes.
                if (maxWidth < 300.dp || fontScale > 1.2f) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!downloading) {
                            AppUpdateAction(state, onUpdate, Modifier.fillMaxWidth())
                        }
                        AppUpdateLater(state, onLater, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppUpdateLater(state, onLater, Modifier.weight(1f))
                        if (!downloading) {
                            AppUpdateAction(state, onUpdate, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUpdateAction(state: AppUpdateState, onUpdate: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onUpdate,
        enabled = !state.busy,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        modifier = modifier.heightIn(min = StandardButtonHeight)
    ) {
        if (state.busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                stringResource(
                    if (state.status == UpdateStatus.READY) Res.string.app_update_restart
                    else Res.string.app_update_action
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AppUpdateLater(state: AppUpdateState, onLater: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onLater,
        enabled = !state.busy,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderMuted),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        modifier = modifier.heightIn(min = StandardButtonHeight)
    ) {
        Text(stringResource(Res.string.app_update_later), style = MaterialTheme.typography.labelLarge)
    }
}

@Preview
@Composable
private fun AppUpdateCardPreview() {
    com.good4.core.presentation.Good4Theme {
        Box(Modifier.width(358.dp)) {
            AppUpdateCard(AppUpdateState(status = UpdateStatus.AVAILABLE), {}, {})
        }
    }
}

@Preview
@Composable
private fun AppUpdateCardNarrowPreview() {
    com.good4.core.presentation.Good4Theme {
        Box(Modifier.width(280.dp)) {
            AppUpdateCard(AppUpdateState(status = UpdateStatus.AVAILABLE, error = true), {}, {})
        }
    }
}

@Preview
@Composable
private fun AppUpdateCardDarkPreview() {
    com.good4.core.presentation.Good4Theme(darkTheme = true) {
        Box(Modifier.width(358.dp)) {
            AppUpdateCard(AppUpdateState(status = UpdateStatus.READY), {}, {})
        }
    }
}
