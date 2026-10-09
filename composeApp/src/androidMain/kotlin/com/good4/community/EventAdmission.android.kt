package com.good4.community

import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

import androidx.compose.ui.Modifier
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.good4.core.network.callV2Function
import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

actual fun newEventTicketToken(): String = UUID.randomUUID().toString()

actual fun observeCommunityFollowerCount(communityId: String) = callbackFlow {
    val root = if (AppEnvironment.firebaseBackend == FirebaseBackend.V2) "organizations" else "communities"
    val listener = FirebaseFirestore.getInstance().collection("$root/$communityId/followers")
        .limit(100)
        .addSnapshotListener { snapshot, error ->
            if (error != null) close(error) else if (snapshot != null) trySend(snapshot.size())
        }
    awaitClose { listener.remove() }
}

actual fun observeEventAdmission(communityId: String, eventId: String): kotlinx.coroutines.flow.Flow<EventAdmissionSnapshot> {
    val v2 = AppEnvironment.firebaseBackend == FirebaseBackend.V2
    val root = if (v2) "events/$eventId" else "communities/$communityId/entries/$eventId"
    return combine(
    callbackFlow<List<CommunityEventRegistrationDto>> {
        val listener = FirebaseFirestore.getInstance().collection("$root/registrations")
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error) else if (snapshot != null) trySend(snapshot.documents.map {
                    CommunityEventRegistrationDto(if (v2) it.getString("userId").orEmpty() else it.id, it.getString("displayName").orEmpty(),
                        (it.get("registeredAt") as? Number)?.toLong() ?: it.getTimestamp("registeredAt")?.seconds ?: 0L,
                        if (v2) it.id else it.getString("ticketToken").orEmpty())
                })
            }
        awaitClose { listener.remove() }
    },
    callbackFlow<List<EventAttendanceDto>> {
        val attendanceCollection = if (v2) "checkins" else "attendance"
        val listener = FirebaseFirestore.getInstance().collection("$root/$attendanceCollection")
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error) else if (snapshot != null) trySend(snapshot.documents.map {
                    EventAttendanceDto(it.getString("userId") ?: it.id, it.getString("checkedInBy").orEmpty(),
                        (it.get("checkedInAt") as? Number)?.toLong() ?: it.getTimestamp("checkedInAt")?.seconds ?: 0L,
                        it.getString("method").orEmpty(), it.getString("ticketToken").orEmpty())
                })
            }
        awaitClose { listener.remove() }
    }
    ) { registrations, attendance -> EventAdmissionSnapshot(registrations, attendance) }
}

actual suspend fun recordEventAdmission(communityId: String, eventId: String, userId: String, ticketToken: String?, undo: Boolean): Boolean {
    if (AppEnvironment.firebaseBackend == FirebaseBackend.V2) {
        val result = callV2Function("recordEventAttendance", buildJsonObject {
            put("eventId", eventId); put("undo", undo)
            if (ticketToken != null) put("registrationId", ticketToken) else put("userId", userId)
        })
        return result["changed"]?.jsonPrimitive?.boolean ?: false
    }
    val db = FirebaseFirestore.getInstance()
    val manager = FirebaseAuth.getInstance().currentUser?.uid ?: error("Giriş yapmalısınız.")
    val root = db.document("communities/$communityId/entries/$eventId")
    return db.runTransaction { tx ->
        val event = tx.get(root)
        val registration = tx.get(root.collection("registrations").document(userId))
        val ref = root.collection("attendance").document(userId)
        val attendance = tx.get(ref)
        check(event.getString("kind") == "event" && event.getString("status") == "published") { "Bu etkinlik giriş almıyor." }
        check(registration.exists()) { "Öğrencinin etkinlik kaydı bulunamadı veya iptal edilmiş." }
        if (ticketToken != null) check(registration.getString("ticketToken") == ticketToken) { "Bu bilet artık geçerli değil." }
        if (undo) {
            if (attendance.exists()) tx.delete(ref)
            attendance.exists()
        } else if (attendance.exists()) false else {
            tx.set(ref, mapOf("userId" to userId, "checkedInBy" to manager,
                "checkedInAt" to System.currentTimeMillis() / 1000,
                "method" to if (ticketToken == null) "manual" else "qr",
                "ticketToken" to (ticketToken ?: "")))
            true
        }
    }.await()
}

@Composable
actual fun EventScannerButton(enabled: Boolean, onScanned: (String) -> Unit, onError: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val scanError = stringResource(Res.string.community_qr_kod_okunamadi)
    val cameraError = stringResource(Res.string.community_kamera_acilamadi_google_play_hizmetlerini_ve_baglantini_kontrol_e)
    val cameraOpening = stringResource(Res.string.community_kamera_aciliyor)
    val scanLabel = stringResource(Res.string.community_qr_ile_giris_al)
    var scanning by remember { mutableStateOf(false) }
    Button(modifier = modifier, enabled = enabled && !scanning, onClick = {
        scanning = true
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { barcode ->
                scanning = false
                barcode.rawValue?.let(onScanned) ?: onError(scanError)
            }.addOnCanceledListener { scanning = false }
            .addOnFailureListener { scanning = false; onError(cameraError) }
    }) { Text(if (scanning) cameraOpening else scanLabel) }
}
