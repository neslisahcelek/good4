package com.good4.campuscloset

import com.good4.auth.data.repository.AuthRepository
import com.good4.core.network.callV2Function
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/** Unread count shown on the home tile; refreshed by every Kampüs Dolabı load. */
class CampusClosetBadge {
    private val _unread = MutableStateFlow(0)
    val unread = _unread.asStateFlow()
    private var lastSummaryAt = 0L

    fun update(count: Int) {
        _unread.value = count
    }

    /** The home screen asks at most every two minutes so returning home stays free. */
    fun shouldRefresh(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastSummaryAt < 120_000) return false
        lastSummaryAt = now
        return true
    }
}

/**
 * Kampüs Dolabı is read and written only through callables, so the market
 * collections stay closed to clients and the server applies campus, block
 * and banned-item rules.
 */
class CampusClosetRepository(
    private val auth: AuthRepository,
    private val badge: CampusClosetBadge
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    val currentUid: String? get() = auth.currentUser?.uid

    private suspend inline fun <reified T> call(name: String, data: JsonObject = JsonObject(emptyMap())): T =
        json.decodeFromJsonElement(callV2Function(name, data))

    suspend fun summary(): MarketMe = call<MarketSummary>("getMarketSummary").me.also { badge.update(it.unreadCount) }

    /** [query] is searched on the server across every listing, not just the loaded page. */
    suspend fun feed(category: String?, before: String?, query: String? = null): MarketFeed =
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

    suspend fun acceptTerms(version: Int) {
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

    suspend fun setFavorite(listingId: String, saved: Boolean) {
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
data class MarketBlockedUser(
    val conversationId: String,
    val otherName: String = "",
    val listingTitle: String = "",
    val listingThumbUrl: String = ""
)

@Serializable
private data class MarketBlockedList(val blocked: List<MarketBlockedUser> = emptyList())

internal val MARKET_CATEGORIES = listOf(
    "clothing" to "Kıyafet & Ayakkabı",
    "accessories" to "Çanta & Aksesuar",
    "electronics" to "Elektronik",
    "sports" to "Spor & Outdoor",
    "books" to "Kitap & Kırtasiye",
    "dorm" to "Yurt & Ev Eşyası",
    "hobby" to "Hobi & Müzik",
    "other" to "Diğer"
)

internal val MARKET_CONDITIONS = listOf(
    "new" to "Yeni / etiketli",
    "likeNew" to "Yeni gibi",
    "good" to "İyi",
    "fair" to "Kullanılmış"
)

internal val MARKET_REPORT_REASONS = listOf(
    "prohibited" to "Yasaklı ürün",
    "scam" to "Dolandırıcılık şüphesi",
    "harassment" to "Taciz veya hakaret",
    "inappropriate" to "Uygunsuz içerik",
    "other" to "Diğer"
)

internal fun categoryLabel(id: String) = MARKET_CATEGORIES.firstOrNull { it.first == id }?.second ?: "Diğer"
internal fun conditionLabel(id: String) = MARKET_CONDITIONS.firstOrNull { it.first == id }?.second ?: ""

internal fun statusLabel(status: String) = when (status) {
    "pending" -> "İnceleniyor"
    "published" -> "Yayında"
    "reserved" -> "Rezerve"
    "sold" -> "Satıldı"
    "rejected" -> "Yayınlanmadı"
    "expired" -> "Süresi doldu"
    else -> "Kaldırıldı"
}

/** Same rounding as the server's offer price. */
internal fun offerPrice(price: Int, percent: Int): Int = (price * (100 - percent) + 50) / 100

internal fun formatPrice(price: Int): String {
    if (price == 0) return "Ücretsiz"
    val digits = price.toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return "$grouped ₺"
}

internal fun campusClosetErrorMessage(error: Throwable): String = when (error.message) {
    "MARKET_EDU_REQUIRED" -> "Bu işlem için .edu.tr adresini doğrulaman gerekiyor."
    "MARKET_TERMS_REQUIRED", "MARKET_TERMS_VERSION_OUTDATED" -> "Devam etmek için Kampüs Dolabı kurallarını kabul et."
    "MARKET_SUSPENDED" -> "Kampüs Dolabı erişimin geçici olarak kapatıldı."
    "MARKET_DISABLED" -> "Kampüs Dolabı şu an bakımda. Daha sonra tekrar dene."
    "MARKET_CONTENT_BLOCKED" ->
        "Bu ürünün satışı Kampüs Dolabı'nda yasaktır. Yasak ürün satma girişimleri kayda alınır; tekrarlanırsa erişimin kapatılır."
    "MARKET_CONTENT_BLOCKED_SUSPENDED" ->
        "Yasak ürün içeren tekrarlanan denemeler nedeniyle Kampüs Dolabı erişimin 24 saat kapatıldı."
    "MARKET_PHONE_IN_LISTING" -> "İlan metnine telefon numarası yazılamaz. Numaranı anlaştığın kişiyle mesajlarda paylaşabilirsin."
    "MARKET_DAILY_LISTING_LIMIT" -> "Bugün en fazla 5 ilan verebilirsin. Yarın tekrar dene."
    "MARKET_ACTIVE_LISTING_LIMIT" -> "Aynı anda en fazla 10 aktif ilanın olabilir. Satılan ilanları \"Satıldı\" olarak işaretle."
    "MARKET_DAILY_MESSAGE_LIMIT" -> "Bu ilan için bugünkü 200 mesaj hakkın doldu. Yarın tekrar yazabilirsin."
    "MARKET_DAILY_CONVERSATION_LIMIT" -> "Bugün en fazla 15 yeni konuşma başlatabilirsin."
    "MARKET_BLOCKED" -> "Bu konuşmaya mesaj gönderilemiyor."
    "MARKET_LISTING_UNAVAILABLE", "MARKET_LISTING_NOT_FOUND" -> "Bu ilan artık yayında değil."
    "MARKET_OTHER_CAMPUS" -> "Bu ilan başka bir kampüse ait. Kampüs Dolabı elden teslim olduğu için yalnızca kendi kampüsündeki ilanlara yazabilirsin."
    "MARKET_OFFER_PENDING" -> "Önceki teklifin henüz yanıtlanmadı."
    "MARKET_OFFER_UNAVAILABLE" -> "Bu ilana teklif verilemiyor."
    "MARKET_OFFER_NOT_PENDING" -> "Bu teklif zaten yanıtlanmış."
    "MARKET_NOT_PARTICIPANT" -> "Bu konuşmaya erişimin yok."
    "MARKET_REPORT_SELF" -> "Kendi ilanını şikayet edemezsin."
    "MARKET_PHOTOS_INVALID" -> "En az 1, en fazla 3 fotoğraf ekle."
    "MARKET_PHOTO_INVALID", "MARKET_PHOTO_CONTENT_INVALID" -> "Fotoğraf okunamadı. Başka bir fotoğraf dene."
    "MARKET_TITLE_INVALID", "MARKET_TITLE_REQUIRED" -> "Başlık 3-60 karakter olmalı."
    "MARKET_DESCRIPTION_INVALID", "MARKET_DESCRIPTION_REQUIRED" -> "Açıklama 10-600 karakter olmalı."
    "MARKET_PRICE_INVALID" -> "Fiyat 0 ile 100.000 ₺ arasında tam sayı olmalı."
    "MARKET_LISTING_STATUS_INVALID" -> "Bu ilanın durumu bu işleme izin vermiyor."
    "MARKET_RENEW_LIMIT" -> "Bu ilan en fazla 3 kez uzatılabilir. Hâlâ satılıksa yeni ilan verebilirsin."
    "MARKET_FAVORITE_LIMIT" -> "En fazla 100 ilan kaydedebilirsin. Eskilerden bazılarını kaldır."
    "MARKET_QUERY_INVALID" -> "Arama en fazla 60 karakter olabilir."
    "MARKET_MESSAGE_INVALID" -> "Mesaj en fazla 500 karakter olabilir."
    "ACCOUNT_NOT_ACTIVE" -> "Hesabın henüz aktif değil."
    "ROLE_NOT_ALLOWED" -> "Kampüs Dolabı yalnızca öğrenci hesapları için."
    else -> error.message?.takeIf { it.any(Char::isLowerCase) }
        ?: "İşlem tamamlanamadı. Bağlantını kontrol edip tekrar dene."
}
