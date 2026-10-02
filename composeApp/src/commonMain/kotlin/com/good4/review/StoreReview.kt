package com.good4.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

const val REVIEW_CONTENT_WAIT_MS = 10_000L
const val REVIEW_IDLE_WAIT_MS = 3_000L
const val REVIEW_SESSION_GAP_MS = 30 * 60 * 1000L
const val REVIEW_COOLDOWN_MS = 90 * 24 * 60 * 60 * 1000L

enum class ReviewFeature { DINING_MENU, CLASS_SCHEDULE, COMMUNITIES }

@Serializable
data class ReviewHistory(
    val sessions: Int = 0,
    val lastRequestedVersion: String? = null,
    val lastRequestedAt: Long? = null
) {
    fun isEligible(version: String, now: Long): Boolean =
        sessions >= 3 && version.isNotBlank() && lastRequestedVersion != version &&
            (lastRequestedAt == null || now - lastRequestedAt >= REVIEW_COOLDOWN_MS)
}

interface ReviewStorage {
    fun read(): String?
    fun write(value: String)
}

class DeviceReviewStorage : ReviewStorage {
    override fun read() = loadReviewHistory()
    override fun write(value: String) = saveReviewHistory(value)
}

expect fun loadReviewHistory(): String?
expect fun saveReviewHistory(value: String)
expect fun reviewAppVersion(): String
expect fun automaticReviewEnabled(): Boolean
expect suspend fun openReviewStore(): StoreOpenResult

enum class StoreOpenResult { OPENED, NOT_CONFIGURED, FAILED }

/** approve is checked immediately before launching, including after async Play preparation. */
interface StoreReviewLauncher {
    fun requestReview(approve: () -> Boolean)
}

@Composable
expect fun rememberStoreReviewLauncher(): StoreReviewLauncher

interface NativeReviewApproval { fun approve(): Boolean }
interface NativeReviewLauncher { fun launch(approval: NativeReviewApproval) }
object StoreReviewBridge { var launcher: NativeReviewLauncher? = null }

/** One device-wide history, independent of navigation, account changes and Activity recreation. */
class StoreReviewCoordinator(
    private val storage: ReviewStorage,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private var history = runCatching {
        storage.read()?.let { Json.decodeFromString<ReviewHistory>(it) }
    }.getOrNull() ?: ReviewHistory()
    private val _foreground = MutableStateFlow(false)
    val foreground = _foreground.asStateFlow()
    private val _modalCount = MutableStateFlow(0)
    val modalCount = _modalCount.asStateFlow()
    private var backgroundAt: Long? = null
    private var newSession = true
    private var sessionCounted = false

    fun onForeground() {
        if (_foreground.value) return
        if (backgroundAt?.let { now() - it >= REVIEW_SESSION_GAP_MS } == true) newSession = true
        if (newSession) {
            sessionCounted = false
            newSession = false
        }
        _foreground.value = true
    }

    fun onBackground() {
        if (!_foreground.value) return
        backgroundAt = now()
        _foreground.value = false
    }

    fun onStudentHomeEntered() {
        if (!_foreground.value || sessionCounted) return
        sessionCounted = true
        persist(history.copy(sessions = history.sessions.coerceIn(0, 999_999) + 1))
    }

    fun isEligible(version: String) = history.isEligible(version, now())

    fun recordAttempt(version: String): Boolean {
        if (!_foreground.value || _modalCount.value != 0 || !isEligible(version)) return false
        return persist(history.copy(lastRequestedVersion = version, lastRequestedAt = now()))
    }

    fun onManualStoreOpened(version: String) {
        // Opening the store signals intent, never proof that a review was submitted.
        persist(history.copy(lastRequestedVersion = version, lastRequestedAt = now()))
    }

    fun blockModal() { _modalCount.value += 1 }
    fun unblockModal() { _modalCount.value = (_modalCount.value - 1).coerceAtLeast(0) }

    private fun persist(value: ReviewHistory): Boolean = runCatching {
        storage.write(Json.encodeToString(value))
        history = value
    }.isSuccess
}

@Composable
fun ReviewModalBlocker(visible: Boolean) {
    val coordinator: StoreReviewCoordinator = koinInject()
    DisposableEffect(visible, coordinator) {
        if (visible) coordinator.blockModal()
        onDispose { if (visible) coordinator.unblockModal() }
    }
}
