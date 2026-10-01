package com.good4.schedule

import com.good4.schedule.domain.ClassSchedules
import com.good4.schedule.domain.ScheduleDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MaritimeSchedulesTest {
    private fun schedule(year: String) = assertNotNull(
        ClassSchedules.find(ClassSchedules.MARITIME_FACULTY, ClassSchedules.MARITIME_BUSINESS_DEPARTMENT, year)
    )

    @Test
    fun hourCountsMatchTheSourceTable() {
        // One entry per lesson hour, counted from the faculty's 2026-2027 fall table.
        assertEquals(22, schedule(ClassSchedules.FIRST_YEAR).entries.size)
        assertEquals(27, schedule(ClassSchedules.SECOND_YEAR).entries.size)
        assertEquals(30, schedule(ClassSchedules.THIRD_YEAR).entries.size)
        assertEquals(27, schedule(ClassSchedules.FOURTH_YEAR).entries.size)
    }

    @Test
    fun morningAndAfternoonBlocksUseTheTableHours() {
        val monday = schedule(ClassSchedules.SECOND_YEAR).entries.filter { it.day == ScheduleDay.MONDAY }
        assertEquals(listOf("09:30", "10:30", "11:30", "13:30", "14:30", "15:30"), monday.map { it.startTime })
        assertEquals("DİY 225", monday.last().courseCode)
        val turkish = schedule(ClassSchedules.FIRST_YEAR).entries.filter { it.courseCode == "TDB 101" }
        assertEquals(listOf("14:30", "15:30", "16:30"), turkish.map { it.startTime })
    }
}
