package com.good4.auth.data.repository

import com.good4.auth.domain.AuthError
import com.good4.auth.domain.AuthUser
import com.good4.core.domain.Result
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseAuthRepositoryIOS : AuthRepository {
    private val firebaseAuth: FirebaseAuth = Firebase.auth

    override val currentUser: AuthUser?
        get() = firebaseAuth.currentUser?.toAuthUser()

    override val authStateFlow: Flow<AuthUser?> = firebaseAuth.authStateChanged.map { user ->
        user?.toAuthUser()
    }

    override suspend fun signIn(email: String, password: String): Result<AuthUser, AuthError> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password)
            val user = result.user?.toAuthUser()
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(AuthError.UserNotFound)
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: ""
            when {
                errorMessage.contains("invalid-credential") ||
                errorMessage.contains("wrong-password") ||
                errorMessage.contains("invalid-email") -> {
                    Result.Error(AuthError.InvalidCredentials)
                }
                errorMessage.contains("user-not-found") -> {
                    Result.Error(AuthError.UserNotFound)
                }
                errorMessage.contains("network") -> {
                    Result.Error(AuthError.NetworkError)
                }
                else -> {
                    Result.Error(AuthError.Unknown(e.message.orEmpty()))
                }
            }
        }
    }

    override suspend fun signUp(email: String, password: String): Result<AuthUser, AuthError> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password)
            val user = result.user?.toAuthUser()
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(AuthError.Unknown(""))
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: ""
            when {
                errorMessage.contains("weak-password") -> {
                    Result.Error(AuthError.WeakPassword)
                }
                errorMessage.contains("email-already-in-use") -> {
                    Result.Error(AuthError.EmailAlreadyInUse)
                }
                errorMessage.contains("network") -> {
                    Result.Error(AuthError.NetworkError)
                }
                else -> {
                    Result.Error(AuthError.Unknown(e.message.orEmpty()))
                }
            }
        }
    }

    override suspend fun signOut(): Result<Unit, AuthError> {
        return try {
            firebaseAuth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AuthError.Unknown(e.message.orEmpty()))
        }
    }

    override suspend fun deleteCurrentUser(): Result<Unit, AuthError> {
        val user = firebaseAuth.currentUser ?: return Result.Error(AuthError.UserNotLoggedIn)
        return try {
            user.delete()
            Result.Success(Unit)
        } catch (e: Exception) {
            val errorMessage = e.message ?: ""
            when {
                errorMessage.contains("requires-recent-login") -> {
                    Result.Error(AuthError.RequiresRecentLogin)
                }
                errorMessage.contains("network") -> {
                    Result.Error(AuthError.NetworkError)
                }
                else -> {
                    Result.Error(AuthError.Unknown(e.message.orEmpty()))
                }
            }
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit, AuthError> {
        val user = firebaseAuth.currentUser ?: return Result.Error(AuthError.UserNotLoggedIn)
        return try {
            user.sendEmailVerification()
            Result.Success(Unit)
        } catch (e: Exception) {
            val errorMessage = e.message.orEmpty()
            when {
                errorMessage.contains("network", ignoreCase = true) ||
                    errorMessage.contains("timeout", ignoreCase = true) ||
                    errorMessage.contains("unreachable", ignoreCase = true) ||
                    errorMessage.contains("recaptcha", ignoreCase = true) -> {
                    Result.Error(AuthError.NetworkError)
                }
                errorMessage.contains("user-not-found", ignoreCase = true) ||
                    errorMessage.contains("no user record", ignoreCase = true) -> {
                    Result.Error(AuthError.UserNotFound)
                }
                else -> {
                    Result.Error(AuthError.Unknown(errorMessage))
                }
            }
        }
    }

    override suspend fun reloadCurrentUser(): Result<AuthUser, AuthError> {
        val user = firebaseAuth.currentUser ?: return Result.Error(AuthError.UserNotLoggedIn)
        return try {
            user.reload()
            val refreshed = firebaseAuth.currentUser?.toAuthUser()
            if (refreshed != null) {
                Result.Success(refreshed)
            } else {
                Result.Error(AuthError.UserNotFound)
            }
        } catch (e: Exception) {
            Result.Error(AuthError.Unknown(e.message.orEmpty()))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit, AuthError> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email)
            Result.Success(Unit)
        } catch (e: Exception) {
            val errorMessage = e.message ?: ""
            when {
                errorMessage.contains("user-not-found") -> {
                    Result.Error(AuthError.UserNotFound)
                }
                else -> {
                    Result.Error(AuthError.Unknown(e.message.orEmpty()))
                }
            }
        }
    }

    override fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    private fun FirebaseUser.toAuthUser(): AuthUser {
        return AuthUser(
            uid = uid,
            email = email,
            displayName = displayName,
            isEmailVerified = isEmailVerified
        )
    }
}

