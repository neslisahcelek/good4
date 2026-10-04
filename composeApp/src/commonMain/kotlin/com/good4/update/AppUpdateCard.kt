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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.PrimaryGreen
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
    ReviewModalBlocker(enabled && !state.snoozed && state.status != UpdateStatus.NONE)
    val visible = enabled && !state.snoozed && state.status != UpdateStatus.NONE
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(40.dp).background(PistachioGreen, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (downloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                            color = PrimaryGreen,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.SystemUpdateAlt,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (ready) stringResource(Res.string.app_update_ready_title) else state.title.asString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary
                    )
                    Text(
                        when {
                            ready -> stringResource(Res.string.app_update_ready_body)
                            downloading -> stringResource(Res.string.app_update_downloading)
                            else -> state.message.asString()
                        },
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = TextSecondary
                    )
                }
            }
            if (state.error) {
                Text(stringResource(Res.string.app_update_error), color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onLater,
                    enabled = !state.busy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.height(StandardButtonHeight)
                ) {
                    Text(stringResource(Res.string.app_update_later), fontSize = 12.sp)
                }
                if (!downloading) {
                    Button(
                        onClick = onUpdate,
                        enabled = !state.busy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        modifier = Modifier.height(StandardButtonHeight)
                    ) {
                        if (state.busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                                strokeWidth = 2.dp,
                                color = TextSecondary
                            )
                        } else {
                            Text(stringResource(if (ready) Res.string.app_update_restart else Res.string.app_update_action),
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppUpdateCardPreview() {
    com.good4.core.presentation.Good4Theme {
        AppUpdateCard(AppUpdateState(status = UpdateStatus.AVAILABLE), {}, {})
    }
}
