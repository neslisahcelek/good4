package com.good4.campusevents

import com.good4.core.data.repository.DocumentWithId
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun event(id: String, start: String, category: String = "sports-nature", title: String = "Etkinlik $id") =
    CampusEvent(id, title, "Topluluk", "Kampüs", "", LocalDateTime.parse(start), null, "", category)

class CampusEventMappingTest {
    // 2026-11-15T09:30 Istanbul = 06:30 UTC.
    private val start = 1_794_724_200L

    @Test fun theAdminDesignWinsOverTheSitePoster() {
        val doc = DocumentWithId("a", CampusEventDto(title = "Çakırlar Turu", startsAt = start, endsAt = start + 3600,
            sourceImageUrl = "https://site/poster.webp", posterUrl = "https://admin/design.webp"))
        assertEquals("https://admin/design.webp", doc.toCampusEvent()?.imageUrl)
        val withoutDesign = DocumentWithId("b", doc.data.copy(posterUrl = ""))
        assertEquals("https://site/poster.webp", withoutDesign.toCampusEvent()?.imageUrl)
    }

    @Test fun timesAreShownInCampusTimeAndAMissingEndStaysOpen() {
        val event = DocumentWithId("a", CampusEventDto(title = "Tur", startsAt = start)).toCampusEvent()
        assertNotNull(event)
        assertEquals(LocalDateTime.parse("2026-11-15T09:30"), event.start)
        assertNull(event.end)
    }

    @Test fun incompleteDocumentsAreSkipped() {
        assertNull(DocumentWithId("a", CampusEventDto(title = "", startsAt = start)).toCampusEvent())
        assertNull(DocumentWithId("b", CampusEventDto(title = "Tur", startsAt = 0)).toCampusEvent())
        assertEquals("other", DocumentWithId("c", CampusEventDto(title = "Tur", startsAt = start, categoryId = "")).toCampusEvent()?.categoryId)
    }

    @Test fun posterMotifsFollowTheEventSubject() {
        assertEquals(PosterMotif("cycling", "sport"), posterMotifFor(event("1", "2026-11-15T09:30", title = "Çakırlar Bisiklet Turu")))
        assertEquals(PosterMotif("hiking", "sport"), posterMotifFor(event("2", "2026-11-11T09:00", title = "Konyaaltı Yer Eğitimi").copy(communityName = "Havacılık Topluluğu")))
        assertEquals(PosterMotif("movie", "social"), posterMotifFor(event("3", "2026-11-13T18:00", title = "100 Meters İzleme Etkinliği")))
        assertEquals(PosterMotif("meetup", "social"), posterMotifFor(event("4", "2026-11-12T17:00", title = "Tanışma Toplantısı")))
    }
}

class CampusEventsUiTest {
    private val events = listOf(
        event("late", "2026-11-15T18:00", "culture-arts"),
        event("early", "2026-11-11T09:00"),
        event("same-day", "2026-11-11T18:00", "social-entertainment")
    )

    @Test fun eventsAreGroupedByDaySoonestFirst() {
        val days = CampusEventsUi(events).days
        assertEquals(listOf("2026-11-11", "2026-11-15"), days.map { it.first.toString() })
        assertEquals(listOf("early", "same-day"), days.first().second.map { it.id })
    }

    @Test fun onlyCategoriesWithEventsAreOfferedAndTheFilterApplies() {
        val ui = CampusEventsUi(events, selectedCategory = "culture-arts")
        assertEquals(listOf("sports-nature", "culture-arts", "social-entertainment"), ui.categories)
        assertEquals(listOf("late"), ui.visible.map { it.id })
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CampusEventsViewModelTest {
    private class FakeSource : CampusEventsDataSource {
        var next = CompletableDeferred<Result<List<CampusEvent>, com.good4.core.domain.Error>>()
        var calls = 0
        override suspend fun upcoming(nowEpochSeconds: Long): Result<List<CampusEvent>, com.good4.core.domain.Error> {
            calls++
            return next.await()
        }
    }

    @Test fun loadsShowsAnErrorAndRetries() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val source = FakeSource()
            val viewModel = CampusEventsViewModel(source) { 0 }
            runCurrent()
            assertTrue(viewModel.state.value.loading)

            source.next.complete(Result.Error(NetworkError("offline")))
            runCurrent()
            assertFalse(viewModel.state.value.loading)
            assertNotNull(viewModel.state.value.error)

            source.next = CompletableDeferred()
            viewModel.load()
            source.next.complete(Result.Success(listOf(event("a", "2026-11-11T09:00"))))
            runCurrent()
            assertEquals(2, source.calls)
            assertNull(viewModel.state.value.error)
            assertEquals(listOf("a"), viewModel.state.value.events.map { it.id })
        } finally { Dispatchers.resetMain() }
    }

    @Test fun aFilterWithoutEventsFallsBackToAllAndOpeningShowsTheEvent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val source = FakeSource()
            val viewModel = CampusEventsViewModel(source) { 0 }
            source.next.complete(Result.Success(listOf(event("a", "2026-11-11T09:00", "culture-arts"))))
            runCurrent()
            viewModel.selectCategory("culture-arts")
            viewModel.openEvent("a")
            assertEquals("a", viewModel.state.value.openEvent?.id)
            viewModel.closeEvent()
            assertNull(viewModel.state.value.openEvent)

            source.next = CompletableDeferred()
            viewModel.load()
            source.next.complete(Result.Success(listOf(event("b", "2026-11-12T09:00", "sports-nature"))))
            runCurrent()
            assertNull(viewModel.state.value.selectedCategory)
        } finally { Dispatchers.resetMain() }
    }
}
