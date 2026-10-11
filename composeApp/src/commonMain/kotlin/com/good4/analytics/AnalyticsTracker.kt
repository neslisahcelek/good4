package com.good4.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AnalyticsConsent { UNKNOWN, GRANTED, DENIED }
enum class InteractionStatus(val value: String) {
    SUCCESS("success"), ERROR("error"), VALIDATION_ERROR("validation_error")
}

/** Only catalogued values can cross the analytics boundary. Never accept user text or IDs. */
enum class AnalyticsTarget(val value: String) {
    COMMUNITIES("communities"), CLASS_SCHEDULE("class_schedule"), SOCIAL_ACTIVITIES("social_activities"),
    CAMPUS_CLOSET("campus_closet"), CAMPUS_MAP("campus_map"), ACADEMIC_CALENDAR("academic_calendar"),
    SUSPENDED_MEALS("suspended_meals"), TOP_UP("top_up"), TENNIS("tennis"),
    PHONE_NUMBERS("phone_numbers"), FEEDBACK("feedback"), ALL("all"),
    CLOTHING("clothing"), BOOKS("books"), ELECTRONICS("electronics"), ACCESSORIES("accessories"),
    SPORTS("sports"), DORM("dorm"), HOBBY("hobby"), OTHER("other"), SOCIAL("social"), SPORT("sport"),
    CAFETERIA("cafeteria"), KYK_BREAKFAST("kyk_breakfast"), KYK_DINNER("kyk_dinner");

    companion object {
        fun shortcut(id: String): AnalyticsTarget? = when (id) {
            "classSchedule" -> CLASS_SCHEDULE
            "socialActivities" -> SOCIAL_ACTIVITIES
            "campusCloset" -> CAMPUS_CLOSET
            "campusMap" -> CAMPUS_MAP
            "academicCalendar" -> ACADEMIC_CALENDAR
            "suspendedMeals" -> SUSPENDED_MEALS
            "topUp" -> TOP_UP
            "phoneNumbers" -> PHONE_NUMBERS
            else -> entries.firstOrNull { it.value == id }
        }
        fun category(id: String?): AnalyticsTarget? = if (id == null) ALL
            else entries.firstOrNull { it.value == id }
    }
}

enum class Interaction(val feature: String, val screen: String, val action: String) {
    HOME_OPEN("home", "home", "open_home"), MENU_OPEN("home", "home", "open_menu"),
    HOME_SHORTCUT("home", "home", "open_shortcut"), MENU_SHORTCUT("home", "menu", "open_shortcut"),
    PROFILE_OPEN("home", "home", "open_profile"), NOTIFICATIONS_OPEN("home", "home", "open_notifications"),
    HOME_EDIT("home", "home", "edit_home"), BANNER_OPEN("promotion", "home", "open_banner"),
    COMMUNITY_OPEN("community", "community_list", "open_community"),
    EVENT_OPEN("community", "community_detail", "open_event"),
    COUPON_OPEN("community", "community_detail", "open_coupon"),
    COUPON_ISSUE("community", "coupon", "request_code"),
    EVENT_REGISTER("community", "event", "register"), EVENT_UNREGISTER("community", "event", "unregister"),
    CLOSET_OPEN_LISTING("campus_closet", "closet_feed", "open_listing"),
    CLOSET_CATEGORY("campus_closet", "closet_feed", "select_category"),
    CLOSET_FEED_FAVORITE("campus_closet", "closet_feed", "toggle_favorite"),
    CLOSET_FAVORITE("campus_closet", "closet_listing", "toggle_favorite"),
    CLOSET_NEW("campus_closet", "closet_feed", "new_listing"),
    CLOSET_PUBLISH("campus_closet", "closet_create", "publish_listing"),
    CLOSET_CHAT("campus_closet", "closet_listing", "open_chat"),
    CLOSET_OFFER("campus_closet", "closet_listing", "send_offer"),
    SOCIAL_OPEN_ACTIVITY("social", "social_feed", "open_activity"),
    SOCIAL_CATEGORY("social", "social_feed", "select_category"),
    SOCIAL_NEW("social", "social_feed", "new_activity"),
    SOCIAL_INBOX("social", "social_feed", "open_inbox"),
    SOCIAL_PUBLISH("social", "social_create", "publish_activity"),
    SOCIAL_JOIN("social", "social_activity", "request_to_join"),
    MEAL_CODE("suspended_meal", "suspended_meals", "request_code"),
    DINING_SELECT("dining", "daily_menu", "select_meal"),
    DINING_RATE("dining", "daily_menu", "rate_meal");

