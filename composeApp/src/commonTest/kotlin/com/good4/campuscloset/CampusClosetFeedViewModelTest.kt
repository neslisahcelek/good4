package com.good4.campuscloset

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class CampusClosetFeedViewModelTest {
    @Test fun aLatePageCannotAppendListingsOrReplaceCursorAfterCategoryChanges() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val oldPage = CompletableDeferred<MarketFeed>()
            val source = object : CampusClosetFeedDataSource {
                override suspend fun feed(category: String?, before: String?, query: String?): MarketFeed {
                    if (before != null) return withContext(NonCancellable) { oldPage.await() }
                    return if (category == "books") MarketFeed(MarketMe(), listOf(MarketListing("book")), "booksCursor")
                    else MarketFeed(MarketMe(), listOf(MarketListing("coat")), "oldCursor")
                }
                override suspend fun setFavorite(listingId: String, saved: Boolean) = Unit
                override suspend fun acceptTerms(version: Int) = Unit
            }
            val viewModel = CampusClosetFeedViewModel(source)
            viewModel.load()
            runCurrent()
            viewModel.loadMore()
            runCurrent()
            viewModel.selectCategory("books")
            runCurrent()
            oldPage.complete(MarketFeed(MarketMe(), listOf(MarketListing("stale")), "staleCursor"))
            runCurrent()
            assertEquals(listOf("book"), viewModel.state.value.listings.map { it.id })
            assertEquals("booksCursor", viewModel.state.value.nextBefore)
            assertFalse(viewModel.state.value.isLoadingMore)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun aPageFailureLeavesTheCursorAvailableAndShowsAnError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val source = object : CampusClosetFeedDataSource {
                override suspend fun feed(category: String?, before: String?, query: String?): MarketFeed {
                    if (before != null) error("network")
                    return MarketFeed(MarketMe(), listOf(MarketListing("coat")), "cursor")
                }
                override suspend fun setFavorite(listingId: String, saved: Boolean) = Unit
                override suspend fun acceptTerms(version: Int) = Unit
            }
            val viewModel = CampusClosetFeedViewModel(source)
            viewModel.load()
            runCurrent()
            viewModel.loadMore()
            runCurrent()
            assertEquals("cursor", viewModel.state.value.nextBefore)
            assertNotNull(viewModel.state.value.message)
            assertFalse(viewModel.state.value.isLoadingMore)
        } finally { Dispatchers.resetMain() }
    }
}
