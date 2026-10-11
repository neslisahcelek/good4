package com.good4.community

import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.*
import kotlinx.datetime.Instant
import kotlin.test.*

class CommunityCouponStateTest {
    private val now = Instant.parse("2026-10-11T12:00:00Z")
    @Test
    fun countdownUsesDeadlineAndSurvivesTimeAwayFromScreen() {
        val expiry = Instant.parse("2026-10-11T12:10:00Z")
        assertEquals(600, couponSecondsRemaining(expiry, now))
        assertEquals(1, couponSecondsRemaining(expiry, Instant.parse("2026-10-11T12:09:59.999Z")))
        assertEquals(0, couponSecondsRemaining(expiry, expiry))
        assertEquals(0, couponSecondsRemaining(expiry, Instant.parse("2026-10-12T12:00:00Z")))
    }
    @Test
    fun couponValidityIncludesTheStartAndExcludesTheEnd() {
        val coupon = V2CommunityCouponDto(startsAt = 100, endsAt = 200)
        assertFalse(coupon.isCurrentlyValid(99))
        assertTrue(coupon.isCurrentlyValid(100))
        assertTrue(coupon.isCurrentlyValid(199))
        assertFalse(coupon.isCurrentlyValid(200))
        assertFalse(coupon.isCurrentlyValid(201))
    }

    @Test
    fun eachServerOutcomeHasADistinctResourceMessage() {
        assertNull(couponOutcomeMessage("issued"))
        assertNull(couponOutcomeMessage("already_issued"))
        for ((outcome, resource) in listOf(
            "already_used_today" to Res.string.community_coupon_used_today,
            "limit_reached" to Res.string.community_coupon_limit_reached,
            "daily_limit_reached" to Res.string.community_coupon_daily_limit_reached,
            "coupon_inactive" to Res.string.community_coupon_inactive,
            "unexpected" to Res.string.community_coupon_code_error
        )) {
            assertEquals(resource, (couponOutcomeMessage(outcome) as UiText.StringResourceId).id)
        }
    }

    @Test
    fun deadlineCountsDownInTheLastDaysAndShowsTheDateBefore() {
        val today = kotlinx.datetime.LocalDate.parse("2026-10-11")
        assertEquals(CouponDeadline.Today, couponDeadline("2026-10-11", today))
        assertEquals(CouponDeadline.Tomorrow, couponDeadline("2026-10-12", today))
        assertEquals(CouponDeadline.DaysLeft(3), couponDeadline("2026-10-14", today))
        assertEquals(CouponDeadline.OnDate(21, 10), couponDeadline("2026-10-21", today))
        assertNull(couponDeadline("", today))
    }
}
