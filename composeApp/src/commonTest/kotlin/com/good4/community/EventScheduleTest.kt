package com.good4.community

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventScheduleTest {
    private val now = LocalDateTime(2026, 10, 4, 12, 0)
    private fun event(date: String, time: String, endDate: String = "", endTime: String = "") =
        CommunityEntryDto(kind = "event", date = date, time = time, endDate = endDate, endTime = endTime)

    @Test
    fun aNewStartSuggestsTwoHoursAndRollsOverMidnight() {
        assertEquals("2026-10-07" to "20:00", suggestedEventEnd("2026-10-07", "18:00"))
        assertEquals("2026-10-08" to "00:30", suggestedEventEnd("2026-10-07", "22:30"))
        val started = CommunityEntryDto(kind = "event").withStart("2026-10-07", "18:00")
        assertEquals("2026-10-07" to "20:00", started.endDate to started.endTime)
    }

    @Test
    fun movingTheStartKeepsTheEventLength() {
        val moved = event("2026-10-07", "18:00", "2026-10-08", "12:00").withStart("2026-10-09", "10:00")
        assertEquals("2026-10-10" to "04:00", moved.endDate to moved.endTime)
        val coupon = CommunityEntryDto(kind = "coupon").withStart("2026-12-01", "")
        assertEquals("" to "", coupon.endDate to coupon.endTime)
    }

    @Test
    fun scheduleValidationRejectsPastStartsAndInvertedOrLongRanges() {
        assertNull(validateEventSchedule(event("2026-10-07", "18:00", "2026-10-07", "20:00"), null, now))
        assertEquals("Başlangıç geçmiş bir zaman olamaz.", validateEventSchedule(event("2026-10-04", "11:00", "2026-10-04", "13:00"), null, now))
        assertEquals("Bitiş, başlangıçtan sonra olmalı.", validateEventSchedule(event("2026-10-07", "18:00", "2026-10-07", "18:00"), null, now))
        assertEquals("Etkinlik en fazla 14 gün sürebilir.", validateEventSchedule(event("2026-10-07", "18:00", "2026-10-21", "18:01"), null, now))
        assertEquals("Bitiş tarihini ve saatini seçin.", validateEventSchedule(event("2026-10-07", "18:00"), null, now))
    }

    @Test
    fun anOngoingEventCanBeEditedWhileItsStartStaysTheSame() {
        val ongoing = event("2026-10-04", "10:00", "2026-10-04", "14:00")
        assertNull(validateEventSchedule(ongoing.copy(endTime = "15:00"), ongoing, now))
        assertEquals("Başlangıç geçmiş bir zaman olamaz.", validateEventSchedule(ongoing.copy(time = "11:00"), ongoing, now))
    }

    @Test
    fun schedulesAreShownInTurkish() {
        assertEquals("7 Ekim Çarşamba · 18:00–20:00", formatEventSchedule(event("2026-10-07", "18:00", "2026-10-07", "20:00")))
        assertEquals("7 Ekim 18:00 – 9 Ekim 12:00", formatEventSchedule(event("2026-10-07", "18:00", "2026-10-09", "12:00")))
        assertEquals("7 Ekim Çarşamba · 18:00", formatEventSchedule(event("2026-10-07", "18:00")))
        assertEquals("2026-10-09", event("2026-10-07", "18:00", "2026-10-09", "12:00").lastDate())
        assertEquals("7 Eki · 18:00", formatEventShort(event("2026-10-07", "18:00", "2026-10-07", "20:00")))
    }

    @Test
    fun quickDurationsSetTheEndAndAreRecognisedAgain() {
        val start = event("2026-10-07", "22:30")
        assertEquals("2026-10-07" to "23:30", start.withDuration(EventDuration.ONE_HOUR).let { it.endDate to it.endTime })
        assertEquals("2026-10-08" to "01:30", start.withDuration(EventDuration.THREE_HOURS).let { it.endDate to it.endTime })
        assertEquals("2026-10-07" to "23:59", start.withDuration(EventDuration.ALL_DAY).let { it.endDate to it.endTime })
        assertEquals(EventDuration.TWO_HOURS, event("2026-10-07", "18:00", "2026-10-07", "20:00").matchingDuration())
        assertEquals(EventDuration.ALL_DAY, event("2026-10-07", "09:00", "2026-10-07", "23:59").matchingDuration())
        assertEquals(EventDuration.CUSTOM, event("2026-10-07", "18:00", "2026-10-09", "12:00").matchingDuration())
        assertNull(event("", "").matchingDuration())
    }

    @Test
    fun formErrorsArePerFieldAndInFieldOrder() {
        val empty = validateEventFields(CommunityEntryDto(kind = "event"), null, now)
        assertEquals(listOf(EventField.TITLE, EventField.CATEGORY, EventField.DESCRIPTION, EventField.START, EventField.LOCATION), empty.keys.toList())
        val complete = event("2026-10-07", "18:00", "2026-10-07", "20:00")
            .copy(title = "Film", categoryId = "culture-arts", description = "Gösterim", location = "Bahçe")
        assertEquals(emptyMap(), validateEventFields(complete, null, now))
        assertEquals(setOf(EventField.END), validateEventFields(complete.copy(endTime = "17:00"), null, now).keys)
        assertEquals(setOf(EventField.CAPACITY), validateEventFields(complete.copy(capacity = 100_001), null, now).keys)
    }

    @Test
    fun relativeDaysIncludeTodayTomorrowAndTheNextSevenDays() {
        val today = LocalDate(2026, 10, 4)
        assertEquals("Bugün", relativeEventDay("2026-10-04", today))
        assertEquals("Yarın", relativeEventDay("2026-10-05", today))
        assertEquals("2 gün sonra", relativeEventDay("2026-10-06", today))
        assertEquals("7 gün sonra", relativeEventDay("2026-10-11", today))
    }

    @Test
    fun relativeDaysHidePastDistantAndInvalidDates() {
        val today = LocalDate(2026, 10, 4)
        listOf("2026-10-03", "2026-10-12", "", "2026-02-30", "tarih").forEach { assertNull(relativeEventDay(it, today)) }
    }

    @Test
    fun relativeDaysWorkAcrossYearsAndLeapDays() {
        assertEquals("Yarın", relativeEventDay("2027-01-01", LocalDate(2026, 12, 31)))
        assertEquals("2 gün sonra", relativeEventDay("2028-03-01", LocalDate(2028, 2, 28)))
    }

    @Test
    fun theRelativeDayDisappearsOnceTheEventIsOver() {
        val today = event("2026-10-04", "09:00", "2026-10-04", "11:00")
        assertNull(today.relativeDayLabel(now))
        assertEquals("Bugün", today.copy(endTime = "14:00").relativeDayLabel(now))
        assertEquals("Yarın", event("2026-10-05", "18:00", "2026-10-05", "20:00").relativeDayLabel(now))
    }
}
