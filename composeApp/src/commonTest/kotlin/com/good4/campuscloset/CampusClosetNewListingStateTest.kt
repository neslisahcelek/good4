package com.good4.campuscloset

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CampusClosetNewListingStateTest {
    private val complete = CampusClosetNewListingState(
        category = "clothing", condition = "new", title = "Test mont",
        description = "Az kullanılmış mont", priceText = "100", photos = listOf(byteArrayOf(1))
    )

    @Test fun filledButShortDescriptionExplainsWhySubmissionIsDisabled() {
        val state = complete.copy(description = "test")
        assertFalse(state.canSubmit)
        assertEquals("Açıklama en az 10 karakter olmalı.", state.validationMessage)
        assertFalse(state.copy(description = "    test    ").canSubmit)
        val recorded = state.copy(description = "yeni gibi ")
        assertEquals(10, recorded.description.length)
        assertFalse(recorded.canSubmit)
        assertEquals("Açıklama en az 10 karakter olmalı.", recorded.validationMessage)
        assertTrue(state.copy(description = "1234567890").canSubmit)
    }

    @Test fun validPaidAndFreeListingsCanBeSubmitted() {
        assertTrue(complete.canSubmit)
        assertNull(complete.validationMessage)
        assertTrue(complete.copy(isFree = true, priceText = "").canSubmit)
        assertTrue(complete.copy(priceText = "100000").canSubmit)
    }

    @Test fun missingSelectionsAndInvalidPricesHaveVisibleReasons() {
        for (state in listOf(
            complete.copy(photos = emptyList()), complete.copy(category = null),
            complete.copy(condition = null), complete.copy(title = "a"),
            complete.copy(priceText = ""), complete.copy(priceText = "0"),
            complete.copy(priceText = "100001")
        )) {
            assertFalse(state.canSubmit)
            assertTrue(!state.validationMessage.isNullOrBlank())
        }
    }

    @Test fun submissionCannotBeRepeatedWhileSendingOrAfterSuccess() {
        assertFalse(complete.copy(submitting = true).canSubmit)
        assertFalse(complete.copy(submitted = true).canSubmit)
    }
}
