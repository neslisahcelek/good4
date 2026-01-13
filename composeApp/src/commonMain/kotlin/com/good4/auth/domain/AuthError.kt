package com.good4.auth.domain

import com.good4.core.domain.Error

sealed class AuthError : Error {
    data object InvalidCredentials : AuthError() {
        override val message: String = "Geçersiz e-posta veya şifre"
    }

    data object UserNotFound : AuthError() {
        override val message: String = "Kullanıcı bulunamadı"
    }

    data object EmailAlreadyInUse : AuthError() {
        override val message: String = "Bu e-posta adresi zaten kullanılıyor"
    }

    data object WeakPassword : AuthError() {
        override val message: String = "Şifre en az 6 karakter olmalıdır"
    }

    data object NetworkError : AuthError() {
        override val message: String = "İnternet bağlantısı hatası"
    }

    data object UserNotLoggedIn : AuthError() {
        override val message: String = "Kullanıcı giriş yapmamış"
    }

    data class Unknown(override val message: String) : AuthError()
}

