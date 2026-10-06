package com.good4.campuscloset

import kotlinx.serialization.Serializable

/** The last first page of Kampüs Dolabı, kept on the device so the screen opens instantly. */
@Serializable
data class CachedCampusClosetFeed(val uid: String, val savedAtMillis: Long, val feed: MarketFeed)

/** A cached page older than this is not shown; the screen waits for the server instead. */
const val CAMPUS_CLOSET_FEED_CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

expect fun loadCampusClosetFeedCache(): String?

expect fun saveCampusClosetFeedCache(value: String?)
