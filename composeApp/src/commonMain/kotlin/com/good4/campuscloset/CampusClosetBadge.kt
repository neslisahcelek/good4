package com.good4.campuscloset

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock

/** Unread count shown on the home tile; refreshed by every Kampüs Dolabı load. */
class CampusClosetBadge {
    private val _unread = MutableStateFlow(0)
    val unread = _unread.asStateFlow()
    private var lastSummaryAt = 0L

    fun update(count: Int) {
        _unread.update { count }
    }

    /** The home screen asks at most every two minutes so returning home stays free. */
    fun shouldRefresh(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastSummaryAt < 120_000) return false
        lastSummaryAt = now
        return true
    }
}
