package com.good4.community

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class EventAttendanceDto(
    val userId: String = "",
    val checkedInBy: String = "",
    val checkedInAt: Long = 0,
    @EncodeDefault val method: String = "manual",
    @EncodeDefault val ticketToken: String = ""
)

data class EventAdmissionSnapshot(
    val registrations: List<CommunityEventRegistrationDto> = emptyList(),
    val attendance: List<EventAttendanceDto> = emptyList()
)

interface EventAdmissionGateway {
    fun observe(communityId: String, eventId: String): Flow<EventAdmissionSnapshot>
    fun followers(communityId: String): Flow<Int>
    suspend fun record(communityId: String, eventId: String, userId: String, token: String?, undo: Boolean): Boolean
}

object NativeEventAdmissionGateway : EventAdmissionGateway {
    override fun observe(communityId: String, eventId: String) = observeEventAdmission(communityId, eventId)
    override fun followers(communityId: String) = observeCommunityFollowerCount(communityId)
    override suspend fun record(communityId: String, eventId: String, userId: String, token: String?, undo: Boolean) =
        recordEventAdmission(communityId, eventId, userId, token, undo)
}

expect fun newEventTicketToken(): String
expect fun observeEventAdmission(communityId: String, eventId: String): Flow<EventAdmissionSnapshot>
expect fun observeCommunityFollowerCount(communityId: String): Flow<Int>
// A server transaction verifies registration and prevents two gatekeepers counting one entry twice.
expect suspend fun recordEventAdmission(communityId: String, eventId: String, userId: String, ticketToken: String?, undo: Boolean): Boolean

interface EventScannerCallback { fun complete(value: String?, error: String?) }
interface EventScannerLauncher { fun launch(completion: EventScannerCallback) }
object EventScannerBridge { var launcher: EventScannerLauncher? = null }

@Composable
expect fun EventScannerButton(enabled: Boolean, onScanned: (String) -> Unit, onError: (String) -> Unit)

fun eventTicketPayload(communityId: String, eventId: String, token: String) =
    if (AppEnvironment.firebaseBackend == FirebaseBackend.V2) "good4:event:v2/$eventId/$token"
    else "good4:event:v1/$communityId/$eventId/$token"

fun parseEventTicket(value: String, communityId: String, eventId: String): String {
    val parts = value.trim().split('/')
    if (AppEnvironment.firebaseBackend == FirebaseBackend.V2) {
        require(parts.size == 3 && parts[0] == "good4:event:v2") { "Bu bir Good4 V2 etkinlik bileti değil." }
        require(parts[1] == eventId) { "Bu bilet bu etkinliğe ait değil." }
        require(Regex("[a-f0-9-]{36}").matches(parts[2])) { "Bilet geçersiz." }
        return parts[2]
    }
    require(parts.size == 4 && parts[0] == "good4:event:v1") { "Bu bir Good4 etkinlik bileti değil." }
    require(parts[1] == communityId && parts[2] == eventId) { "Bu bilet bu etkinliğe ait değil." }
    require(Regex("[a-f0-9-]{36}").matches(parts[3])) { "Bilet geçersiz." }
    return parts[3]
}
