package com.good4.auth.data.repository

import com.good4.auth.domain.AuthError
import com.good4.auth.domain.AuthUser
import com.good4.core.domain.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: AuthUser?
    val authStateFlow: Flow<AuthUser?>

    suspend fun signIn(email: String, password: String): Result<AuthUser, AuthError>
    suspend fun signUp(email: String, password: String): Result<AuthUser, AuthError>
    suspend fun signOut(): Result<Unit, AuthError>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit, AuthError>
    fun isLoggedIn(): Boolean
}

