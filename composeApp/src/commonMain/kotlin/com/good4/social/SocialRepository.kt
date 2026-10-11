package com.good4.social

import com.good4.auth.data.repository.AuthRepository
import com.good4.core.network.callV2Function
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Unread messages plus requests waiting for an answer, shown on the home tile. */
class SocialBadge {
    private val _count = MutableStateFlow(0)
    val count = _count.asStateFlow()

    fun update(me: SocialMe) {
        _count.value = me.unreadCount + me.pendingRequestCount
    }

    fun clear() { _count.value = 0 }
}

/** Every read and write goes through the social callables (functions/src/social.ts). */
class SocialRepository(private val auth: AuthRepository, private val badge: SocialBadge) : SocialHomeDataSource {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }
    private data class Cache(
        val uid: String? = null,
        val accessRevision: Long = 0,
        val revision: Long = 0,
        val me: SocialMe? = null,
        val meSavedAt: Long = 0,
        val summarySavedAt: Long = 0,
        val summaryStartedAt: Long = 0,
        val prefetchStartedAt: Long = 0,
        val mine: CachedSocialMine? = null,
        val pages: Map<String?, CachedSocialFeed> = emptyMap()
    )
    private data class FeedRequest(val uid: String, val revision: Long, val result: Deferred<SocialFeed>)
    private data class SummaryRequest(val uid: String, val revision: Long, val result: Deferred<SocialMe>)
    private data class MineRequest(val uid: String, val revision: Long, val result: Deferred<SocialMine>)
    private val cache = MutableStateFlow(Cache())
    private val requestsMutex = Mutex()
    private val firstPageRequests = mutableMapOf<String?, FeedRequest>()
    private var summaryRequest: SummaryRequest? = null
    private var mineRequest: MineRequest? = null
    // A home prefetch survives navigation to Sosyal; only three category requests can be shared.
    private val readScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override val currentUid: String? get() = auth.currentUser?.uid
    override val revision: Long get() = session().revision
    private fun now() = Clock.System.now().toEpochMilliseconds()

    private fun session(): Cache {
        val uid = currentUid
        val accessRevision = com.good4.eduverification.CampusAccessSession.revision(uid)
        if (cache.value.uid != uid || cache.value.accessRevision != accessRevision) {
            cache.update { if (it.uid == uid && it.accessRevision == accessRevision) it else Cache(uid = uid, revision = it.revision + 1, accessRevision = accessRevision) }
            badge.clear()
        }
        return cache.value
    }

    override fun cachedFeed(kind: String?): CachedSocialFeed? {
        val current = session()
        if (current.uid == null) return null
        return current.pages[kind]?.takeIf { now() - it.savedAt in 0..SOCIAL_FEED_CACHE_MAX_AGE_MS }
    }

    override fun cachedMe(): SocialMe? {
        val current = session()
        return current.me.takeIf { current.uid != null && now() - current.meSavedAt in 0..SOCIAL_FEED_CACHE_MAX_AGE_MS }
    }

    override fun cachedMine(): CachedSocialMine? {
        val current = session()
        if (current.uid == null) return null
        return current.mine?.takeIf { now() - it.savedAt in 0..SOCIAL_FEED_CACHE_MAX_AGE_MS }
    }

    private suspend inline fun <reified T> call(name: String, data: JsonObject = JsonObject(emptyMap())): T =
        json.decodeFromJsonElement(callV2Function(name, data))

    private suspend inline fun <reified T : Any> read(name: String, data: JsonObject = JsonObject(emptyMap())): T =
        withTimeoutOrNull(30_000L) { call<T>(name, data) } ?: error("SOCIAL_READ_TIMEOUT")

    suspend fun summary(): SocialMe {
        val owner = session()
        val request = requestsMutex.withLock {
            checkSession(owner)
            val pending = summaryRequest
            if (pending != null && pending.uid == owner.uid && pending.revision == owner.revision && pending.result.isActive) pending.result
            else {
                pending?.result?.cancel()
                cache.update { if (it.uid == owner.uid && it.revision == owner.revision) it.copy(summaryStartedAt = now()) else it }
                readScope.async { fetchSummary(owner) }.also {
                    summaryRequest = SummaryRequest(requireNotNull(owner.uid), owner.revision, it)
                }
            }
        }
        return request.await().also { checkSession(owner) }
    }

    private suspend fun fetchSummary(owner: Cache): SocialMe {
        val me = read<SocialSummary>("getSocialSummary").me
        checkSession(owner)
        cache.update { if (it.uid == owner.uid && it.revision == owner.revision)
            it.copy(me = me, meSavedAt = now(), summarySavedAt = now(),
                pages = if (it.me?.hasSameFeedScope(me) == false) emptyMap() else it.pages) else it }
        badge.update(me)
        return me
    }

    /** One bounded default page per ten minutes; badge refreshes remain two minutes apart. */
    suspend fun refreshFromHome() {
        val owner = session()
        if (owner.uid == null) return
        val page = owner.pages[null]
        val lastPrefetch = page?.savedAt ?: owner.prefetchStartedAt
        if (lastPrefetch == 0L || now() - lastPrefetch !in 0 until SOCIAL_FEED_CACHE_MAX_AGE_MS) {
            firstPageRequest(null, owner) // Starts now; the screen can await the same request.
            cache.update { if (it.uid == owner.uid && it.revision == owner.revision) it.copy(prefetchStartedAt = now()) else it }
        }
        val lastSummary = maxOf(owner.summarySavedAt, owner.summaryStartedAt)
        if (lastSummary == 0L || now() - lastSummary !in 0 until SOCIAL_FEED_FRESH_MS) summary()
    }

    private fun checkSession(owner: Cache) {
        val current = session()
        if (owner.uid == null || owner.uid != current.uid || owner.revision != current.revision) {
            throw CancellationException("Social session or data changed")
        }
    }

    private suspend fun firstPageRequest(kind: String?, owner: Cache): Deferred<SocialFeed> = requestsMutex.withLock {
        checkSession(owner)
        firstPageRequests.entries.removeAll { (_, request) ->
            val stale = request.uid != owner.uid || request.revision != owner.revision
            if (stale) request.result.cancel()
            stale
        }
        firstPageRequests[kind]?.takeIf { it.result.isActive }?.result
            ?: readScope.async { fetchFeed(kind, null, owner) }.also {
                firstPageRequests[kind] = FeedRequest(requireNotNull(owner.uid), owner.revision, it)
            }
    }

    override suspend fun feed(kind: String?, after: String?): SocialFeed {
        val owner = session()
        val result = if (after == null) firstPageRequest(kind, owner).await() else fetchFeed(kind, after, owner)
        checkSession(owner)
        return result
    }

    private suspend fun fetchFeed(kind: String?, after: String?, owner: Cache): SocialFeed {
        checkSession(owner)
        val feed = read<SocialFeed>("getSocialFeed", buildJsonObject {
            kind?.let { put("kind", it) }
            after?.let { put("after", it) }
        })
        checkSession(owner)
        // Older servers include pending requests only in the summary, so keep its count.
        val me = feed.me.copy(pendingRequestCount = cache.value.me?.pendingRequestCount ?: 0)
        val result = feed.copy(me = me)
        cache.update { current ->
            if (current.uid != owner.uid || current.revision != owner.revision) current
            else {
                val pages = if (current.me?.hasSameFeedScope(me) == false) emptyMap() else current.pages
                current.copy(me = me, meSavedAt = now(), pages = if (after == null)
                    pages + (kind to CachedSocialFeed(result, now(), owner.revision)) else pages)
            }
        }
        badge.update(me)
        return result
    }

    override suspend fun invalidateHomeCache() = requestsMutex.withLock {
        session()
        cache.update { it.copy(revision = it.revision + 1, pages = emptyMap(), me = null, meSavedAt = 0,
            summarySavedAt = 0, summaryStartedAt = 0, prefetchStartedAt = 0, mine = null) }
        firstPageRequests.values.forEach { it.result.cancel() }
        firstPageRequests.clear()
        summaryRequest?.result?.cancel()
        summaryRequest = null
        mineRequest?.result?.cancel()
        mineRequest = null
    }

    private suspend fun write(name: String, data: JsonObject): JsonObject {
        val owner = session()
        checkSession(owner)
        val result = callV2Function(name, data)
        checkSession(owner)
        invalidateHomeCache()
        return result
    }

    suspend fun activity(activityId: String): SocialActivityDetail =
        call("getSocialActivity", buildJsonObject { put("activityId", activityId) })

    override suspend fun mine(): SocialMine {
        val owner = session()
        val request = requestsMutex.withLock {
            checkSession(owner)
            val pending = mineRequest
            if (pending != null && pending.uid == owner.uid && pending.revision == owner.revision && pending.result.isActive) pending.result
            else {
                pending?.result?.cancel()
                readScope.async {
                    val mine = read<SocialMine>("listMySocialActivities")
                    checkSession(owner)
                    cache.update { if (it.uid == owner.uid && it.revision == owner.revision)
                        it.copy(mine = CachedSocialMine(mine, now(), owner.revision)) else it }
                    mine
                }.also { mineRequest = MineRequest(requireNotNull(owner.uid), owner.revision, it) }
            }
        }
        return request.await().also { checkSession(owner) }
    }

    suspend fun requests(activityId: String): SocialRequestList =
        call("listSocialActivityRequests", buildJsonObject { put("activityId", activityId) })

    /** [showName] is the choice made on the rules screen; null leaves the name masked. */
    override suspend fun acceptTerms(version: Int, showName: Boolean?) {
        write("acceptSocialTerms", buildJsonObject {
            put("version", version)
            showName?.let { put("nameMode", if (it) "shown" else "masked") }
        })
    }

    /** Changes the name mode and/or the profile photo; returns the refreshed status. */
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun updateProfile(showName: Boolean?, photo: ByteArray?, removePhoto: Boolean): SocialMe {
        write("setSocialProfile", buildJsonObject {
            showName?.let { put("nameMode", if (it) "shown" else "masked") }
            photo?.let { put("photo", Base64.Default.encode(it)) }
            if (removePhoto) put("removePhoto", true)
        })
        return summary()
    }

    suspend fun create(draft: SocialActivityDraft): String = json.decodeFromJsonElement<CreatedActivity>(write("createSocialActivity", buildJsonObject {
        put("kind", draft.kind)
        put("type", draft.type)
        put("title", draft.title)
        if (draft.note.isNotBlank()) put("note", draft.note)
        put("startsAt", draft.startsAt.toString())
        draft.level?.let { put("level", it) }
        draft.game?.let { put("game", it) }
        put("capacity", draft.capacity)
    })).activityId

    suspend fun cancel(activityId: String) {
        write("cancelSocialActivity", buildJsonObject { put("activityId", activityId) })
    }

    suspend fun requestToJoin(activityId: String, note: String) {
        write("requestToJoinSocialActivity", buildJsonObject {
            put("activityId", activityId)
            if (note.isNotBlank()) put("note", note.trim())
        })
    }

    suspend fun withdraw(activityId: String) {
        write("withdrawSocialRequest", buildJsonObject { put("activityId", activityId) })
    }

    suspend fun respond(activityId: String, requesterUid: String, accept: Boolean): String? =
        json.decodeFromJsonElement<RespondResult>(write("respondToSocialRequest", buildJsonObject {
            put("activityId", activityId)
            put("requesterUid", requesterUid)
            put("accept", accept)
        })).conversationId

    suspend fun inbox(): SocialInbox = call<SocialInbox>("listSocialConversations")

    suspend fun messages(conversationId: String, after: String?): SocialThread = call("getSocialMessages", buildJsonObject {
        put("conversationId", conversationId)
        after?.let { put("after", it) }
    })

    suspend fun sendMessage(conversationId: String, text: String) {
        callV2Function("sendSocialMessage", buildJsonObject {
            put("conversationId", conversationId)
            put("text", text)
        })
    }

    suspend fun block(conversationId: String) {
        write("blockSocialUser", buildJsonObject { put("conversationId", conversationId) })
    }

    suspend fun unblock(conversationId: String) {
        write("unblockSocialUser", buildJsonObject { put("conversationId", conversationId) })
    }

    suspend fun report(targetType: String, targetId: String, reason: String, note: String) {
        callV2Function("reportSocialContent", buildJsonObject {
            put("targetType", targetType)
            put("targetId", targetId)
            put("reason", reason)
            if (note.isNotBlank()) put("note", note.trim())
        })
    }
}

@Serializable
private data class CreatedActivity(val activityId: String)

@Serializable
private data class RespondResult(val status: String = "", val conversationId: String? = null)
