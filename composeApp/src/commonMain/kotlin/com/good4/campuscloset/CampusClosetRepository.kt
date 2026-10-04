package com.good4.campuscloset

import com.good4.auth.data.repository.AuthRepository
import com.good4.core.network.callV2Function
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
}

class CampusClosetRepository(
    private val auth: AuthRepository,
    private val badge: CampusClosetBadge
) : CampusClosetFeedDataSource {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    val currentUid: String? get() = auth.currentUser?.uid

    private suspend inline fun <reified T> call(name: String, data: JsonObject = JsonObject(emptyMap())): T =
        json.decodeFromJsonElement(callV2Function(name, data))

    suspend fun summary(): MarketMe = call<MarketSummary>("getMarketSummary").me.also { badge.update(it.unreadCount) }

    /** [query] is searched on the server across every listing, not just the loaded page. */
    override suspend fun feed(category: String?, before: String?, query: String?): MarketFeed =
        call<MarketFeed>("getMarketFeed", buildJsonObject {
            category?.let { put("category", it) }
            before?.let { put("before", it) }
            query?.trim()?.takeIf { it.isNotEmpty() }?.let { put("query", it.take(60)) }
        }).also { badge.update(it.me.unreadCount) }

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
