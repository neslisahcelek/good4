package com.good4.campusevents

import com.good4.core.presentation.UiText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/** One campus event as the events box shows it; nothing here goes beyond what the source publishes. */
data class CampusEvent(
    val id: String,
    val title: String,
    val communityName: String,
    val location: String,
    val description: String,
    val start: LocalDateTime,
    val end: LocalDateTime?,
    val imageUrl: String,
    val categoryId: String
)

data class CampusEventsUi(
    val events: List<CampusEvent> = emptyList(),
    val selectedCategory: String? = null,
    val loading: Boolean = false,
    val error: UiText? = null,
    val openEventId: String? = null
) {
    val openEvent: CampusEvent? get() = openEventId?.let { id -> events.firstOrNull { it.id == id } }

    /** Categories that actually have events, in a stable order, so the filter row never offers an empty choice. */
    val categories: List<String>
        get() = CATEGORY_ORDER.filter { id -> events.any { it.categoryId == id } }

    val visible: List<CampusEvent>
        get() = events.filter { selectedCategory == null || it.categoryId == selectedCategory }.sortedBy { it.start }

    val days: List<Pair<LocalDate, List<CampusEvent>>>
        get() = visible.groupBy { it.start.date }.toList()
}

private val CATEGORY_ORDER = listOf(
    "sports-nature", "culture-arts", "social-entertainment", "academic-science",
    "career-entrepreneurship", "technology", "volunteering", "other"
)

/**
 * Picks the line-art poster motif (shared with Sosyal) for events without a poster image.
 * Sport motifs get the dark green poster, everything else the cream one.
 */
internal data class PosterMotif(val type: String, val kind: String)

internal fun posterMotifFor(event: CampusEvent): PosterMotif {
    // Turkish-aware lowercasing: "İzleme" must become "izleme", not "i̇zleme", or keywords never match.
    val text = "${event.title} ${event.communityName}".replace('İ', 'i').replace('I', 'ı').lowercase()
    fun has(vararg words: String) = words.any { it in text }
    val type = when {
        has("bisiklet", "ride") -> "cycling"
        has("paten", "hokey") -> "skating"
        has("sup", "kano", "kayak") -> "sup-kayak"
        has("yoga", "pilates") -> "yoga-pilates"
        has("koşu", "maraton") -> "running"
        has("fitness", "egzersiz", "fizyoterapi") -> "fitness"
        has("yürüyüş", "doğa", "dağ", "mağara", "kamp", "havacılık", "paraşüt", "yer eğitimi") -> "hiking"
        has("film", "sinema", "izleme", "gösterim") -> "movie"
        has("konser", "kareoke", "karaoke") -> "concert"
        has("müzik") -> "music"
        has("gezi", "antik", "turu", "tur ") -> "trip"
        has("tanışma", "çay", "kahve", "coffee") -> "meetup"
        has("workshop", "atölye", "sunum", "seminer", "konferans", "eğitim") -> "study"
        has("gönüllü", "farkındalık", "lösev", "bağış") -> "volunteering"
        else -> "meetup"
    }
    val sport = type in setOf("cycling", "skating", "sup-kayak", "yoga-pilates", "running", "fitness", "hiking")
    return PosterMotif(type, if (sport) "sport" else "social")
}

internal fun hhmm(time: LocalDateTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