    internal fun parameters(target: AnalyticsTarget?) = buildMap {
        put("feature", feature); put("screen", screen); put("action", action)
        target?.let { put("target", it.value) }
    }
}

class AnalyticsAttempt internal constructor(
    internal val interaction: Interaction,
    internal val target: AnalyticsTarget?,
    internal val generation: Long
) {
    internal var completed = false
}

interface AnalyticsTracker {
    val consent: StateFlow<AnalyticsConsent>
    val promptDismissed: StateFlow<Boolean>
    fun setConsent(value: AnalyticsConsent): Boolean
    fun dismissPrompt()
    fun click(interaction: Interaction, target: AnalyticsTarget? = null): AnalyticsAttempt?
    fun result(attempt: AnalyticsAttempt?, status: InteractionStatus)
}

interface AnalyticsConsentStorage {
    fun read(): AnalyticsConsent
    fun write(value: AnalyticsConsent)
}

interface AnalyticsTransport {
    fun setEnabled(enabled: Boolean)
    fun reset()
    fun log(name: String, parameters: Map<String, String>)
}

/** Used on the UI thread. SDK delivery is asynchronous and never blocks application actions. */
class DeviceAnalyticsTracker(
    private val storage: AnalyticsConsentStorage,
    private val transport: AnalyticsTransport,
    private val collectionAllowed: Boolean
) : AnalyticsTracker {
    private val mutableConsent = MutableStateFlow(runCatching { storage.read() }.getOrDefault(AnalyticsConsent.UNKNOWN))
    override val consent = mutableConsent.asStateFlow()
    private val dismissed = MutableStateFlow(false)
    override val promptDismissed = dismissed.asStateFlow()
    private var generation = 0L

    init { runCatching { transport.setEnabled(enabled()) } }
    private fun enabled() = collectionAllowed && consent.value == AnalyticsConsent.GRANTED

    override fun setConsent(value: AnalyticsConsent): Boolean {
        if (value == consent.value) return runCatching { storage.write(value) }.isSuccess
        generation++ // A later opt-in must never resurrect a pending operation from a previous consent.
        if (value != AnalyticsConsent.GRANTED) {
            mutableConsent.update { value }
            runCatching { transport.setEnabled(false) }
            runCatching { transport.reset() }
        }
        val saved = runCatching { storage.write(value) }.isSuccess
        if (!saved) return false
        mutableConsent.update { value }
        runCatching { transport.setEnabled(enabled()) }
        return true
    }

    override fun dismissPrompt() { dismissed.update { true } }

    override fun click(interaction: Interaction, target: AnalyticsTarget?): AnalyticsAttempt? {
        if (!enabled()) return null
        runCatching { transport.log("interaction_click", interaction.parameters(target)) }
        return AnalyticsAttempt(interaction, target, generation)
    }

    override fun result(attempt: AnalyticsAttempt?, status: InteractionStatus) {
        if (attempt == null || attempt.completed) return
        attempt.completed = true
        if (!enabled() || attempt.generation != generation) return
        runCatching {
            transport.log("interaction_result", attempt.interaction.parameters(attempt.target) + ("status" to status.value))
        }
    }
}

/** Constructor default for previews and existing isolated ViewModel tests; production DI always injects. */
object NoOpAnalyticsTracker : AnalyticsTracker {
    override val consent = MutableStateFlow(AnalyticsConsent.UNKNOWN).asStateFlow()
    override val promptDismissed = MutableStateFlow(true).asStateFlow()
    override fun setConsent(value: AnalyticsConsent) = false
    override fun dismissPrompt() = Unit
    override fun click(interaction: Interaction, target: AnalyticsTarget?): AnalyticsAttempt? = null
    override fun result(attempt: AnalyticsAttempt?, status: InteractionStatus) = Unit
}

/** Native initialization and common DI use the same persisted installation preference. */
const val ANALYTICS_CONSENT_KEY = "good4_analytics_consent_v1"
