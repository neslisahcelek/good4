package com.good4.community

import androidx.compose.runtime.Composable
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource


import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Events are scheduled in campus time, matching the server's Europe/Istanbul timestamps. */
internal val EventTimeZone = TimeZone.of("Europe/Istanbul")
internal val DefaultEventDuration = 2.hours
internal val MaxEventDuration = 14.days

private val MonthNames = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
private val DayNames = mapOf(
    DayOfWeek.MONDAY to "Pazartesi", DayOfWeek.TUESDAY to "Salı", DayOfWeek.WEDNESDAY to "Çarşamba",
    DayOfWeek.THURSDAY to "Perşembe", DayOfWeek.FRIDAY to "Cuma", DayOfWeek.SATURDAY to "Cumartesi", DayOfWeek.SUNDAY to "Pazar"
)

internal fun eventNow(): LocalDateTime = Clock.System.now().toLocalDateTime(EventTimeZone)

/** The relative day shown on an event, or null once the event is over. */
internal fun CommunityEntryDto.relativeDayLabel(now: LocalDateTime): String? =
    if (hasEnded(now)) null else relativeEventDay(date, now.date)

internal fun relativeEventDay(date: String, today: LocalDate): String? {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return when (val days = today.daysUntil(parsed)) {
        0 -> "Bugün"
        1 -> "Yarın"
        in 2..7 -> "$days gün sonra"
        else -> null
    }
}

internal fun eventDateTime(date: String, time: String): LocalDateTime? = runCatching {
    LocalDateTime(LocalDate.parse(date), LocalTime.parse(time))
}.getOrNull()

/** Older entries have no end; they last until the end of their start day for listing purposes. */
internal fun CommunityEntryDto.lastDate(): String = endDate.ifBlank { date }

/** The end a new start suggests: two hours later, rolling over to the next day when needed. */
internal fun suggestedEventEnd(date: String, time: String): Pair<String, String>? {
    val start = eventDateTime(date, time) ?: return null
    val end = (start.toInstant(EventTimeZone) + DefaultEventDuration).toLocalDateTime(EventTimeZone)
    return end.date.toString() to end.time.hhmm()
}

/**
 * Checks the start/end pair. A start in the past is only accepted when an edit leaves it unchanged,
 * so an ongoing event can still have its location or description corrected.
 */
internal fun validateEventSchedule(entry: CommunityEntryDto, initial: CommunityEntryDto?, now: LocalDateTime): String? {
    val start = eventDateTime(entry.date, entry.time) ?: return "Başlangıç tarihini ve saatini seçin."
    val end = eventDateTime(entry.endDate, entry.endTime) ?: return "Bitiş tarihini ve saatini seçin."
    val startChanged = initial == null || initial.date != entry.date || initial.time != entry.time
    if (startChanged && start < now) return "Başlangıç geçmiş bir zaman olamaz."
    val startInstant = start.toInstant(EventTimeZone)
    val endInstant = end.toInstant(EventTimeZone)
    if (endInstant <= startInstant) return "Bitiş, başlangıçtan sonra olmalı."
    if (endInstant - startInstant > MaxEventDuration) return "Etkinlik en fazla 14 gün sürebilir."
    return null
}

internal fun formatEventDate(date: String): String = runCatching {
    val parsed = LocalDate.parse(date)
    "${parsed.dayOfMonth} ${MonthNames[parsed.monthNumber - 1]} ${DayNames.getValue(parsed.dayOfWeek)}"
}.getOrDefault(date)

