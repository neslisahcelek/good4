package com.good4.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
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
    NotificationsContent(state, onBack, viewModel::select, { viewModel.read() }, viewModel::savePreferences,
        { launcher.openSettings() }, viewModel::refresh)
    state.selected?.let { notification ->
        AlertDialog(
            onDismissRequest = viewModel::closeDetail,
            title = { Text(notification.data.title) },
            text = { Text(notification.data.body) },
            confirmButton = {
                if (notification.data.eventId.isNotBlank() && notification.data.kind != "eventCancelled") {
                    TextButton(onClick = { onOpenEvent(notification) }) {
                        Text(stringResource(if (notification.data.kind == "eventReminder") Res.string.notification_open_ticket else Res.string.notification_open_event))
                    }
                } else TextButton(onClick = viewModel::closeDetail) { Text(stringResource(Res.string.notification_close)) }
            },
            dismissButton = {
                if (notification.data.eventId.isNotBlank() && notification.data.kind != "eventCancelled") {
                    TextButton(onClick = viewModel::closeDetail) { Text(stringResource(Res.string.notification_close)) }
                }
            }
        )
    }
}

@Composable
private fun NotificationsContent(
    state: NotificationsState, onBack: () -> Unit, onSelect: (StudentNotification) -> Unit,
    onReadAll: () -> Unit, onPreferences: (NotificationPreferencesDto) -> Unit,
    onSettings: () -> Unit, onRetry: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(AppBackground).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.notification_back)) }
            Text(stringResource(Res.string.notification_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onReadAll, enabled = state.supported && state.notifications.any { it.data.readAt == 0L }) {
                Icon(Icons.Filled.DoneAll, stringResource(Res.string.notification_read_all))
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.supported) item(key = "preferences") {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(Res.string.notification_preferences), style = MaterialTheme.typography.titleMedium)
                        PreferenceRow(stringResource(Res.string.notification_event_updates), state.preferences.eventUpdates, !state.saving) { onPreferences(state.preferences.copy(eventUpdates = it)) }
                        PreferenceRow(stringResource(Res.string.notification_reminders), state.preferences.reminders, !state.saving) { onPreferences(state.preferences.copy(reminders = it)) }
                        PreferenceRow(stringResource(Res.string.notification_announcements), state.preferences.announcements, !state.saving) { onPreferences(state.preferences.copy(announcements = it)) }
                        if (!state.permission) Text(stringResource(Res.string.notification_permission_off), style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().height(StandardButtonHeight)) {
                            Text(stringResource(Res.string.notification_system_settings))
                        }
                    }
                }
            }
            state.error?.let { error -> item(key = "error") {
                Text(error.asString(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text(stringResource(Res.string.notification_retry)) }
            } }
            if (state.loading) item(key = "loading") { CircularProgressIndicator() }
            if (!state.loading && state.notifications.isEmpty()) item(key = "empty") {
                Text(stringResource(if (state.supported) Res.string.notification_empty else Res.string.notification_staging_unavailable))
            }
            items(state.notifications, key = { it.id }) { notification ->
                ElevatedCard(onClick = { onSelect(notification) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row {
                            Text(notification.data.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            if (notification.data.readAt == 0L) Text(stringResource(Res.string.notification_unread), color = MaterialTheme.colorScheme.primary)
                        }
                        Text(notification.data.body, style = MaterialTheme.typography.bodyMedium)
                        val local = Instant.fromEpochSeconds(notification.data.createdAt).toLocalDateTime(TimeZone.of("Europe/Istanbul"))
                        Text("${local.date} ${local.time.toString().take(5)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
@Composable
private fun PreferenceRow(title: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
@Composable
fun NotificationPermissionEducation() {
    val requested by PushSignals.education.collectAsStateWithLifecycle()
    val launcher = rememberNotificationPermissionLauncher()
    if (requested) AlertDialog(
        onDismissRequest = PushSignals::dismissEducation,
        title = { Text(stringResource(Res.string.notification_permission_title)) },
        text = { Text(stringResource(Res.string.notification_permission_body)) },
        confirmButton = { TextButton(onClick = { PushSignals.dismissEducation(); launcher.request { PushSignals.refresh() } }) { Text(stringResource(Res.string.notification_enable)) } },
        dismissButton = { TextButton(onClick = PushSignals::dismissEducation) { Text(stringResource(Res.string.notification_later)) } }
    )
}
@Preview
@Composable
private fun NotificationsPreview() {
    com.good4.core.presentation.Good4Theme {
        NotificationsContent(NotificationsState(notifications = listOf(StudentNotification("preview", NotificationDto(title = "Etkinlik", body = "Kampüs etkinliği", createdAt = 1790000000)))), {}, {}, {}, {}, {}, {})
    }
}
