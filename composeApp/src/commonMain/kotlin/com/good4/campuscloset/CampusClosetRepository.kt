package com.good4.campuscloset

import com.good4.auth.data.repository.AuthRepository
import com.good4.core.network.callV2Function
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface CampusClosetFeedDataSource {
    suspend fun feed(category: String?, before: String?, query: String? = null): MarketFeed
    suspend fun setFavorite(listingId: String, saved: Boolean)
    suspend fun acceptTerms(version: Int)
    /** The last default page saved on this device for the signed-in student, if any. */
    fun cachedFeed(): MarketFeed? = null
}

/** A home-screen prefetch is reused by the feed if it started this recently. */
private const val PREFETCH_REUSE_MS = 60_000L
/** The home screen fetches the whole first page at most this often; in between it asks only for the badge. */
private const val PREFETCH_INTERVAL_MS = 10 * 60_000L
/** How long the screen waits for a running prefetch before calling the server itself. */
private const val PREFETCH_JOIN_TIMEOUT_MS = 8_000L

class CampusClosetRepository(
    private val auth: AuthRepository,
    private val badge: CampusClosetBadge
) : CampusClosetFeedDataSource {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    val currentUid: String? get() = auth.currentUser?.uid
    private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var prefetch: Deferred<MarketFeed>? = null
    private var prefetchUid: String? = null
    private var prefetchStartedAt = 0L
    private fun now() = Clock.System.now().toEpochMilliseconds()

    private suspend inline fun <reified T> call(name: String, data: JsonObject = JsonObject(emptyMap())): T =
        json.decodeFromJsonElement(callV2Function(name, data))

    suspend fun summary(): MarketMe = call<MarketSummary>("getMarketSummary").me.also { badge.update(it.unreadCount) }

    /** [query] is searched on the server across every listing, not just the loaded page. */
    override suspend fun feed(category: String?, before: String?, query: String?): MarketFeed {
        val uid = currentUid ?: throw CancellationException("Campus Closet session ended")
        val firstPage = category == null && before == null && query.isNullOrBlank()
        if (firstPage) {
            // Opening Kampüs Dolabı right after the home screen joins the request already on its way;
            // if it fails or takes too long, the screen asks the server itself.
            val pending = prefetch
            if (pending?.isActive == true && prefetchUid == uid && now() - prefetchStartedAt < PREFETCH_REUSE_MS) {
                // Share the running request once. Later resumes must see verification/favorite changes.
                prefetch = null
                val feed = withTimeoutOrNull(PREFETCH_JOIN_TIMEOUT_MS) {
                    try {
                        pending.await()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                }
                ensureCurrentAccount(uid)
                if (feed != null) return feed
            }
        }
        return fetchFeed(category, before, query, uid)
    }

    /** Always a real server call; the prefetch uses this so it can never wait on itself. */
    private suspend fun fetchFeed(category: String?, before: String?, query: String?, uid: String): MarketFeed {
        ensureCurrentAccount(uid)
        val firstPage = category == null && before == null && query.isNullOrBlank()
        return call<MarketFeed>("getMarketFeed", buildJsonObject {
            category?.let { put("category", it) }
            before?.let { put("before", it) }
            query?.trim()?.takeIf { it.isNotEmpty() }?.let { put("query", it.take(60)) }
        }).also {
            ensureCurrentAccount(uid)
            badge.update(it.me.unreadCount)
            if (firstPage) saveFeedCache(it, uid)
        }
    }

    /**
     * Called from the home screen instead of the badge-only summary: it warms the feed function and
     * the on-device copy, so tapping Kampüs Dolabı shows listings without waiting.
     */
    suspend fun refreshFromHome() {
        val uid = currentUid ?: return
        if (now() - prefetchStartedAt < PREFETCH_INTERVAL_MS && prefetchUid == uid) {
            summary()
            return
        }
        val request = prefetchScope.async { fetchFeed(null, null, null, uid) }
        prefetch = request
        prefetchUid = uid
        prefetchStartedAt = now()
        request.await()
    }

    override fun cachedFeed(): MarketFeed? {
        val uid = currentUid ?: return null
        val cached = runCatching {
            loadCampusClosetFeedCache()?.let { json.decodeFromString(CachedCampusClosetFeed.serializer(), it) }
        }.getOrNull() ?: return null
        if (cached.uid != uid || now() - cached.savedAtMillis !in 0..CAMPUS_CLOSET_FEED_CACHE_MAX_AGE_MS) return null
        return cached.feed
    }

    private fun ensureCurrentAccount(uid: String) {
        if (currentUid != uid) throw CancellationException("Campus Closet account changed")
    }

    private fun saveFeedCache(feed: MarketFeed, uid: String) {
        ensureCurrentAccount(uid)
        runCatching { saveCampusClosetFeedCache(json.encodeToString(CachedCampusClosetFeed.serializer(), CachedCampusClosetFeed(uid, now(), feed))) }
    }

    suspend fun listing(listingId: String): MarketListingDetail =
        call("getMarketListing", buildJsonObject { put("listingId", listingId) })

    suspend fun myListings(): List<MarketListing> =
        call<MarketMyListings>("listMyMarketListings").listings

    suspend fun inbox(): MarketInbox = call<MarketInbox>("listMarketConversations").also { badge.update(it.unreadCount) }

    suspend fun messages(conversationId: String, after: String?): MarketThread =
        call("getMarketMessages", buildJsonObject {
            put("conversationId", conversationId)
            after?.let { put("after", it) }
        })

    override suspend fun acceptTerms(version: Int) {
        callV2Function("acceptMarketTerms", buildJsonObject { put("version", version) })
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun createListing(draft: NewListingDraft, photos: List<ByteArray>): String {
        val response = callV2Function("createMarketListing", buildJsonObject {
            put("category", draft.category)
            put("condition", draft.condition)
            put("title", draft.title)
            put("description", draft.description)
            put("price", draft.price)
            putJsonArray("photos") { photos.forEach { add(kotlinx.serialization.json.JsonPrimitive(Base64.Default.encode(it))) } }
        })
        return json.decodeFromJsonElement<CreatedListing>(response).listingId
    }

    suspend fun updateListingStatus(listingId: String, action: String) {
        callV2Function("updateMarketListingStatus", buildJsonObject {
            put("listingId", listingId)
            put("action", action)
        })
    }

    suspend fun sendMessage(conversationId: String, text: String) {
        callV2Function("sendMarketMessage", buildJsonObject {
            put("conversationId", conversationId)
            put("text", text)
        })
    }

    suspend fun sendOffer(conversationId: String, percent: Int) {
        callV2Function("sendMarketMessage", buildJsonObject {
            put("conversationId", conversationId)
            put("offerPercent", percent)
        })
    }

    suspend fun respondOffer(conversationId: String, accept: Boolean) {
        callV2Function("respondMarketOffer", buildJsonObject {
            put("conversationId", conversationId)
            put("accept", accept)
        })
    }

    suspend fun block(conversationId: String) {
        callV2Function("blockMarketUser", buildJsonObject { put("conversationId", conversationId) })
    }

    suspend fun unblock(conversationId: String) {
        callV2Function("unblockMarketUser", buildJsonObject { put("conversationId", conversationId) })
    }

    suspend fun blocked(): List<MarketBlockedUser> = call<MarketBlockedList>("listMarketBlocked").blocked

    suspend fun updatePrice(listingId: String, price: Int) {
        callV2Function("updateMarketListingPrice", buildJsonObject {
            put("listingId", listingId)
            put("price", price)
        })
    }

    override suspend fun setFavorite(listingId: String, saved: Boolean) {
        callV2Function("setMarketFavorite", buildJsonObject {
            put("listingId", listingId)
            put("saved", saved)
        })
    }

    suspend fun favorites(): List<MarketListing> = call<MarketMyListings>("listMarketFavorites").listings

    suspend fun renew(listingId: String) {
        callV2Function("renewMarketListing", buildJsonObject { put("listingId", listingId) })
    }

    suspend fun report(targetType: String, targetId: String, reason: String, note: String) {
        callV2Function("reportMarketContent", buildJsonObject {
            put("targetType", targetType)
            put("targetId", targetId)
            put("reason", reason)
            if (note.isNotBlank()) put("note", note.trim())
        })
    }

    /** A buyer's conversation for a listing always has this ID, so it can be opened before it exists. */
    fun conversationIdFor(listingId: String): String? = currentUid?.let { "${listingId}_$it" }
}

@Serializable
private data class MarketMyListings(val listings: List<MarketListing> = emptyList())

@Serializable
private data class CreatedListing(val listingId: String)

@Serializable
private data class MarketBlockedList(val blocked: List<MarketBlockedUser> = emptyList())
