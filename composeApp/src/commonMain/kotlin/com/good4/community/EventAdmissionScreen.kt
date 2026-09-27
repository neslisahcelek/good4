package com.good4.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun EventTicketDialog(communityId: String, entry: CommunityEntry, ticket: CommunityEventRegistrationDto, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("QR giriş biletin") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(entry.data.title)
                Text("${entry.data.date} · ${entry.data.time}")
                Image(rememberQrCodePainter(eventTicketPayload(communityId, entry.id, ticket.ticketToken)),
                    "Etkinlik giriş QR kodu", Modifier.fillMaxWidth().aspectRatio(1f).background(Color.White).padding(20.dp))
                Text(ticket.displayName)
                Text("Girişte bu bileti görevliye göster. Bilet sana özeldir; başkalarıyla paylaşma.")
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } })
}

@Composable
fun EventAdmissionScreen(
    entry: CommunityEntry, registrations: List<CommunityEventRegistrationDto>, attendance: List<EventAttendanceDto>,
    busy: Boolean, message: String?, error: String?, onDismiss: () -> Unit,
    onScanned: (String) -> Unit, onError: (String) -> Unit,
    onAdmit: (String, Boolean) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var arrivedOnly by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<Pair<CommunityEventRegistrationDto, Boolean>?>(null) }
    val arrivals = attendance.associateBy { it.userId }
    val filtered = registrations.filter { (!arrivedOnly || it.userId in arrivals) && it.displayName.contains(search, true) }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Katılımcıları Yönet", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(enabled = !busy, onClick = onDismiss) { Text("Kapat") }
                }
                Text(entry.data.title, style = MaterialTheme.typography.titleMedium)
                Text("${registrations.size} kayıtlı · ${attendance.size} giriş yaptı")
                EventScannerButton(enabled = !busy && entry.data.status == "published", onScanned = onScanned, onError = onError)
                Text("QR okutulduğunda giriş onaylanır. Öğrencinin adını kontrol et; yanlış girişi listeden geri alabilirsin.", style = MaterialTheme.typography.bodySmall)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(search, { search = it }, label = { Text("Öğrenci ara") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!arrivedOnly, { arrivedOnly = false }, label = { Text("Kayıtlılar") })
                    FilterChip(arrivedOnly, { arrivedOnly = true }, label = { Text("Gelenler") })
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (filtered.isEmpty()) item { Text(if (search.isNotBlank()) "Aramana uygun öğrenci yok." else if (arrivedOnly) "Henüz giriş yapan yok." else "Henüz kayıtlı öğrenci yok.") }
                    items(filtered, key = { it.userId }) { student ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(student.displayName.ifBlank { "Good4 öğrencisi" })
                                    arrivals[student.userId]?.let {
                                        val time = Instant.fromEpochSeconds(it.checkedInAt).toLocalDateTime(TimeZone.currentSystemDefault())
                                        Text("Giriş: ${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')} · ${if (it.method == "qr") "QR" else "Manuel"}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                TextButton(enabled = !busy && entry.data.status == "published", onClick = { confirmation = student to (student.userId in arrivals) }) {
                                    Text(if (student.userId in arrivals) "Geri al" else "Geldi")
                                }
                            }
                        }
                    }
                }
            }
        }
        confirmation?.let { (student, undo) ->
            AlertDialog(onDismissRequest = { confirmation = null }, title = { Text(if (undo) "Girişi geri al" else "Manuel giriş") },
                text = { Text("${student.displayName}: ${if (undo) "giriş işareti kaldırılacak." else "kimliğini kontrol ettikten sonra girişini onayla."}") },
                confirmButton = { TextButton(onClick = { confirmation = null; onAdmit(student.userId, undo) }) { Text("Onayla") } },
                dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Vazgeç") } })
        }
    }
}
