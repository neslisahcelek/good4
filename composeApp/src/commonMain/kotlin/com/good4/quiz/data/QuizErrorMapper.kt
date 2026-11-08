package com.good4.quiz.data

import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.*

fun mapQuizErrorToUiText(rawError: String?): UiText {
    if (rawError.isNullOrBlank()) return UiText.StringResourceId(Res.string.quiz_error_generic)
    val code = rawError.trim()
    return when {
        code.contains("QUIZ_NOT_FOLLOWER", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_not_follower)
        code.contains("QUIZ_LOCKED", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_locked)
        code.contains("QUIZ_FULL", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_full)
        code.contains("QUIZ_NICKNAME_TAKEN", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_nickname_taken)
        code.contains("QUIZ_NICKNAME_INVALID", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_nickname_invalid)
        code.contains("QUIZ_NICKNAME_BLOCKED", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_nickname_blocked)
        code.contains("QUIZ_KICKED", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_kicked)
        code.contains("QUIZ_HOST_CANNOT_PLAY", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_host_cannot_play)
        code.contains("QUIZ_SESSION_ENDED", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_session_ended)
        code.contains("QUIZ_ALREADY_ANSWERED", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_already_answered)
        code.contains("QUIZ_TIME_UP", ignoreCase = true) ||
            code.contains("QUIZ_NOT_ACCEPTING_ANSWERS", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_time_up)
        code.contains("QUIZ_NO_PLAYERS", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_no_players)
        code.contains("QUIZ_MANAGER_REQUIRED", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_manager_required)
        code.contains("QUIZ_NOT_READY", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_not_ready)
        code.contains("QUIZ_SESSION_ACTIVE", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_session_active)
        code.contains("QUIZ_SESSION_NOT_FOUND", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_session_not_found)
        code.contains("QUIZ_REPORT_NOT_FOUND", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_report_not_found)
        code.contains("PERMISSION_DENIED", ignoreCase = true) ->
            UiText.StringResourceId(Res.string.quiz_error_not_follower)
        code.contains("UNAVAILABLE", ignoreCase = true) ||
            code.contains("NETWORK", ignoreCase = true) ||
            code.contains("INTERNAL", ignoreCase = true) ||
            code.contains("Exception", ignoreCase = true) ||
            code.contains(":") ->
            UiText.StringResourceId(Res.string.quiz_error_generic)
        // Any other server code must not reach the screen as a raw identifier.
        Regex("QUIZ_[A-Z_]+").containsMatchIn(code) ->
            UiText.StringResourceId(Res.string.quiz_error_generic)
        else -> UiText.DynamicString(rawError)
    }
}
