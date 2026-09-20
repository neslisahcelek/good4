package com.good4.core.util

import android.content.ActivityNotFoundException
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapsLaunchTest {
    @Test fun webLinkIsOpenedWithoutBeingConvertedToSearchText() {
        val intents = mutableListOf<Intent>()
        launchMap(" https://maps.app.goo.gl/example ") { intents.add(it) }
        assertEquals("https://maps.app.goo.gl/example", intents.single().dataString)
        assertTrue(intents.single().flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun missingMapsAppFallsBackToBrowserWithEncodedAddress() {
        val intents = mutableListOf<Intent>()
        launchMap("Antalya & Kampüs") {
            intents.add(it)
            if (intents.size == 1) throw ActivityNotFoundException()
        }
        assertEquals("geo", intents.first().data?.scheme)
        assertEquals("Antalya & Kampüs", intents.last().data?.getQueryParameter("q"))
        assertEquals("https", intents.last().data?.scheme)
    }

    @Test fun missingBrowserDoesNotCrashOrSearchForTheUrl() {
        var attempts = 0
        launchMap("https://maps.app.goo.gl/example") {
            attempts++
            throw ActivityNotFoundException()
        }
        assertEquals(1, attempts)
    }

    @Test fun noHandlersDoesNotCrashAndBlankAddressDoesNotLaunch() {
        var attempts = 0
        launchMap("Antalya") { attempts++; throw ActivityNotFoundException() }
        assertEquals(2, attempts)
        launchMap("  ") { throw AssertionError("Blank address must not launch") }
    }
}
