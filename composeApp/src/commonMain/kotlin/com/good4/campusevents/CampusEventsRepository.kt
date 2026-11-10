package com.good4.campusevents

import com.good4.core.data.repository.DocumentWithId
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/** Raw `campusEvents/{id}` document; written only by the import job and the admin panel. */
@Serializable
data class CampusEventDto(
    val title: String = "",
    val communityName: String = "",
    val location: String = "",
    val description: String = "",
    val startsAt: Long = 0,
    val endsAt: Long = 0,
    val categoryId: String = "other",
    val sourceImageUrl: String = "",
    val posterUrl: String = "",
    val status: String = "published"
)

private val CampusZone = TimeZone.of("Europe/Istanbul")

/** The admin's design wins over the site's poster; with neither, the screen draws a line poster. */
internal fun DocumentWithId<CampusEventDto>.toCampusEvent(): CampusEvent? {
    val dto = data
    if (dto.title.isBlank() || dto.startsAt <= 0) return null
    return CampusEvent(
        id = id,
        title = dto.title,
        communityName = dto.communityName,
        location = dto.location,
        description = dto.description,
        start = Instant.fromEpochSeconds(dto.startsAt).toLocalDateTime(CampusZone),
        end = dto.endsAt.takeIf { it > dto.startsAt }?.let { Instant.fromEpochSeconds(it).toLocalDateTime(CampusZone) },
        imageUrl = dto.posterUrl.ifBlank { dto.sourceImageUrl },
        categoryId = dto.categoryId.ifBlank { "other" }
    )
}

/** Where the events box reads from; a seam so the ViewModel can be tested without Firestore. */
interface CampusEventsDataSource {
    /** Published events that have not ended yet, soonest first. */
    suspend fun upcoming(nowEpochSeconds: Long): Result<List<CampusEvent>, Error>
}

class CampusEventsRepository(private val store: FirestoreRepository) : CampusEventsDataSource {
    override suspend fun upcoming(nowEpochSeconds: Long): Result<List<CampusEvent>, Error> =
        when (val page = store.queryPage(
            COLLECTION, mapOf("status" to "published"), CampusEventDto::class,
            pageSize = PAGE_SIZE, minimumTimestamp = "endsAt" to nowEpochSeconds
        )) {
            is Result.Error -> page
            is Result.Success -> Result.Success(page.data.items.mapNotNull { it.toCampusEvent() }.sortedBy { it.start })
        }

    private companion object {
        const val COLLECTION = "campusEvents"
        const val PAGE_SIZE = 100L
    }
}
