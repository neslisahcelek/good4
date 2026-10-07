package com.good4.social

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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Unread messages plus requests waiting for an answer, shown on the home tile. */
class SocialBadge {
    private val _count = MutableStateFlow(0)
    val count = _count.asStateFlow()
    private var lastSummaryAt = 0L

    fun update(me: SocialMe) {
        _count.value = me.unreadCount + me.pendingRequestCount
    }

    /** The home screen asks at most every two minutes so returning home stays free. */
    fun shouldRefresh(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastSummaryAt < 120_000) return false
        lastSummaryAt = now
        return true
    }
}

/** Every read and write goes through the social callables (functions/src/social.ts). */
class SocialRepository(private val badge: SocialBadge) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    private suspend inline fun <reified T> call(name: String, data: JsonObject = JsonObject(emptyMap())): T =
        json.decodeFromJsonElement(callV2Function(name, data))

    suspend fun summary(): SocialMe = call<SocialSummary>("getSocialSummary").me.also(badge::update)

    suspend fun feed(kind: String?, after: String?): SocialFeed = call("getSocialFeed", buildJsonObject {
        kind?.let { put("kind", it) }
        after?.let { put("after", it) }
    })

    suspend fun activity(activityId: String): SocialActivityDetail =
        call("getSocialActivity", buildJsonObject { put("activityId", activityId) })

    suspend fun mine(): SocialMine = call("listMySocialActivities")

    suspend fun requests(activityId: String): SocialRequestList =
        call("listSocialActivityRequests", buildJsonObject { put("activityId", activityId) })

    /** [showName] is the choice made on the rules screen; null leaves the name masked. */
    suspend fun acceptTerms(version: Int, showName: Boolean? = null) {
        callV2Function("acceptSocialTerms", buildJsonObject {
            put("version", version)
            showName?.let { put("nameMode", if (it) "shown" else "masked") }
        })
    }

    /** Changes the name mode and/or the profile photo; returns the refreshed status. */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun updateProfile(showName: Boolean? = null, photo: ByteArray? = null, removePhoto: Boolean = false): SocialMe {
        callV2Function("setSocialProfile", buildJsonObject {
            showName?.let { put("nameMode", if (it) "shown" else "masked") }
            photo?.let { put("photo", Base64.Default.encode(it)) }
            if (removePhoto) put("removePhoto", true)
        })
        return summary()
    }

    suspend fun create(draft: SocialActivityDraft): String = call<CreatedActivity>("createSocialActivity", buildJsonObject {
        put("kind", draft.kind)
        put("type", draft.type)
        put("title", draft.title)
        if (draft.note.isNotBlank()) put("note", draft.note)
        put("startsAt", draft.startsAt.toString())
        draft.level?.let { put("level", it) }
        draft.game?.let { put("game", it) }
        put("capacity", draft.capacity)
    }).activityId

    suspend fun cancel(activityId: String) {
        callV2Function("cancelSocialActivity", buildJsonObject { put("activityId", activityId) })
    }

    suspend fun requestToJoin(activityId: String, note: String) {
        callV2Function("requestToJoinSocialActivity", buildJsonObject {
            put("activityId", activityId)
            if (note.isNotBlank()) put("note", note.trim())
        })
    }

    suspend fun withdraw(activityId: String) {
        callV2Function("withdrawSocialRequest", buildJsonObject { put("activityId", activityId) })
    }

    suspend fun respond(activityId: String, requesterUid: String, accept: Boolean): String? =
        call<RespondResult>("respondToSocialRequest", buildJsonObject {
            put("activityId", activityId)
            put("requesterUid", requesterUid)
            put("accept", accept)
        }).conversationId

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
        callV2Function("blockSocialUser", buildJsonObject { put("conversationId", conversationId) })
    }

    suspend fun unblock(conversationId: String) {
        callV2Function("unblockSocialUser", buildJsonObject { put("conversationId", conversationId) })
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
