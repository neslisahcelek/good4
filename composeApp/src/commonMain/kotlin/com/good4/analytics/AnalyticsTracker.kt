package com.good4.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AnalyticsConsent { UNKNOWN, GRANTED, DENIED }
enum class InteractionStatus(val value: String) {
    SUCCESS("success"), ERROR("error"), VALIDATION_ERROR("validation_error")
}

/** Only catalogued screens can cross the analytics boundary. Never accept user text or URLs. */
enum class AnalyticsScreen(val feature: String, val value: String) {
    SPLASH("splash", "splash"),
    FORCE_UPDATE("app_update", "force_update"),
    LOGIN("auth", "login"),
    STUDENT_REGISTER("auth", "student_register"),
    BUSINESS_REGISTER("auth", "business_register"),
    EMAIL_VERIFICATION("auth", "email_verification"),
    SESSION_RESTORE("auth", "session_restore"),
    STUDENT_HOME("home", "student_home"),
    STUDENT_MENU("home", "student_menu"),
    EDIT_HOME("home", "edit_home"),
    ACADEMIC_CALENDAR("academic_calendar", "academic_calendar"),
    CLASS_SCHEDULE("class_schedule", "class_schedule"),
    STUDENT_PROFILE("profile", "student_profile"),
    ACCOUNT_SETTINGS("profile", "account_settings"),
    NOTIFICATIONS("notifications", "notifications"),
    DISCOUNTS("discounts", "discounts"),
    CAMPUS_CLOSET("campus_closet", "closet_feed"),
    CAMPUS_CLOSET_LISTING("campus_closet", "closet_listing"),
    CAMPUS_CLOSET_NEW_LISTING("campus_closet", "closet_create"),
    CAMPUS_CLOSET_MY_LISTINGS("campus_closet", "closet_my_listings"),
    CAMPUS_CLOSET_INBOX("campus_closet", "closet_inbox"),
    CAMPUS_CLOSET_FAVORITES("campus_closet", "closet_favorites"),
    CAMPUS_CLOSET_BLOCKED("campus_closet", "closet_blocked"),
    CAMPUS_CLOSET_CHAT("campus_closet", "closet_chat"),
    SOCIAL("social", "social_feed"),
    SOCIAL_CREATE("social", "social_create"),
    SOCIAL_ACTIVITY("social", "social_activity"),
    SOCIAL_REQUESTS("social", "social_requests"),
    SOCIAL_INBOX("social", "social_inbox"),
    SOCIAL_CHAT("social", "social_chat"),
    SUSPENDED_MEALS("suspended_meal", "suspended_meals"),
    DINING_MENU("dining", "daily_menu"),
    CAMPUS_MAP("campus_map", "campus_map"),
    BUSINESS_HOME("business", "business_home"),
    BUSINESS_PROFILE("business", "business_profile"),
    BUSINESS_ACCOUNT_SETTINGS("business", "business_account_settings"),
    ADMIN_HOME("admin", "admin_home"),
    ADMIN_PROFILE("admin", "admin_profile"),
    ADMIN_ACCOUNT_SETTINGS("admin", "admin_account_settings")
}