/** "7 Ekim Çarşamba · 18:00–20:00", or both dates when the event spans several days. */
internal fun formatEventSchedule(entry: CommunityEntryDto): String {
    if (entry.date.isBlank()) return ""
    val startDate = formatEventDate(entry.date)
    if (entry.time.isBlank()) return startDate
    if (entry.endTime.isBlank()) return "$startDate · ${entry.time}"
    if (entry.lastDate() == entry.date) return "$startDate · ${entry.time}–${entry.endTime}"
    val endDate = runCatching { LocalDate.parse(entry.endDate) }.getOrNull()
        ?.let { "${it.dayOfMonth} ${MonthNames[it.monthNumber - 1]}" } ?: entry.endDate
    val shortStart = runCatching { LocalDate.parse(entry.date) }.getOrNull()
        ?.let { "${it.dayOfMonth} ${MonthNames[it.monthNumber - 1]}" } ?: entry.date
    return "$shortStart ${entry.time} – $endDate ${entry.endTime}"
}

private fun LocalTime.hhmm() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

/**
 * Moves the start. An existing end keeps the event's length; otherwise the default length is suggested.
 * Coupons only carry an expiry date, so they are returned with the new date as is.
 */
internal fun CommunityEntryDto.withStart(newDate: String, newTime: String): CommunityEntryDto {
    val moved = copy(date = newDate, time = newTime)
    if (kind != "event") return moved
    val newStart = eventDateTime(newDate, newTime) ?: return moved
    val oldStart = eventDateTime(date, time)
    val oldEnd = eventDateTime(endDate, endTime)
    if (oldStart != null && oldEnd != null && oldEnd > oldStart) {
        val length = oldEnd.toInstant(EventTimeZone) - oldStart.toInstant(EventTimeZone)
        val end = (newStart.toInstant(EventTimeZone) + length).toLocalDateTime(EventTimeZone)
        return moved.copy(endDate = end.date.toString(), endTime = end.time.hhmm())
    }
    val (suggestedDate, suggestedTime) = suggestedEventEnd(newDate, newTime) ?: return moved
    return moved.copy(endDate = suggestedDate, endTime = suggestedTime)
}

/** Older events were saved without an end; the editor proposes the default length for them. */
internal fun CommunityEntryDto.withSuggestedEnd(): CommunityEntryDto {
    if (kind != "event" || (endDate.isNotBlank() && endTime.isNotBlank())) return this
    val (suggestedDate, suggestedTime) = suggestedEventEnd(date, time) ?: return this
    return copy(endDate = suggestedDate, endTime = suggestedTime)
}

private val ShortMonthNames = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")

internal fun shortMonthName(date: String): String =
    runCatching { ShortMonthNames[LocalDate.parse(date).monthNumber - 1] }.getOrDefault("")

/** Events without an end time count as running until the end of their last day. */
internal fun CommunityEntryDto.hasEnded(now: LocalDateTime): Boolean {
    val end = eventDateTime(lastDate(), endTime.ifBlank { "23:59" }) ?: return false
    return end < now
}

/** Quick end choices under the start; [CUSTOM] reveals the end date and time fields. */
internal enum class EventDuration(val shortLabelResource: StringResource, val minutes: Int?) {
    ONE_HOUR(Res.string.community_1_sa, 60), TWO_HOURS(Res.string.community_2_sa, 120), THREE_HOURS(Res.string.community_3_sa, 180), ALL_DAY(Res.string.community_tum_gun, null), CUSTOM(Res.string.community_ozel, null);
    val shortLabel: String @Composable get() = stringResource(shortLabelResource)
}

/** Sets the end from a quick choice; "Tüm gün" runs until the end of the start day. */
internal fun CommunityEntryDto.withDuration(duration: EventDuration): CommunityEntryDto {
    val start = eventDateTime(date, time) ?: return this
    return when (duration) {
        EventDuration.CUSTOM -> this
        EventDuration.ALL_DAY -> copy(endDate = date, endTime = "23:59")
        else -> {
            val end = (start.toInstant(EventTimeZone) + duration.minutes!!.minutes).toLocalDateTime(EventTimeZone)
            copy(endDate = end.date.toString(), endTime = end.time.hhmm())
        }
    }
}

