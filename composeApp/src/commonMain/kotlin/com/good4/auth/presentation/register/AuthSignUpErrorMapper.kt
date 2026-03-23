package com.good4.auth.presentation.register

import com.good4.auth.domain.AuthError
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_account_disabled
import good4.composeapp.generated.resources.error_email_already_in_use
import good4.composeapp.generated.resources.error_email_invalid_format
import good4.composeapp.generated.resources.error_invalid_credentials
import good4.composeapp.generated.resources.error_network_connection_short
import good4.composeapp.generated.resources.error_recent_login_required
import good4.composeapp.generated.resources.error_sign_up_generic
import good4.composeapp.generated.resources.error_too_many_login_attempts
import good4.composeapp.generated.resources.error_user_not_found
import good4.composeapp.generated.resources.error_user_not_logged_in
import good4.composeapp.generated.resources.error_weak_password

/** Maps [com.good4.auth.data.repository.AuthRepository.signUp] errors to localized [UiText]. */
fun mapAuthSignUpErrorToUiText(authError: AuthError): UiText {
    return when (authError) {
        is AuthError.EmailAlreadyInUse ->
            UiText.StringResourceId(Res.string.error_email_already_in_use)

        is AuthError.WeakPassword ->
            UiText.StringResourceId(Res.string.error_weak_password)

        is AuthError.NetworkError ->
            UiText.StringResourceId(Res.string.error_network_connection_short)

        is AuthError.InvalidEmail ->
            UiText.StringResourceId(Res.string.error_email_invalid_format)

        is AuthError.InvalidCredentials ->
            UiText.StringResourceId(Res.string.error_invalid_credentials)

        is AuthError.UserNotFound ->
            UiText.StringResourceId(Res.string.error_user_not_found)

        is AuthError.TooManyRequests ->
            UiText.StringResourceId(Res.string.error_too_many_login_attempts)

        is AuthError.AccountDisabled ->
            UiText.StringResourceId(Res.string.error_account_disabled)

        is AuthError.UserNotLoggedIn ->
            UiText.StringResourceId(Res.string.error_user_not_logged_in)

        is AuthError.RequiresRecentLogin ->
            UiText.StringResourceId(Res.string.error_recent_login_required)

        is AuthError.Unknown -> uiTextFromUnknownAuthDetail(authError.message)
    }
}

private fun uiTextFromUnknownAuthDetail(raw: String): UiText {
    val detail = raw.trim()
    if (detail.isEmpty()) {
        return UiText.StringResourceId(Res.string.error_sign_up_generic)
    }
    val d = detail.lowercase()
    return when {
        d.contains("email-already-in-use") ||
            d.contains("email_already_in_use") ||
            (d.contains("already in use") && d.contains("email")) ->
            UiText.StringResourceId(Res.string.error_email_already_in_use)

        d.contains("weak-password") || d.contains("weak_password") ->
            UiText.StringResourceId(Res.string.error_weak_password)

        d.contains("network") ||
            d.contains("timeout") ||
            d.contains("unreachable") ||
            d.contains("connection") && d.contains("refused") ->
            UiText.StringResourceId(Res.string.error_network_connection_short)

        d.contains("too-many-requests") || d.contains("too_many_requests") ->
            UiText.StringResourceId(Res.string.error_too_many_login_attempts)

        d.contains("user-disabled") || d.contains("user_disabled") ->
            UiText.StringResourceId(Res.string.error_account_disabled)

        d.contains("invalid-email") || d.contains("invalid_email") ->
            UiText.StringResourceId(Res.string.error_email_invalid_format)

        else -> UiText.StringResourceId(Res.string.error_sign_up_generic)
    }
}
