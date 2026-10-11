package com.good4.analytics

import androidx.test.platform.app.InstrumentationRegistry
import com.good4.core.util.AppEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Explicit staging-only DebugView probe. Run separately; never part of production or emulator checks. */
class AnalyticsSDKSmokeTest {
    @Test fun fixedDiagnosticEventsReachNativeSdk() {
        // Ordinary instrumented runs use emulators: skip this explicit, networked probe there.
        org.junit.Assume.assumeTrue(!AppEnvironment.useFirebaseEmulators)
        assertEquals("good4tr-test", AppEnvironment.firebaseProjectId)
        assertTrue(analyticsCollectionAllowed())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val memoryConsent = object : AnalyticsConsentStorage {
            override fun read() = AnalyticsConsent.UNKNOWN
            override fun write(value: AnalyticsConsent) = Unit
        }
        val tracker = DeviceAnalyticsTracker(memoryConsent, AndroidAnalyticsTransport(context), true)
        try {
            tracker.setConsent(AnalyticsConsent.GRANTED)
            tracker.click(Interaction.HOME_OPEN)
            tracker.result(tracker.click(Interaction.SOCIAL_PUBLISH), InteractionStatus.VALIDATION_ERROR)
            tracker.result(tracker.click(Interaction.CLOSET_PUBLISH), InteractionStatus.SUCCESS)
            tracker.result(tracker.click(Interaction.MEAL_CODE), InteractionStatus.ERROR)
            // In debug mode the SDK uploads frequently. Observe delivery separately in DebugView.
            Thread.sleep(15_000)
        } finally {
            tracker.setConsent(AnalyticsConsent.DENIED)
        }
    }
}
