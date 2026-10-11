package com.good4.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnalyticsTrackerTest {
    private class Storage(var value: AnalyticsConsent = AnalyticsConsent.UNKNOWN) : AnalyticsConsentStorage {
        var fail = false
        override fun read() = value
        override fun write(value: AnalyticsConsent) { check(!fail); this.value = value }
    }
    private class Transport : AnalyticsTransport {
        var collectionEnabled = false
        var resets = 0
        var fail = false
        val events = mutableListOf<Pair<String, Map<String, String>>>()
        override fun setEnabled(enabled: Boolean) { this.collectionEnabled = enabled; check(!fail) }
        override fun reset() { resets++; check(!fail) }
        override fun log(name: String, parameters: Map<String, String>) { check(!fail); events += name to parameters }
    }

    @Test fun noCollectionOrReplayBeforeConsent() {
        val sink = Transport()
        val tracker = DeviceAnalyticsTracker(Storage(), sink, true)
        val pending = tracker.click(Interaction.MEAL_CODE)
        assertNull(pending)
        assertFalse(sink.collectionEnabled)
        tracker.setConsent(AnalyticsConsent.GRANTED)
        tracker.result(pending, InteractionStatus.SUCCESS)
        assertTrue(sink.events.isEmpty())
    }

    @Test fun oneClickAndOneResultWithCataloguedParameters() {
        val sink = Transport()
        val tracker = DeviceAnalyticsTracker(Storage(AnalyticsConsent.GRANTED), sink, true)
        val attempt = tracker.click(Interaction.CLOSET_PUBLISH)
        tracker.result(attempt, InteractionStatus.SUCCESS)
        tracker.result(attempt, InteractionStatus.ERROR)
        assertEquals(listOf("interaction_click", "interaction_result"), sink.events.map { it.first })
        assertEquals(mapOf("feature" to "campus_closet", "screen" to "closet_create", "action" to "publish_listing", "status" to "success"), sink.events.last().second)
    }

    @Test fun withdrawalInvalidatesPendingResultsEvenAfterOptInAgain() {
        val sink = Transport()
        val tracker = DeviceAnalyticsTracker(Storage(AnalyticsConsent.GRANTED), sink, true)
        val pending = tracker.click(Interaction.SOCIAL_JOIN)
        tracker.setConsent(AnalyticsConsent.DENIED)
        assertFalse(sink.collectionEnabled)
        assertEquals(1, sink.resets)
        tracker.setConsent(AnalyticsConsent.GRANTED)
        tracker.result(pending, InteractionStatus.SUCCESS)
        assertEquals(1, sink.events.size)
    }

    @Test fun restrictedBuildStaysDisabledDespiteStoredOrNewConsent() {
        val sink = Transport()
        val tracker = DeviceAnalyticsTracker(Storage(AnalyticsConsent.GRANTED), sink, false)
        assertFalse(sink.collectionEnabled)
        tracker.click(Interaction.HOME_SHORTCUT, AnalyticsTarget.CAMPUS_CLOSET)
        tracker.setConsent(AnalyticsConsent.DENIED)
        tracker.setConsent(AnalyticsConsent.GRANTED)
        assertFalse(sink.collectionEnabled)
        assertTrue(sink.events.isEmpty())
    }

    @Test fun preferenceSurvivesRestartAndDeniedUsersAreNotPromptedAgain() {
        val storage = Storage()
        DeviceAnalyticsTracker(storage, Transport(), true).setConsent(AnalyticsConsent.DENIED)
        val sink = Transport()
        val restarted = DeviceAnalyticsTracker(storage, sink, true)
        assertEquals(AnalyticsConsent.DENIED, restarted.consent.value)
        assertFalse(sink.collectionEnabled)
    }

    @Test fun dismissDoesNotGrantConsent() {
        val tracker = DeviceAnalyticsTracker(Storage(), Transport(), true)
        tracker.dismissPrompt()
        assertTrue(tracker.promptDismissed.value)
        assertEquals(AnalyticsConsent.UNKNOWN, tracker.consent.value)
    }

    @Test fun failedPersistenceCannotEnableCollection() {
        val storage = Storage().apply { fail = true }
        val sink = Transport()
        val tracker = DeviceAnalyticsTracker(storage, sink, true)
        assertFalse(tracker.setConsent(AnalyticsConsent.GRANTED))
        assertEquals(AnalyticsConsent.UNKNOWN, tracker.consent.value)
        assertFalse(sink.collectionEnabled)
        assertNull(tracker.click(Interaction.HOME_OPEN))
    }

    @Test fun sdkFailuresDoNotEscapeIntoUserActions() {
        val tracker = DeviceAnalyticsTracker(Storage(AnalyticsConsent.GRANTED), Transport().apply { fail = true }, true)
        val attempt = tracker.click(Interaction.MEAL_CODE)
        tracker.result(attempt, InteractionStatus.ERROR)
        assertTrue(tracker.setConsent(AnalyticsConsent.DENIED))
    }

    @Test fun failedWithdrawalCanBeRetriedWithoutPretendingItWasSaved() {
        val storage = Storage(AnalyticsConsent.GRANTED).apply { fail = true }
        val sink = Transport()
        val tracker = DeviceAnalyticsTracker(storage, sink, true)
        assertFalse(tracker.setConsent(AnalyticsConsent.DENIED))
        assertFalse(sink.collectionEnabled)
        assertFalse(tracker.setConsent(AnalyticsConsent.DENIED))
        storage.fail = false
        assertTrue(tracker.setConsent(AnalyticsConsent.DENIED))
        assertEquals(AnalyticsConsent.DENIED, storage.value)
    }

    @Test fun unreadableConsentFailsClosed() {
        val storage = object : AnalyticsConsentStorage {
            override fun read(): AnalyticsConsent = error("unavailable")
            override fun write(value: AnalyticsConsent) = Unit
        }
        val sink = Transport()
        val tracker = DeviceAnalyticsTracker(storage, sink, true)
        assertEquals(AnalyticsConsent.UNKNOWN, tracker.consent.value)
        assertFalse(sink.collectionEnabled)
    }

    @Test fun fixedTargetsNeverPassThroughUnknownText() {
        assertNull(AnalyticsTarget.shortcut("person@example.com"))
        assertNull(AnalyticsTarget.category("private-listing-id"))
        assertEquals(AnalyticsTarget.CLASS_SCHEDULE, AnalyticsTarget.shortcut("classSchedule"))
        assertEquals(AnalyticsTarget.ALL, AnalyticsTarget.category(null))
    }
}
