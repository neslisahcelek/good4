package com.good4.campuscloset

import kotlinx.serialization.Serializable

@Serializable
data class MarketPhoto(val url: String = "", val thumbUrl: String = "")

@Serializable
data class MarketListing(
    val id: String,
    val title: String = "",
    val price: Int = 0,
    val category: String = "",
    val condition: String = "",
    val photos: List<MarketPhoto> = emptyList(),
    val sellerName: String = "",
    val universityName: String = "",
    val status: String = "",
    val publishedAt: String? = null,
    val createdAt: String? = null,
    val isMine: Boolean = false,
    val isFavorite: Boolean = false,
    val description: String? = null,
    val rejectReason: String? = null,
    /** Seller only: when the listing leaves the feed unless renewed, and renewals left. */
    val expiresAt: String? = null,
    val renewsLeft: Int = 0
) {
    /** Whole days until expiry (0 on the last day), or null when it does not apply. */
    fun daysLeft(nowMillis: Long): Int? = expiresAt
        ?.let { runCatching { kotlinx.datetime.Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
        ?.let { ((it - nowMillis) / 86_400_000L).toInt().coerceAtLeast(0) }

    /** Offer "30 gün daha yayında tut" in the last week and after expiry. */
    fun canRenew(nowMillis: Long): Boolean = isMine && renewsLeft > 0 &&
        (status == "expired" || (status in listOf("published", "reserved") && (daysLeft(nowMillis) ?: Int.MAX_VALUE) <= 7))
}

@Serializable
data class MarketMe(
    val enabled: Boolean = true,
    val eduVerified: Boolean = false,
    val eduEmail: String? = null,
    val universityName: String? = null,
    val termsAccepted: Boolean = false,
    val termsVersion: Int = 1,
    val suspendedUntil: String? = null,
    val unreadCount: Int = 0
)

@Serializable
data class MarketFeed(val me: MarketMe, val listings: List<MarketListing> = emptyList(), val nextBefore: String? = null)

@Serializable
data class MarketSummary(val me: MarketMe)

@Serializable
data class MarketListingDetail(
    val me: MarketMe,
    val listing: MarketListing,
    val conversationId: String? = null,
    val sameCampus: Boolean = false
)

@Serializable
data class MarketOffer(val percent: Int = 0, val price: Int = 0, val status: String = "")

@Serializable
data class MarketConversation(
    val id: String,
    val listingId: String = "",
    val listingTitle: String = "",
    val listingThumbUrl: String = "",
    val listingPrice: Int = 0,
    val role: String = "buyer",
    val otherName: String = "",
    val lastMessageText: String = "",
    val lastMessageAt: String? = null,
    val lastMessageMine: Boolean = false,
    val unread: Int = 0,
    val messageCount: Int = 0,
    val status: String = "open",
    val blockedByMe: Boolean = false,
    val offer: MarketOffer? = null
) {
    val isSeller get() = role == "seller"
}

@Serializable
data class MarketInbox(val unreadCount: Int = 0, val conversations: List<MarketConversation> = emptyList())

@Serializable
data class MarketMessage(
    val id: String,
    val mine: Boolean = false,
    val type: String = "text",
    val text: String = "",
    val offerPercent: Int? = null,
    val offerPrice: Int? = null,
    val createdAt: String? = null
)

@Serializable
data class MarketThread(val conversation: MarketConversation, val messages: List<MarketMessage> = emptyList())

data class NewListingDraft(
    val category: String,
    val condition: String,
    val title: String,
    val description: String,
    val price: Int
)

/** Must match MARKET_LIMITS in firebase/v2/functions/src/market.ts. */
object CampusClosetLimits {
    const val MAX_PHOTOS = 3
    const val MAX_TITLE = 60
    const val MAX_DESCRIPTION = 600
    const val MAX_MESSAGE = 500
    /** Messages one sender may send about one listing per day (server: maxMessagesPerListingPerDay). */
    const val MAX_MESSAGES_PER_LISTING_PER_DAY = 200
    const val MAX_PRICE = 100_000
    val OFFER_PERCENTS = listOf(10, 15, 20)
}


@Serializable
data class MarketBlockedUser(
    val conversationId: String,
    val otherName: String = "",
    val listingTitle: String = "",
    val listingThumbUrl: String = ""
)
