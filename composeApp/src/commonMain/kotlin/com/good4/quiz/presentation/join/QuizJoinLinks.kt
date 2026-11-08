package com.good4.quiz.presentation.join

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val QUIZ_LINK_PREFIX = "good4://quiz"
private val QuizSessionIdPattern = Regex("^[A-Za-z0-9_-]{1,128}$")

/**
 * Reads the session id from a live quiz join link opened by the web join page or the host's QR code:
 * `good4://quiz?session=<id>`, `good4://quiz?sessionId=<id>` or `good4://quiz/<id>`.
 * Anything else, including ids with unexpected characters, is rejected.
 */
internal fun parseQuizJoinLink(raw: String): String? {
    val link = raw.trim().substringBefore('#')
    if (!link.startsWith(QUIZ_LINK_PREFIX, ignoreCase = true)) return null
    val rest = link.substring(QUIZ_LINK_PREFIX.length)
    if (rest.isNotEmpty() && rest[0] != '/' && rest[0] != '?') return null
    val path = rest.substringBefore('?').trim('/')
    val fromQuery = rest.substringAfter('?', "")
        .split('&')
        .firstNotNullOfOrNull { part ->
            val key = part.substringBefore('=')
            if (key == "session" || key == "sessionId") part.substringAfter('=', "") else null
        }
    val sessionId = fromQuery ?: path.takeIf { it.isNotEmpty() }
    return sessionId?.takeIf { QuizSessionIdPattern.matches(it) }
}

/** Holds a join link until navigation is ready (signed in, past splash/login) to open the quiz. */
object QuizJoinLinks {
    private val incoming = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = incoming.asStateFlow()

    /** Returns true when [url] was a quiz join link, so callers can skip other link handlers. */
    fun receive(url: String): Boolean {
        val sessionId = parseQuizJoinLink(url) ?: return false
        incoming.value = sessionId
        return true
    }

    fun consume(sessionId: String) {
        if (incoming.value == sessionId) incoming.value = null
    }
}
