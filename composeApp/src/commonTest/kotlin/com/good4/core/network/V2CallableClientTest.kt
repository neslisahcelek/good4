package com.good4.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class V2CallableClientTest {
    @Test
    fun testAndProductionCallTheirOwnFirebaseProject() {
        assertEquals(
            "https://europe-west1-good4tr-test.cloudfunctions.net/ensureStudentProfile",
            v2CallableUrl("good4tr-test", "ensureStudentProfile")
        )
        assertEquals(
            "https://europe-west1-good4tr-v2.cloudfunctions.net/ensureStudentProfile",
            v2CallableUrl("good4tr-v2", "ensureStudentProfile")
        )
    }

    @Test
    fun unsupportedProjectsAndPathsAreRejected() {
        assertFailsWith<IllegalArgumentException> { v2CallableUrl("good4tr", "ensureStudentProfile") }
        assertFailsWith<IllegalArgumentException> { v2CallableUrl("good4tr-test", "../ensureStudentProfile") }
    }
}
