package com.good4.review

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StoreReviewTest {
    private class Storage(var value: String? = null) : ReviewStorage {
        override fun read() = value
        override fun write(value: String) { this.value = value }
        fun history() = Json.decodeFromString<ReviewHistory>(value!!)
    }

    @Test fun onlyDistinctStudentSessionsCountAndHistorySurvivesRestart() {
        val storage = Storage()
        var time = 1_000L
        val coordinator = StoreReviewCoordinator(storage) { time }
        coordinator.onForeground()
        assertEquals(null, storage.value) // Auth/splash alone is not a student visit.
        coordinator.onStudentHomeEntered()
        coordinator.onStudentHomeEntered()
        assertEquals(1, storage.history().sessions)
        assertFalse(coordinator.isEligible("1"))
        coordinator.onBackground()
        time += REVIEW_SESSION_GAP_MS - 1
        coordinator.onForeground()
        coordinator.onStudentHomeEntered()
        assertEquals(1, storage.history().sessions)
        coordinator.onBackground()
        time += REVIEW_SESSION_GAP_MS
        coordinator.onForeground()
        coordinator.onStudentHomeEntered()
        assertEquals(2, storage.history().sessions)
        assertFalse(coordinator.isEligible("1"))
        val restarted = StoreReviewCoordinator(storage) { time }
        restarted.onForeground()
        restarted.onStudentHomeEntered()
        assertEquals(3, storage.history().sessions)
        assertTrue(restarted.isEligible("1"))
    }

    @Test fun requestsRequireNewVersionAndNinetyDaysAndCannotLaunchTwice() {
        val storage = Storage(Json.encodeToString(ReviewHistory(sessions = 3)))
        var time = 10_000L
        val coordinator = StoreReviewCoordinator(storage) { time }
        coordinator.onForeground()
        assertTrue(coordinator.recordAttempt("1"))
        assertFalse(coordinator.recordAttempt("1"))
        time += REVIEW_COOLDOWN_MS - 1
        assertFalse(coordinator.isEligible("2"))
        time += 1
        assertFalse(coordinator.isEligible("1"))
        assertTrue(coordinator.isEligible("2"))
        coordinator.blockModal()
        assertFalse(coordinator.recordAttempt("2"))
        coordinator.unblockModal()
        coordinator.onBackground()
        assertFalse(coordinator.recordAttempt("2"))
        coordinator.onForeground()
        assertTrue(coordinator.recordAttempt("2"))
        assertFalse(StoreReviewCoordinator(storage) { time }.isEligible("2"))
    }

    @Test fun manualStoreIntentSuppressesAutomaticReviewAndCorruptStorageRecovers() {
        val corrupt = StoreReviewCoordinator(Storage("broken"))
        assertFalse(corrupt.isEligible("1"))
        val storage = Storage(Json.encodeToString(ReviewHistory(sessions = 5)))
        val coordinator = StoreReviewCoordinator(storage) { 123L }
        coordinator.onManualStoreOpened("1")
        assertFalse(coordinator.isEligible("1"))
        assertFalse(coordinator.isEligible("2"))
    }

    @Test fun homeAloneNeverPromptsAndEachUsefulFeatureCanPromptInPlace() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            ReviewFeature.entries.forEach { feature ->
                val store = ViewModelStore()
                try {
                    val coordinator = StoreReviewCoordinator(Storage(Json.encodeToString(ReviewHistory(sessions = 3))))
                    coordinator.onForeground()
                    val viewModel = StoreReviewViewModel(coordinator, version = "1", enabled = true)
                    store.put("review", viewModel)
                    val approvals = mutableListOf<() -> Boolean>()
                    val launcher = object : StoreReviewLauncher {
                        override fun requestReview(approve: () -> Boolean) { approvals.add(approve) }
                    }
                    viewModel.updateHome(true)
                    runCurrent()
                    advanceTimeBy(60_000)
                    runCurrent()
                    assertTrue(approvals.isEmpty())
                    viewModel.updateFeature(feature, true, true, true, launcher)
                    runCurrent()
                    advanceTimeBy(REVIEW_CONTENT_WAIT_MS + REVIEW_IDLE_WAIT_MS - 1)
                    runCurrent()
                    assertTrue(approvals.isEmpty())
                    advanceTimeBy(1)
                    runCurrent()
                    assertEquals(1, approvals.size)
                    assertTrue(approvals.single().invoke())
                    assertFalse(approvals.single().invoke())
                    viewModel.updateFeature(feature, true, true, false, launcher)
                    runCurrent()
                    viewModel.updateFeature(feature, true, true, true, launcher)
                    runCurrent()
                    advanceTimeBy(REVIEW_IDLE_WAIT_MS)
                    runCurrent()
                    assertEquals(1, approvals.size)
                } finally { store.clear(); runCurrent() }
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun loadingScrollingModalsAndBackgroundPreventPromptAndStaleLaunchIsRejected() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val coordinator = StoreReviewCoordinator(Storage(Json.encodeToString(ReviewHistory(sessions = 3))))
            coordinator.onForeground()
            val viewModel = StoreReviewViewModel(coordinator, version = "1", enabled = true)
            store.put("review", viewModel)
            val approvals = mutableListOf<() -> Boolean>()
            val launcher = object : StoreReviewLauncher {
                override fun requestReview(approve: () -> Boolean) { approvals.add(approve) }
            }
            fun update(ready: Boolean = true, idle: Boolean = true) {
                viewModel.updateFeature(ReviewFeature.CLASS_SCHEDULE, true, ready, idle, launcher)
            }
            update(ready = false)
            runCurrent()
            advanceTimeBy(60_000)
            runCurrent()
            assertTrue(approvals.isEmpty())
            update(idle = false)
            runCurrent()
            advanceTimeBy(60_000)
            runCurrent()
            assertTrue(approvals.isEmpty())
            update()
            runCurrent()
            advanceTimeBy(REVIEW_IDLE_WAIT_MS - 1)
            runCurrent()
            assertTrue(approvals.isEmpty())
            coordinator.blockModal()
            runCurrent()
            advanceTimeBy(60_000)
            assertTrue(approvals.isEmpty())
            coordinator.unblockModal()
            runCurrent()
            advanceTimeBy(REVIEW_CONTENT_WAIT_MS)
            coordinator.onBackground()
            runCurrent()
            advanceTimeBy(60_000)
            assertTrue(approvals.isEmpty())
            coordinator.onForeground()
            runCurrent()
            advanceTimeBy(REVIEW_CONTENT_WAIT_MS + REVIEW_IDLE_WAIT_MS)
            runCurrent()
            assertEquals(1, approvals.size)
            update(idle = false)
            runCurrent()
            update()
            runCurrent()
            assertFalse(approvals.first().invoke())
            advanceTimeBy(REVIEW_IDLE_WAIT_MS)
            runCurrent()
            assertEquals(2, approvals.size)
            viewModel.leaveScreen()
            runCurrent()
            assertFalse(approvals.last().invoke())
            update()
            runCurrent()
            advanceTimeBy(REVIEW_CONTENT_WAIT_MS + REVIEW_IDLE_WAIT_MS)
            runCurrent()
            assertEquals(3, approvals.size)
            assertTrue(approvals.last().invoke())
        } finally {
            store.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test fun firstTwoSessionsDoNotPromptEvenAfterViewingAFeature() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val storage = Storage()
        try {
            repeat(3) { session ->
                val store = ViewModelStore()
                try {
                    val coordinator = StoreReviewCoordinator(storage)
                    coordinator.onForeground()
                    val viewModel = StoreReviewViewModel(coordinator, "1", true)
                    store.put("review", viewModel)
                    var launches = 0
                    viewModel.updateFeature(ReviewFeature.DINING_MENU, true, true, true, object : StoreReviewLauncher {
                        override fun requestReview(approve: () -> Boolean) { if (approve()) launches++ }
                    })
                    runCurrent()
                    advanceTimeBy(REVIEW_CONTENT_WAIT_MS + REVIEW_IDLE_WAIT_MS)
                    runCurrent()
                    assertEquals(if (session == 2) 1 else 0, launches)
                } finally { store.clear(); runCurrent() }
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun disabledReviewDoesNotRequestAndMissingLinkProducesUiError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val storage = Storage(Json.encodeToString(ReviewHistory(sessions = 3)))
            val coordinator = StoreReviewCoordinator(storage)
            coordinator.onForeground()
            val viewModel = StoreReviewViewModel(coordinator, "1", false, { StoreOpenResult.NOT_CONFIGURED })
            store.put("review", viewModel)
            viewModel.updateFeature(ReviewFeature.COMMUNITIES, true, true, true, object : StoreReviewLauncher {
                override fun requestReview(approve: () -> Boolean) { error("Disabled review must not launch") }
            })
            advanceTimeBy(60_000)
            runCurrent()
            viewModel.openStore()
            runCurrent()
            assertNotNull(viewModel.state.value.error)
            assertFalse(viewModel.state.value.isOpening)
            assertTrue(coordinator.isEligible("1"))
        } finally {
            store.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }
}
