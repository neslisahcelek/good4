package com.good4.community

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventFiltersTest {
    private fun event(id: String, communityId: String = "community-a", categoryId: String = "technology") = CommunityFeaturedEvent(
        Community(communityId, CommunityDto(name = communityId)),
        CommunityEntry(id, CommunityEntryDto(categoryId = categoryId)),
    )

    @Test
    fun categoryAndFollowingAreAppliedBeforeTheTenCardLimit() {
        val candidates = (1..12).map { event("early-$it", "not-followed", "culture-arts") } +
            event("matching-late", "followed", "technology") + event("wrong-community", "other", "technology")
        val result = filterFeaturedCommunityEvents(candidates, "technology", true, setOf("followed"))
        assertEquals(listOf("matching-late"), result.map { it.entry.id })
        assertEquals(10, filterFeaturedCommunityEvents(candidates, "", false, emptySet()).size)
    }

    @Test
    fun missingCategoriesStayInAllAndHaveTheirOwnFilter() {
        val candidates = listOf(event("legacy", categoryId = ""), event("technology"), event("unknown", categoryId = "future-id"))
        assertEquals(3, filterFeaturedCommunityEvents(candidates, "", false, emptySet()).size)
        assertEquals(listOf("legacy", "unknown"), filterFeaturedCommunityEvents(candidates, EventCategory.UNCATEGORIZED, false, emptySet()).map { it.entry.id })
        assertEquals("Kategori belirtilmemiş", EventCategory.labelFor(""))
    }

    @Test
    fun noFollowsOrNoMatchingCategoryProduceEmptyResults() {
        assertTrue(filterFeaturedCommunityEvents(listOf(event("event")), "", true, emptySet()).isEmpty())
        assertTrue(filterFeaturedCommunityEvents(listOf(event("event")), "volunteering", false, emptySet()).isEmpty())
        assertFalse(matchesEventCategory("", "technology"))
    }
}
