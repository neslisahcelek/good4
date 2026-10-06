package com.good4.social

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SocialMe(
    val enabled: Boolean = false,
    val eduVerified: Boolean = false,
    val eduEmail: String? = null,
    val universityName: String? = null,
    val termsAccepted: Boolean = false,
    val termsVersion: Int = 1,
    /** "masked" ("A.. Y..") or "shown" ("Ayşe Y."); the other students see the name this gives. */
    val nameMode: String = "masked",
    val shownName: String = "",
    val maskedName: String = "",
    val photoUrl: String? = null,
    val suspendedUntil: String? = null,
    val unreadCount: Int = 0,
    /** Requests waiting for the student's answer on their own upcoming activities (summary only). */
    val pendingRequestCount: Int = 0
) {
    val canJoin: Boolean get() = enabled && eduVerified && termsAccepted && suspendedUntil == null
    val namesShown: Boolean get() = nameMode == "shown"
}

@Serializable
data class SocialActivity(
    val id: String,
    val kind: String = "",
    val type: String = "",
    val title: String = "",
    val startsAt: String? = null,
    val level: String? = null,
    /** Board games only: okey, backgammon, chess, uno, taboo, cards or other. */
    val game: String? = null,
    val capacity: Int = 1,
    val acceptedCount: Int = 0,
    val spotsLeft: Int = 0,
    val organizerName: String = "",
    val organizerPhotoUrl: String? = null,
    val universityName: String = "",
    /** open, full, cancelled, ended or removed; started activities already read as ended. */
    val status: String = "",
    val isMine: Boolean = false,
    val note: String? = null,
    /** Organizer only. */
    val pendingCount: Int = 0
) {
    val isSport: Boolean get() = kind == "sport"
    val isUpcoming: Boolean get() = status == "open" || status == "full"
    val startsAtInstant: Instant? get() = startsAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
}

@Serializable
data class SocialFeed(val me: SocialMe, val activities: List<SocialActivity> = emptyList(), val nextAfter: String? = null)

@Serializable
data class SocialSummary(val me: SocialMe)

@Serializable
data class SocialMyRequest(
    /** pending, accepted, withdrawn or closed ("Yer kalmadı"; declines read the same). */
    val status: String = "",
    val conversationId: String? = null
)

@Serializable
data class SocialActivityDetail(
    val me: SocialMe,
    val activity: SocialActivity,
    val myRequest: SocialMyRequest? = null,
    val sameCampus: Boolean = false
)

@Serializable
data class SocialJoinRequest(
    val requesterUid: String,
    val name: String = "",
    val photoUrl: String? = null,
    val note: String = "",
    val status: String = "",
    val createdAt: String? = null,
    val conversationId: String? = null
)

@Serializable
data class SocialRequestList(val activity: SocialActivity, val requests: List<SocialJoinRequest> = emptyList())

@Serializable
data class SocialJoined(val activity: SocialActivity, val status: String = "", val conversationId: String? = null)

@Serializable
data class SocialMine(val organized: List<SocialActivity> = emptyList(), val joined: List<SocialJoined> = emptyList())

@Serializable
data class SocialConversation(
    val id: String,
    val activityId: String = "",
    val activityTitle: String = "",
    val activityKind: String = "",
    val activityType: String = "",
    val activityStartsAt: String? = null,
    val role: String = "participant",
    val otherName: String = "",
    val otherPhotoUrl: String? = null,
    val lastMessageText: String = "",
    val lastMessageAt: String? = null,
    val lastMessageMine: Boolean = false,
    val unread: Int = 0,
    val status: String = "open",
    val readOnly: Boolean = false,
    val blockedByMe: Boolean = false
) {
    val isOrganizer: Boolean get() = role == "organizer"
}

@Serializable
data class SocialInbox(val unreadCount: Int = 0, val conversations: List<SocialConversation> = emptyList())

@Serializable
data class SocialMessage(
    val id: String,
    val mine: Boolean = false,
    /** text or system ("İsteğin kabul edildi", "Etkinlik iptal edildi"). */
    val type: String = "text",
    val text: String = "",
    val createdAt: String? = null
)

@Serializable
data class SocialThread(val conversation: SocialConversation, val messages: List<SocialMessage> = emptyList())

data class SocialActivityDraft(
    val kind: String,
    val type: String,
    val title: String,
    val note: String,
    val startsAt: Instant,
    val level: String?,
    val game: String?,
    val capacity: Int
)

/** Must match SOCIAL_LIMITS in firebase/v2/functions/src/social.ts. */
object SocialLimits {
    const val MIN_TITLE = 3
    const val MAX_TITLE = 60
    const val MAX_NOTE = 300
    const val MAX_REQUEST_NOTE = 200
    const val MAX_MESSAGE = 500
    /** Server: SOCIAL_PROFILE_LIMITS.maxPhotoChangesPerDay. */
    const val MAX_PHOTO_CHANGES_PER_DAY = 5
    const val MAX_CAPACITY = 10
    const val MIN_LEAD_MINUTES = 15
    const val MAX_LEAD_DAYS = 30
}
