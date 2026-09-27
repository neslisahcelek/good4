package com.good4.community

/** IDs match the canonical V2 event category contract; labels are presentation only. */
enum class EventCategory(val id: String, val label: String) {
    ACADEMIC_SCIENCE("academic-science", "Akademik ve Bilim"),
    CAREER_ENTREPRENEURSHIP("career-entrepreneurship", "Kariyer ve Girişimcilik"),
    TECHNOLOGY("technology", "Teknoloji"),
    CULTURE_ARTS("culture-arts", "Kültür ve Sanat"),
    SPORTS_NATURE("sports-nature", "Spor ve Doğa"),
    SOCIAL_ENTERTAINMENT("social-entertainment", "Sosyal ve Eğlence"),
    VOLUNTEERING("volunteering", "Gönüllülük"),
    OTHER("other", "Diğer");

    companion object {
        const val UNCATEGORIZED = "uncategorized"
        fun fromId(id: String): EventCategory? = entries.firstOrNull { it.id == id }
        fun labelFor(id: String): String = fromId(id)?.label ?: "Kategori belirtilmemiş"
    }
}

internal fun matchesEventCategory(eventCategoryId: String, selectedCategoryId: String): Boolean = when (selectedCategoryId) {
    "" -> true
    EventCategory.UNCATEGORIZED -> EventCategory.fromId(eventCategoryId) == null
    else -> eventCategoryId == selectedCategoryId
}

/** Apply both filters to all eligible candidates before limiting the carousel. */
internal fun filterFeaturedCommunityEvents(
    candidates: List<CommunityFeaturedEvent>, categoryId: String,
    followedOnly: Boolean, followedCommunityIds: Set<String>,
): List<CommunityFeaturedEvent> = candidates.asSequence()
    .filter { matchesEventCategory(it.entry.data.categoryId, categoryId) }
    .filter { !followedOnly || it.community.id in followedCommunityIds }
    .take(10)
    .toList()
