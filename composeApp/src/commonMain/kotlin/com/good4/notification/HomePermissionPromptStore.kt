package com.good4.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

@Serializable
data class HomePermissionPromptHistory(
    val sessions: Int = 0,
    val analyticsDismissed: Boolean = false
)

expect fun loadHomePermissionPromptHistory(): String?
expect fun saveHomePermissionPromptHistory(value: String)

/** Installation-wide suggestions; short background trips and navigation don't count as new sessions. */
class HomePermissionPromptStore(
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private val mutable = MutableStateFlow(runCatching {
        loadHomePermissionPromptHistory()?.let { Json.decodeFromString<HomePermissionPromptHistory>(it) }
    }.getOrNull() ?: HomePermissionPromptHistory())
    val history = mutable.asStateFlow()
    private var sessionCounted = false
    private var backgroundAt: Long? = null
    var analyticsPromptShownThisSession = false
        private set

    fun onForeground() {
        if (backgroundAt?.let { now() - it >= 30 * 60 * 1000L } == true) {
            sessionCounted = false
            analyticsPromptShownThisSession = false
        }
        backgroundAt = null
    }

    fun onBackground() { backgroundAt = now() }

    fun onStudentHomeEntered() {
        if (sessionCounted) return
        sessionCounted = true
        persist(mutable.value.copy(sessions = mutable.value.sessions.coerceIn(0, 999_999) + 1))
    }

    fun dismissAnalytics(): Boolean = persist(mutable.value.copy(analyticsDismissed = true))
    fun markAnalyticsPromptShown() { analyticsPromptShownThisSession = true }

    private fun persist(value: HomePermissionPromptHistory): Boolean = runCatching {
        saveHomePermissionPromptHistory(Json.encodeToString(value))
        mutable.update { value }
    }.isSuccess
}
