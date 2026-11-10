package com.good4.community.discover

import com.good4.community.Community
import com.good4.community.CommunityFeaturedEvent
import kotlinx.datetime.LocalDate

/**
 * What the student community discovery screen shows. Filled from the existing screen state;
 * this type adds no data of its own (no member counts, popularity or similar).
 */
data class CommunityDiscoverUi(
    val query: String = "",
    val followedOnly: Boolean = false,
    val loading: Boolean = false,
    val loadFailed: Boolean = false,
    /** Events already filtered and ordered by the existing logic; the first one becomes the highlight. */
    val events: List<CommunityFeaturedEvent> = emptyList(),
    val coupons: List<CommunityFeaturedEvent> = emptyList(),
    /** Communities after the existing tab and search filters. */
    val communities: List<Community> = emptyList(),
    val today: LocalDate
) {
    private val searching: Boolean get() = query.isNotBlank()

    /** Exactly one screen mode, in the brief's priority order so states never overlap. */
    val mode: DiscoverMode
        get() = when {
            loading -> DiscoverMode.Loading
            loadFailed -> DiscoverMode.Error
            searching -> if (communities.isEmpty()) DiscoverMode.SearchEmpty else DiscoverMode.SearchResults
            followedOnly && communities.isEmpty() -> DiscoverMode.FollowedEmpty
            events.isEmpty() && coupons.isEmpty() && communities.isEmpty() -> DiscoverMode.AllEmpty
            else -> DiscoverMode.Content
        }
}

enum class DiscoverMode { Loading, Error, SearchEmpty, SearchResults, FollowedEmpty, AllEmpty, Content }

class CommunityDiscoverActions(
    val onBack: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onFollowedOnlyChange: (Boolean) -> Unit = {},
    val onEventClick: (CommunityFeaturedEvent) -> Unit = {},
    val onCouponClick: (CommunityFeaturedEvent) -> Unit = {},
    val onCommunityClick: (Community) -> Unit = {},
    val onRetry: () -> Unit = {},
    /** A 6-digit game code typed by the student; the existing join flow resolves it after approval. */
    val onJoinWithPin: (String) -> Unit = {}
)
