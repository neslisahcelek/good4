package com.good4.notification

import com.good4.auth.data.repository.AuthRepository
import com.good4.core.network.callV2Function
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class PushDeviceState(val token: String? = null, val permission: String = "unknown", val revision: Long = 0)
data class CampusPushDestination(val recipientUid: String, val type: String, val targetId: String)

/** Only the primary account may open a notification; IDs are never treated as URLs. */
fun campusPushDestination(recipientUid: String?, type: String?, targetId: String?): CampusPushDestination? {
    if (recipientUid.isNullOrBlank() || recipientUid.length > 128) return null
    if (type != "market_message" && type != "market_listing") return null
    if (targetId.isNullOrBlank() || targetId.length > 300 || targetId.any { it == '/' || it.isWhitespace() || it.isISOControl() }) return null
    return CampusPushDestination(recipientUid, type, targetId)
}

object CampusPushNotifications {
    private val _device = MutableStateFlow(PushDeviceState())
    val device = _device.asStateFlow()
    private val _pending = MutableStateFlow<CampusPushDestination?>(null)
    val pending = _pending.asStateFlow()
    private val _signingOut = MutableStateFlow<String?>(null)
    internal val signingOut = _signingOut.asStateFlow()

    fun updateDevice(token: String?, permission: String) {
        _device.update { PushDeviceState(token.takeIf { permission == "granted" }, permission, it.revision + 1) }
    }

    fun receive(recipientUid: String?, type: String?, targetId: String?) {
        campusPushDestination(recipientUid, type, targetId)?.let { _pending.value = it }
    }

    fun consume(destination: CampusPushDestination) { _pending.compareAndSet(destination, null) }
    fun refresh() = refreshNativePushRegistration()
    fun requestPermission() = requestNativePushPermission()

    internal fun allowSession(uid: String?) {
        if (uid == null || _signingOut.value != uid) _signingOut.value = null
    }

    /** Run while the primary Firebase session still exists. Offline logout still
     * deletes the SDK token so this installation stops receiving the old account's alerts. */
    suspend fun beforeSignOut(uid: String?) {
        _signingOut.value = uid
        val token = _device.value.token
        _pending.value = null
        updateDevice(null, _device.value.permission)
        if (uid != null && token != null) {
            withTimeoutOrNull(4_000) {
                runCatching { callV2Function("unregisterPushDevice", buildJsonObject { put("token", token) }) }
            }
        }
        withTimeoutOrNull(4_000) { deleteNativePushToken() }
    }
}

/** One collector for token rotation, account changes and foreground refresh.
 * A network failure retries without interrupting login or the market. */
class PushRegistrationManager(
    private val auth: AuthRepository,
    private val call: suspend (String, JsonObject) -> JsonObject = ::callV2Function,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private var lastUid: String? = null
    private var lastToken: String? = null
    private var lastRegisteredAt = 0L

    suspend fun observe() {
        combine(auth.authStateFlow, CampusPushNotifications.device, CampusPushNotifications.signingOut) { user, device, signingOut ->
            Triple(user?.uid, device, signingOut)
        }.collectLatest { (uid, device, signingOut) ->
            CampusPushNotifications.allowSession(uid)
            if (uid == null) { lastUid = null; lastToken = null; return@collectLatest }
            if (uid == signingOut) return@collectLatest
            if (device.permission != "granted") {
                if (device.permission == "denied" && lastUid == uid && lastToken != null) {
                    runCatching { call("unregisterPushDevice", buildJsonObject { put("token", lastToken) }) }
                    lastUid = null; lastToken = null
                }
                return@collectLatest
            }
            // A temporary SDK/network failure is not a withdrawal of permission.
            // Keep the last good address until a replacement actually arrives.
            if (device.token == null) return@collectLatest
            if (lastUid == uid && lastToken == device.token && now() - lastRegisteredAt < 6 * 60 * 60 * 1000) return@collectLatest
            var retryMs = 2_000L
            while (auth.currentUser?.uid == uid && CampusPushNotifications.signingOut.value != uid) {
                try {
                    if (lastUid == uid && lastToken != null && lastToken != device.token) {
                        call("unregisterPushDevice", buildJsonObject { put("token", lastToken) })
                    }
                    call("registerPushDevice", buildJsonObject {
                        put("token", device.token)
                        put("platform", nativePushPlatform())
                    })
                    lastUid = uid; lastToken = device.token; lastRegisteredAt = now()
                    break
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    delay(retryMs)
                    retryMs = (retryMs * 2).coerceAtMost(60_000)
                }
            }
        }
    }
}

expect fun nativePushPlatform(): String
expect fun refreshNativePushRegistration()
expect fun requestNativePushPermission()
expect suspend fun deleteNativePushToken()

interface PushTokenDeletedCallback { fun complete() }
interface NativePushLauncher {
    fun refresh()
    fun requestPermission()
    fun deleteToken(completion: PushTokenDeletedCallback)
}
object NativePushBridge { var launcher: NativePushLauncher? = null }
