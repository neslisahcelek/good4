package com.good4.campuscloset

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CampusClosetPriceEditStateTest {
    @Test fun priceChangesMustBeValidAndDifferentFromTheCurrentPrice() {
        val state = CampusClosetPriceEditState("listing", 100)
        assertFalse(state.canSave)
        for (text in listOf("", "0", "100001", "invalid")) assertFalse(state.copy(priceText = text).canSave)
        assertTrue(state.copy(priceText = "100000").canSave)
        assertTrue(state.copy(isFree = true).canSave)
        assertEquals(0, state.copy(isFree = true).price)
    }

    @Test fun freeListingsNeedAPositivePriceToBecomePaid() {
        val state = CampusClosetPriceEditState("listing", 0)
        assertFalse(state.canSave)
        assertFalse(state.copy(isFree = false).canSave)
        val paid = state.copy(isFree = false).withPrice("a1b20")
        assertEquals("120", paid.priceText)
        assertTrue(paid.canSave)
    }
}
