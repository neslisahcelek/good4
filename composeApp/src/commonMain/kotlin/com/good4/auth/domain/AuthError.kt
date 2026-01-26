package com.good4.auth.domain

import com.good4.core.domain.Error

sealed class AuthError : Error {
    data object InvalidCredentials : AuthError() {
        override val message: String = ""
    }

    data object UserNotFound : AuthError() {
        override val message: String = ""
    }

    data object EmailAlreadyInUse : AuthError() {
        override val message: String = ""
    }

    data object WeakPassword : AuthError() {
        override val message: String = ""
    }

    data object NetworkError : AuthError() {
        override val message: String = ""
    }

    data object UserNotLoggedIn : AuthError() {
        override val message: String = ""
    }

    data class Unknown(override val message: String) : AuthError()
}
