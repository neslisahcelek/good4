package com.good4.auth.data.repository

import com.good4.auth.domain.AuthError
import com.good4.auth.domain.AuthUser
import com.good4.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Desktop için Auth implementasyonu.
 * Not: Desktop'ta Firebase doğrudan desteklenmiyor.
 * Gerçek kullanım için REST API veya başka bir çözüm gerekiyor.
 */
class AuthRepositoryDesktop : AuthRepository {
    private val _authState = MutableStateFlow<AuthUser?>(null)

    override val currentUser: AuthUser?
        get() = _authState.value

    override val authStateFlow: Flow<AuthUser?> = _authState

    override suspend fun signIn(email: String, password: String): Result<AuthUser, AuthError> {
        // Desktop için gerçek implementasyon gerekiyor
        // Şimdilik hata döndürüyoruz
        return Result.Error(AuthError.Unknown("Desktop platformu henüz desteklenmiyor"))
    }

    override suspend fun signUp(email: String, password: String): Result<AuthUser, AuthError> {
        return Result.Error(AuthError.Unknown("Desktop platformu henüz desteklenmiyor"))
    }

    override suspend fun signOut(): Result<Unit, AuthError> {
        _authState.value = null
        return Result.Success(Unit)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit, AuthError> {
        return Result.Error(AuthError.Unknown("Desktop platformu henüz desteklenmiyor"))
    }

    override fun isLoggedIn(): Boolean {
        return _authState.value != null
    }
}

