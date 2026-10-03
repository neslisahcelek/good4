package com.good4.campuscloset

import io.ktor.http.encodeURLParameter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CampusEmailVerificationTest {
    private val requestId = "a".repeat(64)
    private val continueUrl = "https://good4tr-v2.firebaseapp.com/campus-email-verification?requestId=$requestId"
    private val actionUrl = "https://good4tr-v2.firebaseapp.com/__/auth/action?mode=signIn&oobCode=code&apiKey=test-key&continueUrl=${continueUrl.encodeURLParameter()}"

    @Test fun onlyExactStudentDomainIsAccepted() {
        assertTrue(isCampusStudentEmail(" Can@OGR.AKDENIZ.EDU.TR "))
        for (email in listOf("can@akdeniz.edu.tr", "can@ogr.other.edu.tr", "can@sub.ogr.akdeniz.edu.tr",
            "can@ogr.akdeniz.edu.tr.evil.org", "can@@ogr.akdeniz.edu.tr", "can @ogr.akdeniz.edu.tr")) {
            assertFalse(isCampusStudentEmail(email), email)
        }
    }

    @Test fun firebaseHostingWrapperPreservesTheAccountBoundRequest() {
        assertEquals(requestId, parseCampusEmailLink(actionUrl)?.requestId)
        val wrapper = "https://good4tr-v2.firebaseapp.com/__/auth/links?link=${actionUrl.encodeURLParameter()}"
        assertEquals(parseCampusEmailLink(actionUrl), parseCampusEmailLink(wrapper))
        assertEquals(requestId, parseCampusEmailLink(actionUrl.replace("/__/auth/action", "/__/auth/links"))?.requestId)
    }

    @Test fun unrelatedAndMalformedLinksAreRejected() {
        for (link in listOf(actionUrl.replace("https:", "http:"), actionUrl.replace("good4tr-v2.firebaseapp.com", "evil.example"),
            actionUrl.replace("mode=signIn", "mode=verifyEmail"), actionUrl.replace("oobCode=code", "oobCode="),
            actionUrl.replace(requestId, "short"), "garbage", "https://good4tr-v2.firebaseapp.com/")) {
            assertNull(parseCampusEmailLink(link), link)
        }
        val foreignContinueUrl = "https://evil.example/campus-email-verification?requestId=$requestId"
        assertNull(parseCampusEmailLink(actionUrl.replace(continueUrl.encodeURLParameter(), foreignContinueUrl.encodeURLParameter())))
        var foreignAction = actionUrl.replaceFirst(CAMPUS_EMAIL_LINK_HOST, "evil.example")
        repeat(3) {
            foreignAction = "https://$CAMPUS_EMAIL_LINK_HOST/__/auth/links?link=${foreignAction.encodeURLParameter()}"
        }
        assertNull(parseCampusEmailLink(foreignAction))
    }

    @Test fun completeLandingLinksAreNormalizedButRequestOnlyPagesCannotVerify() {
        val landing = "https://good4tr-v2.web.app/campus-email-verification?mode=signIn&oobCode=code&apiKey=test-key&requestId=$requestId"
        val parsed = parseCampusEmailLink(landing)
        assertEquals(requestId, parsed?.requestId)
        assertEquals(parsed, parsed?.firebaseLink?.let(::parseCampusEmailLink))
        assertEquals(parseCampusEmailLink(actionUrl), parseCampusEmailLink(actionUrl.replace("/__/auth/action", "/campus-email-verification")))
        assertNull(parseCampusEmailLink(continueUrl))
        assertNull(parseCampusEmailLink(landing.replace("mode=signIn&", "")))
        assertNull(parseCampusEmailLink(landing.replace("apiKey=test-key&", "")))
    }

    @Test fun ambiguousParametersAndExcessiveWrappersAreRejected() {
        assertNull(parseCampusEmailLink("$actionUrl&oobCode=another"))
        assertNull(parseCampusEmailLink("$actionUrl&requestId=${"b".repeat(64)}"))
        val duplicatedRequest = "$continueUrl&requestId=${"b".repeat(64)}"
        assertNull(parseCampusEmailLink(actionUrl.replace(continueUrl.encodeURLParameter(), duplicatedRequest.encodeURLParameter())))
        assertNull(parseCampusEmailLink(actionUrl.replace("https://good4tr", "https://user@good4tr")))
        var wrapped = actionUrl
        repeat(4) { wrapped = "https://$CAMPUS_EMAIL_LINK_HOST/__/auth/links?link=${wrapped.encodeURLParameter()}" }
        assertNull(parseCampusEmailLink(wrapped))
    }
}
