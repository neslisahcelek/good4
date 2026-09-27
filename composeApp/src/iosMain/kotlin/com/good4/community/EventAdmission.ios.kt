package com.good4.community

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import platform.Foundation.NSUUID
import com.good4.core.network.callV2Function
import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

actual fun newEventTicketToken(): String = NSUUID().UUIDString.lowercase()

actual fun observeCommunityFollowerCount(communityId: String) = Firebase.firestore
    .collection("${if (AppEnvironment.firebaseBackend == FirebaseBackend.V2) "organizations" else "communities"}/$communityId/followers")
    .snapshots.map { it.documents.size }

actual fun observeEventAdmission(communityId: String, eventId: String): Flow<EventAdmissionSnapshot> {
    val v2 = AppEnvironment.firebaseBackend == FirebaseBackend.V2
    val root = if (v2) "events/$eventId" else "communities/$communityId/entries/$eventId"
    return combine(
        Firebase.firestore.collection("$root/registrations").snapshots,
        Firebase.firestore.collection("$root/${if (v2) "checkins" else "attendance"}").snapshots
    ) { registrations, attendance ->
        if (!v2) EventAdmissionSnapshot(
            registrations.documents.map { it.data<CommunityEventRegistrationDto>() },
            attendance.documents.map { it.data<EventAttendanceDto>() }
        ) else EventAdmissionSnapshot(
            registrations.documents.map { document ->
                CommunityEventRegistrationDto(
                    document.get<String>("userId"), document.get<String>("displayName"),
                    document.get<Timestamp>("registeredAt").seconds, document.id
                )
            },
            attendance.documents.map { document ->
                EventAttendanceDto(
                    document.get<String>("userId"), document.get<String>("checkedInBy"),
                    document.get<Timestamp>("checkedInAt").seconds, document.get<String>("method"), document.id
                )
            }
        )
    }
}

actual suspend fun recordEventAdmission(communityId: String, eventId: String, userId: String, ticketToken: String?, undo: Boolean): Boolean {
    if (AppEnvironment.firebaseBackend == FirebaseBackend.V2) {
        val result = callV2Function("recordEventAttendance", buildJsonObject {
            put("eventId", eventId); put("undo", undo)
            if (ticketToken != null) put("registrationId", ticketToken) else put("userId", userId)
        })
        return result["changed"]?.jsonPrimitive?.boolean ?: false
    }
    val db = Firebase.firestore
    val manager = Firebase.auth.currentUser?.uid ?: error("Giriş yapmalısınız.")
    val root = db.document("communities/$communityId/entries/$eventId")
    return db.runTransaction {
        val event = get(root)
        val registration = get(root.collection("registrations").document(userId))
        val ref = root.collection("attendance").document(userId)
        val attendance = get(ref)
        check(event.get<String>("kind") == "event" && event.get<String>("status") == "published") { "Bu etkinlik giriş almıyor." }
        check(registration.exists) { "Öğrencinin etkinlik kaydı bulunamadı veya iptal edilmiş." }
        if (ticketToken != null) check(registration.get<String>("ticketToken") == ticketToken) { "Bu bilet artık geçerli değil." }
        if (undo) {
            if (attendance.exists) delete(ref)
            attendance.exists
        } else if (attendance.exists) false else {
            set(ref, EventAttendanceDto(userId, manager, Clock.System.now().epochSeconds,
                if (ticketToken == null) "manual" else "qr", ticketToken ?: ""))
            true
        }
    }
}

@Composable
actual fun EventScannerButton(enabled: Boolean, onScanned: (String) -> Unit, onError: (String) -> Unit) {
    var scanning by remember { mutableStateOf(false) }
    Button(enabled = enabled && !scanning, onClick = {
        val launcher = EventScannerBridge.launcher
        if (launcher == null) onError("Kamera kullanılamıyor. Manuel giriş kullanabilirsiniz.") else {
            scanning = true
            launcher.launch(object : EventScannerCallback {
                override fun complete(value: String?, error: String?) {
                    scanning = false
                    if (value != null) onScanned(value) else if (error != null) onError(error)
                }
            })
        }
    }) { Text(if (scanning) "Kamera açılıyor…" else "QR okut") }
}
