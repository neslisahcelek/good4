package com.good4.notification

import com.good4.core.presentation.components.Good4LoadingState

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.*
import com.good4.core.presentation.components.StandardButtonHeight
import good4.composeapp.generated.resources.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun NotificationsScreen(onBack: () -> Unit, viewModel: NotificationsViewModel, onOpenEvent: (StudentNotification) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launcher = rememberNotificationPermissionLauncher()
    LaunchedEffect(Unit) { viewModel.refresh() }
    NotificationsContent(
        state = state,
        onBack = onBack,
        onSelect = viewModel::select,
        onReadAll = { viewModel.read() },
        onDelete = viewModel::delete,
        onDeleteAll = viewModel::deleteAll,
        onPreferences = viewModel::savePreferences,
        onSettings = { launcher.openSettings() },
        onRetry = viewModel::refresh,
        onLoadMore = viewModel::loadMore
    )
    state.selected?.let { notification ->
        AlertDialog(
            onDismissRequest = viewModel::closeDetail,
            title = { Text(notification.data.title) },
            text = { Text(notification.data.body) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { viewModel.delete(notification.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Text(stringResource(Res.string.notification_delete))
                    }
                    if (notification.data.eventId.isNotBlank() && notification.data.kind != "eventCancelled") {
                        TextButton(onClick = { onOpenEvent(notification) }) {
                            Text(stringResource(if (notification.data.kind == "eventReminder") Res.string.notification_open_ticket else Res.string.notification_open_event))
                        }
                    } else TextButton(onClick = viewModel::closeDetail) { Text(stringResource(Res.string.notification_close)) }
                }
            },
            dismissButton = {
                if (notification.data.eventId.isNotBlank() && notification.data.kind != "eventCancelled") {
                    TextButton(onClick = viewModel::closeDetail) { Text(stringResource(Res.string.notification_close)) }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsContent(
    state: NotificationsState,
    onBack: () -> Unit,
    onSelect: (StudentNotification) -> Unit,
    onReadAll: () -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onPreferences: (NotificationPreferencesDto) -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    var showPreferences by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllConfirm by rememberSaveable { mutableStateOf(false) }

    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text(stringResource(Res.string.notification_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.notification_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllConfirm = false
                        onDeleteAll()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text(stringResource(Res.string.notification_delete_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) {
                    Text(stringResource(Res.string.notification_close))
                }
            }
        )
    }
    if (showPreferences && state.supported) {
        ModalBottomSheet(
            onDismissRequest = { showPreferences = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceDefault,
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            NotificationPreferencesContent(state, onPreferences, onSettings, { showPreferences = false })
        }
    }
    Column(Modifier.fillMaxSize().background(AppBackground).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.notification_back)) }
            Text(stringResource(Res.string.notification_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onReadAll, enabled = state.supported && state.notifications.any { it.data.readAt == 0L }) {
                Icon(Icons.Filled.DoneAll, stringResource(Res.string.notification_read_all))
            }
            IconButton(
                onClick = { showDeleteAllConfirm = true },
                enabled = state.supported && state.notifications.isNotEmpty()
            ) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    stringResource(Res.string.notification_delete_all),
                    tint = if (state.notifications.isNotEmpty()) TextPrimary else TextSecondary
                )
            }
            if (state.supported) {
                IconButton(onClick = { showPreferences = true }) {
                    Icon(Icons.Filled.Settings, stringResource(Res.string.notification_preferences), tint = TextPrimary)
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (state.loading && state.notifications.isEmpty()) {
                Good4LoadingState()
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.error?.let { error -> item(key = "error") {
                        Text(error.asString(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text(stringResource(Res.string.notification_retry)) }
                    } }
                    if (state.loading) item(key = "loading") {
                        Good4LoadingState(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                    }
                    if (!state.loading && state.notifications.isEmpty()) item(key = "empty") {
                        Text(stringResource(if (state.supported) Res.string.notification_empty else Res.string.notification_staging_unavailable))
                    }
                    items(state.notifications, key = { it.id }) { notification ->
                        ElevatedCard(onClick = { onSelect(notification) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(notification.data.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                    if (notification.data.readAt == 0L) {
                                        Text(
                                            stringResource(Res.string.notification_unread),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDelete(notification.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = stringResource(Res.string.notification_delete),
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(notification.data.body, style = MaterialTheme.typography.bodyMedium)
                                val local = Instant.fromEpochSeconds(notification.data.createdAt).toLocalDateTime(TimeZone.of("Europe/Istanbul"))
                                Text("${local.date} ${local.time.toString().take(5)}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (state.nextCursor != null || state.loadingMore || state.pageError != null) item(key = "next-page") {
                        if (!state.loading && state.pageError == null) {
                            LaunchedEffect(state.nextCursor) { onLoadMore() }
                        }
                        if (state.loadingMore) Good4LoadingState(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                        state.pageError?.let {
                            Text(it.asString(), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onLoadMore) { Text(stringResource(Res.string.notification_retry)) }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun NotificationPreferencesContent(
    state: NotificationsState,
    onPreferences: (NotificationPreferencesDto) -> Unit,
    onSettings: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(Res.string.notification_preferences), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(stringResource(Res.string.notification_preferences_description), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, stringResource(Res.string.notification_close), tint = TextSecondary)
            }
        }
        Surface(shape = RoundedCornerShape(24.dp), color = SurfaceMuted) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                PreferenceRow(
                    stringResource(Res.string.notification_event_updates),
                    stringResource(Res.string.notification_event_updates_description),
                    state.preferences.eventUpdates, !state.saving && !state.loading && state.preferencesLoaded
                ) { onPreferences(state.preferences.copy(eventUpdates = it)) }
                HorizontalDivider(color = BorderMuted)
                PreferenceRow(
                    stringResource(Res.string.notification_reminders),
                    stringResource(Res.string.notification_reminders_description),
                    state.preferences.reminders, !state.saving && !state.loading && state.preferencesLoaded
                ) { onPreferences(state.preferences.copy(reminders = it)) }
                HorizontalDivider(color = BorderMuted)
                PreferenceRow(
                    stringResource(Res.string.notification_announcements),
                    stringResource(Res.string.notification_announcements_description),
                    state.preferences.announcements, !state.saving && !state.loading && state.preferencesLoaded
                ) { onPreferences(state.preferences.copy(announcements = it)) }
            }
        }
        state.error?.let { Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!state.permission) {
                Text(stringResource(Res.string.notification_permission_off), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            OutlinedButton(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth().height(StandardButtonHeight),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderMuted),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text(stringResource(Res.string.notification_system_settings))
            }
        }
    }
}

@Composable
private fun PreferenceRow(title: String, description: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked, onCheckedChange = null, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SurfaceDefault,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = SurfaceDefault,
                uncheckedTrackColor = BorderMuted,
                uncheckedBorderColor = BorderMuted
            )
        )
    }
}

@Preview
@Composable
private fun NotificationsPreview() {
    com.good4.core.presentation.Good4Theme {
        NotificationsContent(
            state = NotificationsState(notifications = listOf(StudentNotification("preview", NotificationDto(title = "Etkinlik", body = "Kampüs etkinliği", createdAt = 1790000000)))),
            onBack = {}, onSelect = {}, onReadAll = {}, onDelete = {}, onDeleteAll = {},
            onPreferences = {}, onSettings = {}, onRetry = {}, onLoadMore = {}
        )
    }
}

@Preview
@Composable
private fun NotificationPreferencesPreview() {
    Good4Theme {
        Surface(color = SurfaceDefault) {
            NotificationPreferencesContent(NotificationsState(), {}, {}, {})
        }
    }
}