/** Only catalogued values can cross the analytics boundary. Never accept user text or IDs. */
enum class AnalyticsTarget(val value: String) {
    COMMUNITIES("communities"), CLASS_SCHEDULE("class_schedule"), SOCIAL_ACTIVITIES("social_activities"),
    CAMPUS_CLOSET("campus_closet"), CAMPUS_MAP("campus_map"), ACADEMIC_CALENDAR("academic_calendar"),
    SUSPENDED_MEALS("suspended_meals"), TOP_UP("top_up"), TENNIS("tennis"),
    PHONE_NUMBERS("phone_numbers"), FEEDBACK("feedback"), ALL("all"),
    CLOTHING("clothing"), BOOKS("books"), ELECTRONICS("electronics"), ACCESSORIES("accessories"),
    SPORTS("sports"), DORM("dorm"), HOBBY("hobby"), OTHER("other"), SOCIAL("social"), SPORT("sport"),
    CAFETERIA("cafeteria"), KYK_BREAKFAST("kyk_breakfast"), KYK_DINNER("kyk_dinner"),
    ACCEPT("accept"), REJECT("reject"), SOLD("sold"), RESERVE("reserve"), REMOVE("remove"),
    FOLLOW("follow"), UNFOLLOW("unfollow");

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
    COMMUNITY_FOLLOW("community", "community_detail", "follow"),
    COMMUNITY_UNFOLLOW("community", "community_detail", "unfollow"),
    CLOSET_OPEN_LISTING("campus_closet", "closet_feed", "open_listing"),
    CLOSET_CATEGORY("campus_closet", "closet_feed", "select_category"),
    CLOSET_FEED_FAVORITE("campus_closet", "closet_feed", "toggle_favorite"),
    CLOSET_FAVORITE("campus_closet", "closet_listing", "toggle_favorite"),
    CLOSET_NEW("campus_closet", "closet_feed", "new_listing"),
    CLOSET_PUBLISH("campus_closet", "closet_create", "publish_listing"),
    CLOSET_CHAT("campus_closet", "closet_listing", "open_chat"),
    CLOSET_OFFER("campus_closet", "closet_listing", "send_offer"),
    CLOSET_MESSAGE_SEND("campus_closet", "closet_chat", "send_message"),
    CLOSET_OFFER_RESPOND("campus_closet", "closet_chat", "respond_offer"),
    CLOSET_STATUS_UPDATE("campus_closet", "closet_listing", "update_status"),
    CLOSET_PRICE_UPDATE("campus_closet", "closet_listing", "update_price"),
    CLOSET_RENEW("campus_closet", "closet_listing", "renew_listing"),
    SOCIAL_OPEN_ACTIVITY("social", "social_feed", "open_activity"),
    SOCIAL_CATEGORY("social", "social_feed", "select_category"),
    SOCIAL_NEW("social", "social_feed", "new_activity"),
    SOCIAL_INBOX("social", "social_feed", "open_inbox"),
    SOCIAL_PUBLISH("social", "social_create", "publish_activity"),
    SOCIAL_JOIN("social", "social_activity", "request_to_join"),
    SOCIAL_REQUEST_RESPOND("social", "social_requests", "respond_request"),
    SOCIAL_CHAT_MESSAGE("social", "social_chat", "send_message"),
    SOCIAL_WITHDRAW("social", "social_activity", "withdraw_request"),
    SOCIAL_CANCEL("social", "social_activity", "cancel_activity"),
    MEAL_CODE("suspended_meal", "suspended_meals", "request_code"),
    MEAL_CODE_REDEEM("suspended_meal", "suspended_meals", "redeem_code"),
    DINING_SELECT("dining", "daily_menu", "select_meal"),
    DINING_RATE("dining", "daily_menu", "rate_meal"),
    EDU_VERIFY_REQUEST("edu_verification", "edu_verification", "request_code"),
    EDU_VERIFY_CONFIRM("edu_verification", "edu_verification", "confirm_code"),
    EDU_VERIFY_DISCONNECT("edu_verification", "edu_verification", "disconnect"),
    FEEDBACK_SUBMIT("feedback", "feedback", "submit_feedback"),
    STORE_REVIEW_PROMPT("review", "store_review", "show_prompt"),
    STORE_REVIEW_OPEN("review", "store_review", "open_store"),
    SCHEDULE_PROFILE_SET("schedule", "academic_selection", "save_profile");

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
    fun screenView(screen: AnalyticsScreen)
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

    override fun screenView(screen: AnalyticsScreen) {
        if (!enabled()) return
        runCatching {
            transport.log("screen_view", mapOf(
                "screen_name" to screen.value,
                "screen_class" to screen.value,
                "screen" to screen.value,
                "feature" to screen.feature
            ))
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
    override fun screenView(screen: AnalyticsScreen) = Unit
}

/** Native initialization and common DI use the same persisted installation preference. */
const val ANALYTICS_CONSENT_KEY = "good4_analytics_consent_v1"
