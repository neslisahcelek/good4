package com.good4.core.data.repository

import com.good4.core.util.AppEnvironment
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

expect fun firebaseCacheUserId(): String?

/** One session-scoped value; concurrent readers share the same fetch. */
class ReadCache<T>(private val lifetimeSeconds: Long) {
    private val mutex = Mutex()
    private var key = ""
    private var fetchedAt = 0L
    private var value: T? = null
    private var hasValue = false
    suspend fun load(suffix: String = "", cacheable: (T) -> Boolean = { true }, forceRefresh: Boolean = false, fetch: suspend () -> T): T = mutex.withLock {
        val sessionKey = "${AppEnvironment.firebaseProjectId}:${firebaseCacheUserId()}:$generation:$suffix"
        val now = Clock.System.now().epochSeconds
        if (!forceRefresh && hasValue && key == sessionKey && now - fetchedAt in 0 until lifetimeSeconds) {
            @Suppress("UNCHECKED_CAST")
            return@withLock value as T
        }
        value = null; hasValue = false
        val result = fetch()
        if (cacheable(result) && sessionKey == "${AppEnvironment.firebaseProjectId}:${firebaseCacheUserId()}:$generation:$suffix") {
            value = result; hasValue = true; key = sessionKey; fetchedAt = now
        }
        result
    }
    companion object {
        private var generation = 0L
        fun invalidateSession() { generation++ }
    }
}
