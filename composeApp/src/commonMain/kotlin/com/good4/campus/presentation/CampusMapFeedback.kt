package com.good4.campus.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.campus.domain.CampusPoint
import com.good4.core.presentation.TextSecondary
import com.good4.feedback.FeedbackViewModel
import org.koin.compose.viewmodel.koinViewModel

private val campusMapFeedbackTopics = listOf("Yanlış konum", "Eksik yer", "Öneri")

/**
 * Short map-specific feedback that lands in the admin panel's feedback list.
 * The selected place is attached only while the student keeps its chip.
 */
@Composable
internal fun CampusMapFeedbackDialog(
    selectedPoint: CampusPoint?,
    onDismiss: () -> Unit,
    feedbackViewModel: FeedbackViewModel = koinViewModel()
) {
    val state by feedbackViewModel.state.collectAsStateWithLifecycle()
    var topic by remember { mutableStateOf(campusMapFeedbackTopics.first()) }
    var details by remember { mutableStateOf("") }
    var attachedPoint by remember { mutableStateOf(selectedPoint) }

    LaunchedEffect(Unit) { feedbackViewModel.startNew() }

    val trimmed = details.trim()
    val canSend = trimmed.length >= 10 && !state.isSubmitting

    AlertDialog(
        onDismissRequest = { if (!state.isSubmitting) onDismiss() },
        title = { Text(if (state.isSubmitted) "Teşekkürler" else "Harita geri bildirimi") },
        text = {
            if (state.isSubmitted) {
                Text("Geri bildirimin Good4 ekibine ulaştı. Haritayı birlikte düzeltelim.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        campusMapFeedbackTopics.forEach { option ->
                            FilterChip(
                                selected = topic == option,
                                onClick = { topic = option },
                                label = { Text(option, fontSize = 12.sp, maxLines = 1) }
                            )
                        }
                    }
                    attachedPoint?.let { point ->
                        InputChip(
                            selected = false,
                            onClick = { attachedPoint = null },
                            label = { Text(point.name, maxLines = 1) },
                            leadingIcon = { Icon(Icons.Outlined.Place, null, Modifier.size(16.dp)) },
                            trailingIcon = { Icon(Icons.Outlined.Close, "İlgili yeri kaldır", Modifier.size(16.dp)) }
                        )
                    }
                    OutlinedTextField(
                        value = details,
                        onValueChange = { if (it.length <= 1000) details = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("Ne gördün? Örn. yemekhane girişi biraz daha kuzeyde.") },
                        supportingText = {
                            Text(
                                state.errorMessage ?: "En az 10 karakter",
                                color = if (state.errorMessage != null) MaterialTheme.colorScheme.error else TextSecondary
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            if (state.isSubmitted) {
                TextButton(onClick = onDismiss) { Text("Kapat") }
            } else {
                TextButton(
                    enabled = canSend,
                    onClick = {
                        feedbackViewModel.onSubjectChange("Kampüs haritası: $topic")
                        feedbackViewModel.onMessageChange(
                            buildString {
                                attachedPoint?.let { append("İlgili yer: ${it.name} (${it.latitude}, ${it.longitude})\n") }
                                append(trimmed)
                            }
                        )
                        feedbackViewModel.submit()
                    }
                ) { Text(if (state.isSubmitting) "Gönderiliyor…" else "Gönder") }
            }
        },
        dismissButton = {
            if (!state.isSubmitted) {
                TextButton(enabled = !state.isSubmitting, onClick = onDismiss) { Text("Vazgeç") }
            }
        }
    )
}
