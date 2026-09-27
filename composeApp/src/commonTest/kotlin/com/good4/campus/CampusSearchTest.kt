package com.good4.campus

import com.good4.campus.domain.CampusPointCategory
import com.good4.campus.domain.CampusPoints
import com.good4.campus.domain.search
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CampusSearchTest {
    @Test
    fun matchesWithoutTurkishCharacters() {
        assertEquals(listOf("Mühendislik Fakültesi"), CampusPoints.search("muhendislik").map { it.name })
        assertTrue(CampusPoints.search("IKTISADI").any { it.name == "İktisadi ve İdari Bilimler Fakültesi" })
    }

    @Test
    fun matchesCategoryAndEveryWord() {
        assertTrue(CampusPoints.search("atm").all { it.category == CampusPointCategory.ATM })
        assertTrue(CampusPoints.search("atm").isNotEmpty())
        assertEquals(listOf("Hukuk Fakültesi"), CampusPoints.search("hukuk fak").map { it.name })
    }

    @Test
    fun blankQueryReturnsNothing() {
        assertTrue(CampusPoints.search("  ").isEmpty())
    }
}
