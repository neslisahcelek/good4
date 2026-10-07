package com.good4.social

import androidx.compose.runtime.Composable
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val WEEKDAYS = listOf(
    Res.string.social_day_monday, Res.string.social_day_tuesday, Res.string.social_day_wednesday,
    Res.string.social_day_thursday, Res.string.social_day_friday, Res.string.social_day_saturday, Res.string.social_day_sunday
)

private val MONTHS = listOf(
    Res.string.social_oca, Res.string.social_sub, Res.string.social_mar, Res.string.social_nis,
    Res.string.social_may, Res.string.social_haz, Res.string.social_tem, Res.string.social_agu,
    Res.string.social_eyl, Res.string.social_eki, Res.string.social_kas, Res.string.social_ara
)

internal fun weekdayResource(date: LocalDate): StringResource = WEEKDAYS[date.dayOfWeek.ordinal]

internal fun localDateOf(instant: Instant): LocalDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date

internal fun today(): LocalDate = localDateOf(Clock.System.now())

internal fun clockLabel(instant: Instant): String {
    val time = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}

/** "Bugün", "Yarın", the weekday within a week, otherwise "12 Eki". */
@Composable
internal fun dayLabel(date: LocalDate): String {
    val now = today()
    return when (date) {
        now -> stringResource(Res.string.social_today)
        now.plus(DatePeriod(days = 1)) -> stringResource(Res.string.social_tomorrow)
        else -> if (date < now.plus(DatePeriod(days = 7))) stringResource(weekdayResource(date))
        else "${date.dayOfMonth} ${stringResource(MONTHS[date.monthNumber - 1])}"
    }
}

/** Date on feed posters: "Bugün", "Yarın" or "Cumartesi, 12 Eki". */
@Composable
internal fun dayHeading(date: LocalDate): String {
    val now = today()
    return when (date) {
        now -> stringResource(Res.string.social_today)
        now.plus(DatePeriod(days = 1)) -> stringResource(Res.string.social_tomorrow)
        else -> "${stringResource(weekdayResource(date))}, ${date.dayOfMonth} ${stringResource(MONTHS[date.monthNumber - 1])}"
    }
}

/** "Cumartesi, 12 Eki · 18:00" for the detail screen. */
@Composable
internal fun fullWhenLabel(instant: Instant): String {
    val date = localDateOf(instant)
    return "${stringResource(weekdayResource(date))}, ${date.dayOfMonth} ${stringResource(MONTHS[date.monthNumber - 1])} · ${clockLabel(instant)}"
}

@Composable
internal fun typeLabel(id: String): String = socialType(id)?.label?.let { stringResource(it) } ?: stringResource(Res.string.social_type_other)

@Composable
internal fun gameLabel(id: String?): String? = (SOCIAL_BOARD_GAMES + SOCIAL_VIDEO_GAMES).firstOrNull { it.first == id }?.second?.let { stringResource(it) }

@Composable
internal fun levelLabel(id: String?): String? = SOCIAL_LEVELS.firstOrNull { it.first == id }?.second?.let { stringResource(it) }

/** One short status under a poster: places left, or what the student already did. */
@Composable
internal fun spotsLabel(activity: SocialActivity): String = when {
    activity.status == "full" || activity.spotsLeft == 0 -> stringResource(Res.string.social_full)
    activity.spotsLeft == 1 && activity.capacity > 1 -> stringResource(Res.string.social_last_spot)
    else -> stringResource(Res.string.social_spots_left, activity.spotsLeft)
}

internal fun socialErrorMessage(error: Throwable): UiText = UiText.StringResourceId(
    when (error.message) {
        "SOCIAL_DISABLED" -> Res.string.social_error_disabled
        "SOCIAL_EDU_REQUIRED" -> Res.string.social_error_edu_required
        "SOCIAL_TERMS_REQUIRED", "SOCIAL_TERMS_VERSION_OUTDATED" -> Res.string.social_error_terms_required
        "SOCIAL_SUSPENDED" -> Res.string.social_error_suspended
        "MARKET_CONTENT_BLOCKED" -> Res.string.social_error_content_blocked
        "MARKET_CONTENT_BLOCKED_SUSPENDED" -> Res.string.social_error_content_blocked_suspended
        "SOCIAL_PHONE_IN_ACTIVITY" -> Res.string.social_error_phone_in_activity
        "SOCIAL_DAILY_ACTIVITY_LIMIT" -> Res.string.social_error_daily_activity_limit
        "SOCIAL_ACTIVE_ACTIVITY_LIMIT" -> Res.string.social_error_active_activity_limit
        "SOCIAL_TITLE_INVALID", "SOCIAL_TITLE_REQUIRED" -> Res.string.social_error_title
        "SOCIAL_GAME_INVALID" -> Res.string.social_error_game
        "SOCIAL_PHOTO_INVALID", "SOCIAL_PHOTO_CONTENT_INVALID" -> Res.string.social_error_photo
        "SOCIAL_DAILY_PHOTO_LIMIT" -> Res.string.social_error_photo_limit
        "SOCIAL_NOTE_INVALID" -> Res.string.social_error_note
        "SOCIAL_STARTS_AT_INVALID" -> Res.string.social_error_starts_at
        "SOCIAL_ACTIVITY_NOT_FOUND" -> Res.string.social_error_not_found
        "SOCIAL_ACTIVITY_UNAVAILABLE", "SOCIAL_REQUEST_CLOSED", "SOCIAL_REQUEST_QUEUE_FULL" -> Res.string.social_error_unavailable
        "SOCIAL_ACTIVITY_STATUS_INVALID" -> Res.string.social_error_status
        "SOCIAL_OWN_ACTIVITY" -> Res.string.social_error_own_activity
        "SOCIAL_ALREADY_REQUESTED" -> Res.string.social_error_already_requested
        "SOCIAL_OTHER_CAMPUS" -> Res.string.social_error_other_campus
        "SOCIAL_DAILY_REQUEST_LIMIT" -> Res.string.social_error_daily_request_limit
        "SOCIAL_REQUEST_NOTE_INVALID" -> Res.string.social_error_request_note
        "SOCIAL_REQUEST_NOT_ACTIVE", "SOCIAL_REQUEST_NOT_PENDING" -> Res.string.social_error_request_changed
        "SOCIAL_BLOCKED" -> Res.string.social_error_blocked
        "SOCIAL_CONVERSATION_CLOSED" -> Res.string.social_error_conversation_closed
        "SOCIAL_DAILY_MESSAGE_LIMIT" -> Res.string.social_error_daily_message_limit
        "SOCIAL_MESSAGE_INVALID" -> Res.string.social_error_message
        "SOCIAL_NOT_PARTICIPANT" -> Res.string.social_error_not_participant
        "SOCIAL_BLOCK_LIMIT" -> Res.string.social_error_block_limit
        "SOCIAL_REPORT_SELF" -> Res.string.social_error_report_self
        "ACCOUNT_NOT_ACTIVE" -> Res.string.social_hesabin_henuz_aktif_degil
        "ROLE_NOT_ALLOWED" -> Res.string.social_error_students_only
        else -> Res.string.social_islem_tamamlanamadi_baglantini_kontrol_edip_tekrar_dene
    }
)
