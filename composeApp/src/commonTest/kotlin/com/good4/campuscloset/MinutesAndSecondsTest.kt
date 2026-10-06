package com.good4.campuscloset

import kotlin.test.Test
import kotlin.test.assertEquals

class MinutesAndSecondsTest {
    @Test
    fun resendCountdownIsShownAsMinutesAndSeconds() {
        assertEquals("5:00", minutesAndSeconds(300))
        assertEquals("1:05", minutesAndSeconds(65))
        assertEquals("0:09", minutesAndSeconds(9))
    }
}
