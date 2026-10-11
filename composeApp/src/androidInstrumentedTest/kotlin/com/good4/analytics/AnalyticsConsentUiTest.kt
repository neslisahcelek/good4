package com.good4.analytics

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.good4.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/** Device UI checks use a recording transport, so no test interactions leave the device. */
class AnalyticsConsentUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private class Storage(var value: AnalyticsConsent = AnalyticsConsent.UNKNOWN) : AnalyticsConsentStorage {
        override fun read() = value
        override fun write(value: AnalyticsConsent) { this.value = value }
    }
    private class Transport : AnalyticsTransport {
        var collectionEnabled = false
        var resets = 0
        override fun setEnabled(enabled: Boolean) { this.collectionEnabled = enabled }
        override fun reset() { resets++ }
        override fun log(name: String, parameters: Map<String, String>) = Unit
    }

    private fun show(tracker: AnalyticsTracker) {
        val viewModel = AnalyticsConsentViewModel(tracker)
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            MaterialTheme {
                Column {
                    AnalyticsPreference(viewModel)
                    AnalyticsConsentPrompt(viewModel)
                }
            }
        } }
    }

    @Test fun grantThenWithdrawUpdatesPreferenceAndTransport() {
        val storage = Storage()
        val sink = Transport()
        show(DeviceAnalyticsTracker(storage, sink, true))
        compose.onNodeWithText("İzin ver").performClick()
        compose.onNodeWithText("İzin verme").assertDoesNotExist()
        compose.onNode(isToggleable()).assertIsOn().performClick()
        compose.onNode(isToggleable()).assertIsOff()
        compose.runOnIdle {
            assertEquals(AnalyticsConsent.DENIED, storage.value)
            assertFalse(sink.collectionEnabled)
            assertEquals(1, sink.resets)
        }
    }

    @Test fun declineKeepsCollectionOffAfterRestart() {
        val storage = Storage()
        val sink = Transport()
        show(DeviceAnalyticsTracker(storage, sink, true))
        compose.onNodeWithText("İzin verme").performClick()
        compose.onNodeWithText("İzin ver").assertDoesNotExist()
        compose.onNode(isToggleable()).assertIsOff()
        compose.runOnIdle {
            val restarted = DeviceAnalyticsTracker(storage, sink, true)
            assertEquals(AnalyticsConsent.DENIED, restarted.consent.value)
            assertFalse(sink.collectionEnabled)
        }
    }
}
