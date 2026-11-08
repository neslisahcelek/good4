package com.good4.quiz

import com.good4.quiz.presentation.join.parseQuizJoinLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuizJoinLinksTest {
    @Test
    fun readsSessionIdFromSupportedLinkShapes() {
        assertEquals("sF34CtpmmKbyTH2X7LkG", parseQuizJoinLink("good4://quiz?session=sF34CtpmmKbyTH2X7LkG"))
        assertEquals("abc_123-XY", parseQuizJoinLink("good4://quiz?sessionId=abc_123-XY"))
        assertEquals("abc123", parseQuizJoinLink("good4://quiz/abc123"))
        assertEquals("abc123", parseQuizJoinLink("GOOD4://quiz?utm=qr&session=abc123#top"))
    }

    @Test
    fun rejectsOtherLinksAndUnsafeIds() {
        listOf(
            "good4://quizzes?session=abc123",
            "good4://social?session=abc123",
            "https://good4tr-v2.web.app/quiz?session=abc123",
            "good4://quiz",
            "good4://quiz?session=",
            "good4://quiz?session=../etc",
            "good4://quiz?session=a%2Fb",
            "good4://quiz/a/b",
            "good4://quiz?session=" + "a".repeat(129),
        ).forEach { assertNull(parseQuizJoinLink(it), it) }
    }
}
