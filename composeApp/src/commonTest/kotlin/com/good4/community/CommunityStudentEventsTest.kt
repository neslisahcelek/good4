package com.good4.community

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CommunityStudentEventsTest {
    private val now = LocalDateTime(2026, 10, 4, 12, 0)
    private fun event(id: String, date: String, endTime: String = "20:00", status: String = "published", kind: String = "event") =
        CommunityEntry(id, CommunityEntryDto(kind = kind, date = date, time = "18:00", endDate = date, endTime = endTime, status = status))

    @Test
    fun upcomingUsesTheEndTimeAndSortsOldestFirst() {
        val entries = listOf(event("later", "2026-10-08"), event("ended", "2026-10-04", "11:59"), event("ongoing", "2026-10-04"))
        assertEquals(listOf("ongoing", "later"), studentEventsFor(entries, StudentEventFilter.UPCOMING, emptySet(), now).map { it.id })
    }

    @Test
    fun pastSortsNewestFirstAndExcludesCouponsAndUnpublishedEvents() {
        val entries = listOf(event("old", "2026-10-01"), event("recent", "2026-10-03"), event("draft", "2026-10-02", status = "draft"),
            event("cancelled", "2026-10-02", status = "cancelled"), event("coupon", "2026-10-02", kind = "coupon"))
        assertEquals(listOf("recent", "old"), studentEventsFor(entries, StudentEventFilter.PAST, emptySet(), now).map { it.id })
    }

    @Test
    fun registrationsIncludePastAndUpcomingPublishedEventsOnly() {
        val entries = listOf(event("future", "2026-10-08"), event("past", "2026-10-01"), event("other", "2026-10-02"),
            event("cancelled", "2026-10-02", status = "cancelled"))
        assertEquals(listOf("past", "future"), studentEventsFor(entries, StudentEventFilter.REGISTERED,
            setOf("future", "past", "cancelled"), now).map { it.id })
    }

    @Test
    fun registeredStatusWinsOverEndedAndFull() {
        val data = event("event", "2026-10-01").data.copy(capacity = 1, registrationCount = 1)
        assertEquals(StudentEventStatus.REGISTERED, studentEventStatus(data, true, now))
        assertEquals(StudentEventStatus.ENDED, studentEventStatus(data, false, now))
    }

    @Test
    fun fullStatusRequiresAPositiveCapacityAndEnoughRegistrations() {
        val data = event("event", "2026-10-08").data
        assertEquals(StudentEventStatus.FULL, studentEventStatus(data.copy(capacity = 2, registrationCount = 2), false, now))
        assertNull(studentEventStatus(data.copy(capacity = 2, registrationCount = 1), false, now))
        assertNull(studentEventStatus(data.copy(capacity = 0, registrationCount = 100), false, now))
    }

    @Test
    fun cancellingTheLastSeatMakesTheEventAvailableAgain() {
        val entry = event("event", "2026-10-08").copy(data = event("event", "2026-10-08").data.copy(capacity = 1, registrationCount = 1))
        val state = CommunityState(entries = listOf(entry), registeredEventIds = setOf(entry.id), registrationLoadingIds = setOf(entry.id))
        val cancelled = state.withStudentRegistration(entry.id, false)
        assertEquals(emptySet(), cancelled.registeredEventIds)
        assertEquals(emptySet(), cancelled.registrationLoadingIds)
        assertEquals(0, cancelled.entries.single().data.registrationCount)
        assertNull(studentEventStatus(cancelled.entries.single().data, false, now))
        val registered = cancelled.withStudentRegistration(entry.id, true)
        assertEquals(1, registered.entries.single().data.registrationCount)
        assertEquals(StudentEventStatus.REGISTERED, studentEventStatus(registered.entries.single().data, true, now))
    }

    @Test
    fun registrationUpdatesDoNotDoubleCountOrChangeOtherEvents() {
        val entry = event("event", "2026-10-08").copy(data = event("event", "2026-10-08").data.copy(registrationCount = 4))
        val other = event("other", "2026-10-09")
        val state = CommunityState(entries = listOf(entry, other), registeredEventIds = setOf(entry.id))
        assertEquals(state.entries, state.withStudentRegistration(entry.id, true).entries)
        assertEquals(other, state.withStudentRegistration(entry.id, false).entries.last())
    }

    @Test
    fun successFeedbackRequiresTheVisibleEventAndClearsOnCancellation() {
        val state = CommunityState(entries = listOf(event("event", "2026-10-08")))
        assertNull(state.withStudentRegistration("event", true).justRegisteredEntryId)
        val registered = state.withStudentRegistration("event", true, showFeedback = true)
        assertEquals("event", registered.justRegisteredEntryId)
        assertNull(registered.withStudentRegistration("event", false, showFeedback = true).justRegisteredEntryId)
    }
}