/** Which quick choice the current start and end match, or [EventDuration.CUSTOM] for anything else. */
internal fun CommunityEntryDto.matchingDuration(): EventDuration? {
    val start = eventDateTime(date, time) ?: return null
    val end = eventDateTime(endDate, endTime) ?: return null
    if (endDate == date && endTime == "23:59") return EventDuration.ALL_DAY
    val minutes = (end.toInstant(EventTimeZone) - start.toInstant(EventTimeZone)).inWholeMinutes
    return EventDuration.entries.firstOrNull { it.minutes?.toLong() == minutes } ?: EventDuration.CUSTOM
}

enum class EventField(val labelResource: StringResource) {
    TITLE(Res.string.campus_closet_baslik), CATEGORY(Res.string.campus_closet_kategori), DESCRIPTION(Res.string.campus_closet_aciklama),
    START(Res.string.community_baslangic), END(Res.string.community_bitis), LOCATION(Res.string.community_konum), CAPACITY(Res.string.community_kontenjan);
    val label: String @Composable get() = stringResource(labelResource)
}

/** Per-field messages for the event form, in the order the fields appear. */
internal fun validateEventFields(entry: CommunityEntryDto, initial: CommunityEntryDto?, now: LocalDateTime): Map<EventField, UiText> {
    val errors = linkedMapOf<EventField, UiText>()
    if (entry.title.isBlank()) errors[EventField.TITLE] = UiText.StringResourceId(Res.string.community_etkinlige_bir_baslik_ver)
    else if (entry.title.length > 120) errors[EventField.TITLE] = UiText.StringResourceId(Res.string.community_baslik_en_fazla_120_karakter_olabilir)
    if (EventCategory.fromId(entry.categoryId) == null) errors[EventField.CATEGORY] = UiText.StringResourceId(Res.string.community_bir_kategori_sec)
    if (entry.description.isBlank()) errors[EventField.DESCRIPTION] = UiText.StringResourceId(Res.string.community_etkinlikte_neler_olacagini_kisaca_yaz)
    val start = eventDateTime(entry.date, entry.time)
    val startChanged = initial == null || initial.date != entry.date || initial.time != entry.time
    when {
        start == null -> errors[EventField.START] = UiText.StringResourceId(Res.string.community_baslangic_tarihini_ve_saatini_sec)
        (startChanged || (initial?.status == "draft" && entry.status == "published")) &&
            start.toInstant(EventTimeZone) < now.toInstant(EventTimeZone) - 5.minutes -> errors[EventField.START] = UiText.StringResourceId(Res.string.community_baslangic_gecmis_bir_zaman_olamaz)
    }
    val end = eventDateTime(entry.endDate, entry.endTime)
    if (start != null) {
        val startInstant = start.toInstant(EventTimeZone)
        when {
            end == null -> errors[EventField.END] = UiText.StringResourceId(Res.string.community_bitis_zamanini_sec)
            end.toInstant(EventTimeZone) <= startInstant -> errors[EventField.END] = UiText.StringResourceId(Res.string.community_bitis_baslangictan_sonra_olmali)
            end.toInstant(EventTimeZone) - startInstant > MaxEventDuration -> errors[EventField.END] = UiText.StringResourceId(Res.string.community_etkinlik_en_fazla_14_gun_surebilir)
        }
    }
    if (entry.location.isBlank()) errors[EventField.LOCATION] = UiText.StringResourceId(Res.string.community_etkinligin_yerini_yaz)
    if (entry.capacity > 100_000) errors[EventField.CAPACITY] = UiText.StringResourceId(Res.string.community_en_fazla_100_000)
    return errors
}

/** "7 Eki · 18:00" for narrow cards; the full range is shown on the event itself. */
internal fun formatEventShort(entry: CommunityEntryDto): String {
    val day = runCatching { LocalDate.parse(entry.date).dayOfMonth.toString() }.getOrNull() ?: return entry.date
    return listOf("$day ${shortMonthName(entry.date)}", entry.time).filter { it.isNotBlank() }.joinToString(" · ")
}
