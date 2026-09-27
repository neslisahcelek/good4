package com.good4.auth.presentation.login

import com.good4.auth.domain.AuthError
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_account_disabled
import good4.composeapp.generated.resources.error_email_invalid_format
import good4.composeapp.generated.resources.error_invalid_credentials
import good4.composeapp.generated.resources.error_network_connection
import good4.composeapp.generated.resources.error_too_many_login_attempts
import good4.composeapp.generated.resources.error_unknown
import good4.composeapp.generated.resources.error_user_not_found

/** Maps [com.good4.auth.data.repository.AuthRepository.signIn] errors to localized [UiText]. */
fun mapAuthLoginErrorToUiText(authError: AuthError): UiText {
    return when (authError) {
        is AuthError.InvalidEmail -> UiText.StringResourceId(Res.string.error_email_invalid_format)
        is AuthError.InvalidCredentials -> UiText.StringResourceId(Res.string.error_invalid_credentials)
        is AuthError.UserNotFound -> UiText.StringResourceId(Res.string.error_user_not_found)
        is AuthError.NetworkError -> UiText.StringResourceId(Res.string.error_network_connection)
        is AuthError.TooManyRequests -> UiText.StringResourceId(Res.string.error_too_many_login_attempts)
        is AuthError.AccountDisabled -> UiText.StringResourceId(Res.string.error_account_disabled)
        is AuthError.Unknown -> uiTextFromUnknownLoginDetail(authError.message)
        else -> UiText.StringResourceId(Res.string.error_unknown)
    }
}

private fun uiTextFromUnknownLoginDetail(raw: String): UiText {
    val detail = raw.trim()
    if (detail.isEmpty()) {
        return UiText.StringResourceId(Res.string.error_unknown)
    }

    val d = detail.lowercase()
    return when {
        d.contains("user-not-found") ||
            d.contains("user_not_found") ||
            d.contains("error_user_not_found") ||
            d.contains("no user record") ||
            d.contains("there is no user record") ->
            UiText.StringResourceId(Res.string.error_user_not_found)

        d.contains("invalid-credential") ||
            d.contains("invalid_credential") ||
            d.contains("error_invalid_credential") ||
            d.contains("invalid_login_credentials") ||
            d.contains("error_invalid_login_credentials") ||
            d.contains("wrong-password") ||
            d.contains("wrong password") ||
            d.contains("password is invalid") ->
            UiText.StringResourceId(Res.string.error_invalid_credentials)

        d.contains("too-many-requests") ||
            d.contains("too_many_requests") ||
            d.contains("error_too_many_requests") ||
            d.contains("temporarily disabled") ->
            UiText.StringResourceId(Res.string.error_too_many_login_attempts)

        d.contains("user-disabled") ||
            d.contains("user_disabled") ||
            d.contains("error_user_disabled") ||
            d.contains("account has been disabled") ->
            UiText.StringResourceId(Res.string.error_account_disabled)

        d.contains("network") ||
            d.contains("network-request-failed") ||
            d.contains("network_request_failed") ||
            d.contains("timeout") ||
            d.contains("unreachable") ||
            d.contains("connection refused") ->
            UiText.StringResourceId(Res.string.error_network_connection)

        d.contains("invalid-email") ||
            d.contains("invalid_email") ||
            d.contains("badly formatted") ->
            UiText.StringResourceId(Res.string.error_email_invalid_format)

        else -> UiText.StringResourceId(Res.string.error_unknown)
    }
}
