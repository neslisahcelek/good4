package com.good4.campuscloset

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class CampusClosetFeedCacheTest {
    @Test fun theSavedPageShowsAtOnceAndTheServerPageReplacesIt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = CompletableDeferred<MarketFeed>()
            val source = object : CampusClosetFeedDataSource {
                override suspend fun feed(category: String?, before: String?, query: String?) = server.await()
                override suspend fun setFavorite(listingId: String, saved: Boolean) = Unit
                override suspend fun acceptTerms(version: Int) = Unit
                override fun cachedFeed() = MarketFeed(MarketMe(), listOf(MarketListing("cached")), "cachedCursor")
            }
            val viewModel = CampusClosetFeedViewModel(source)
            viewModel.load()
            runCurrent()
            assertEquals(listOf("cached"), viewModel.state.value.listings.map { it.id })
            assertFalse(viewModel.state.value.isLoading)

            server.complete(MarketFeed(MarketMe(), listOf(MarketListing("fresh")), "freshCursor"))
            runCurrent()
            assertEquals(listOf("fresh"), viewModel.state.value.listings.map { it.id })
            assertEquals("freshCursor", viewModel.state.value.nextBefore)
        } finally { Dispatchers.resetMain() }
    }
}
