package com.good4.community.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Student community discovery: search and tabs, an upcoming-event highlight, the community list as the
 * main area, then coupons. Stateless; everything comes from [ui] and goes out through [actions].
 */
@Composable
fun CommunityDiscoverScreen(ui: CommunityDiscoverUi, actions: CommunityDiscoverActions, modifier: Modifier = Modifier) {
    Good4Scaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.community_discover_title),
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back), tint = TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
        ) {
            discoverContent(ui, actions)
        }
    }
}

private val GutterModifier = Modifier.padding(horizontal = DiscoverDefaults.Gutter)
private val RowPadding = PaddingValues(horizontal = DiscoverDefaults.Gutter)

internal fun LazyListScope.discoverContent(ui: CommunityDiscoverUi, actions: CommunityDiscoverActions) {
    item(key = "search") {
        DiscoverSearchField(ui.query, actions.onQueryChange, GutterModifier)
    }
    item(key = "tabs") {
        DiscoverTabs(
            followedOnly = ui.followedOnly,
            enabled = !ui.loading,
            onSelect = actions.onFollowedOnlyChange,
            modifier = GutterModifier.padding(top = 12.dp)
        )
    }
    // The game-code entry stays reachable in every state: a student at a live event should never be blocked by an empty feed.
    item(key = "quiz-pin") {
        QuizPinCard(onJoin = actions.onJoinWithPin, modifier = GutterModifier.padding(top = 16.dp))
    }
    item(key = "tabs-gap") { Spacer(Modifier.height(DiscoverDefaults.SectionGap - 4.dp)) }

    when (ui.mode) {
        DiscoverMode.Loading -> item(key = "loading") { DiscoverSkeleton(GutterModifier.animateItem()) }
        DiscoverMode.Error -> item(key = "error") { LoadErrorState(actions.onRetry, GutterModifier.animateItem()) }
        DiscoverMode.SearchEmpty -> item(key = "search-empty") {
            SearchEmptyState(ui.query, onClear = { actions.onQueryChange("") }, modifier = GutterModifier.animateItem())
        }
        DiscoverMode.FollowedEmpty -> item(key = "followed-empty") {
            FollowedEmptyState(onDiscover = { actions.onFollowedOnlyChange(false) }, modifier = GutterModifier.animateItem())
        }
        DiscoverMode.AllEmpty -> item(key = "all-empty") { AllEmptyState(GutterModifier.animateItem()) }
        DiscoverMode.SearchResults -> communitiesSection(ui, actions)
        DiscoverMode.Content -> {
            eventsSection(ui, actions)
            communitiesSection(ui, actions)
            couponsSection(ui, actions)
        }
    }
}

private fun LazyListScope.eventsSection(ui: CommunityDiscoverUi, actions: CommunityDiscoverActions) {
    item(key = "events-heading") {
        DiscoverSectionHeading(stringResource(Res.string.community_discover_events_title), GutterModifier.animateItem())
    }
    item(key = "events-heading-gap") { Spacer(Modifier.height(DiscoverDefaults.HeadingGap)) }
    val highlight = ui.events.firstOrNull()
    if (highlight == null) {
        item(key = "events-empty") { EventsEmptyNote(GutterModifier.animateItem()) }
    } else {
        item(key = "event-highlight-${highlight.entry.id}") {
            HighlightEventCard(highlight, ui.today, onClick = { actions.onEventClick(highlight) }, modifier = GutterModifier.animateItem())
        }
        val rest = ui.events.drop(1)
        // A single event stays a single card; a strip appears only when there is more to swipe.
        if (rest.isNotEmpty()) {
            item(key = "events-row") {
                LazyRow(
                    modifier = Modifier.padding(top = DiscoverDefaults.CardGap).animateItem(),
                    contentPadding = RowPadding,
                    horizontalArrangement = Arrangement.spacedBy(DiscoverDefaults.CardGap)
                ) {
                    items(rest, key = { it.entry.id }) { event ->
                        CompactEventCard(event, onClick = { actions.onEventClick(event) })
                    }
                }
            }
        }
    }
    item(key = "events-gap") { Spacer(Modifier.height(DiscoverDefaults.SectionGap)) }
}

private fun LazyListScope.communitiesSection(ui: CommunityDiscoverUi, actions: CommunityDiscoverActions) {
    item(key = "communities-heading") {
        DiscoverSectionHeading(
            stringResource(Res.string.community_discover_communities_title),
            GutterModifier.animateItem(),
            trailing = ui.communities.size.takeIf { it > 0 }?.let { stringResource(Res.string.community_discover_count, it) }
        )
    }
    item(key = "communities-heading-gap") { Spacer(Modifier.height(DiscoverDefaults.HeadingGap)) }
    if (ui.communities.isEmpty()) {
        item(key = "communities-empty") { CommunitiesEmptyState(GutterModifier.animateItem()) }
    } else {
        ui.communities.forEachIndexed { index, community ->
            item(key = "community-${community.id}") {
                DiscoverCommunityCard(
                    community,
                    onClick = { actions.onCommunityClick(community) },
                    modifier = GutterModifier
                        .padding(top = if (index == 0) 0.dp else DiscoverDefaults.CardGap - 2.dp)
                        .animateItem()
                )
            }
        }
    }
    item(key = "communities-gap") { Spacer(Modifier.height(DiscoverDefaults.SectionGap)) }
}

private fun LazyListScope.couponsSection(ui: CommunityDiscoverUi, actions: CommunityDiscoverActions) {
    item(key = "coupons-heading") {
        DiscoverSectionHeading(stringResource(Res.string.community_discover_coupons_title), GutterModifier.animateItem())
    }
    item(key = "coupons-heading-gap") { Spacer(Modifier.height(DiscoverDefaults.HeadingGap)) }
    if (ui.coupons.isEmpty()) {
        item(key = "coupons-empty") { CouponsEmptyNote(GutterModifier.animateItem()) }
    } else {
        item(key = "coupons-row") {
            LazyRow(
                modifier = Modifier.animateItem(),
                contentPadding = RowPadding,
                horizontalArrangement = Arrangement.spacedBy(DiscoverDefaults.CardGap)
            ) {
                items(ui.coupons, key = { it.entry.id }) { coupon ->
                    DiscoverCouponCard(coupon, ui.today, onClick = { actions.onCouponClick(coupon) })
                }
            }
        }
    }
}
