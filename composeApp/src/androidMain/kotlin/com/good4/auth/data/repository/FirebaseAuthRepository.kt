package com.good4.auth.data.repository

import com.good4.auth.domain.AuthError
import com.good4.auth.domain.AuthUser
import com.good4.core.domain.Result
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class FirebaseAuthRepository : AuthRepository {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override val currentUser: AuthUser?
        get() = firebaseAuth.currentUser?.toAuthUser()

    override val authStateFlow: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Result<AuthUser, AuthError> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user?.toAuthUser()
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(AuthError.UserNotFound)
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.Error(
                mapFirebaseSignInError(
                    errorCode = null,
                    errorMessage = e.message.orEmpty()
                ) ?: AuthError.InvalidCredentials
            )
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.Error(
                mapFirebaseSignInError(
                    errorCode = null,
                    errorMessage = e.message.orEmpty()
                ) ?: AuthError.UserNotFound
            )
        } catch (e: FirebaseAuthException) {
            val mappedError = mapFirebaseSignInError(
                errorCode = e.errorCode,
                errorMessage = e.message.orEmpty()
            )
            if (mappedError != null) {
                Result.Error(mappedError)
            } else {
                Result.Error(AuthError.Unknown(e.message.orEmpty()))
            }
        } catch (e: FirebaseException) {
            val mappedError = mapFirebaseSignInError(
                errorCode = null,
                errorMessage = e.message.orEmpty()
            )
            if (mappedError != null) {
                Result.Error(mappedError)
            } else {
                Result.Error(AuthError.Unknown(e.message.orEmpty()))
            }
        } catch (e: UnknownHostException) {
            Result.Error(AuthError.NetworkError)
        } catch (e: SocketTimeoutException) {
            Result.Error(AuthError.NetworkError)
        } catch (e: Exception) {
            val mappedError = mapFirebaseSignInError(
                errorCode = null,
                errorMessage = e.message.orEmpty()
            )
            if (mappedError != null) {
                Result.Error(mappedError)
            } else {
                Result.Error(AuthError.Unknown(e.message.orEmpty()))
            }
        }
    }

    override suspend fun signUp(email: String, password: String): Result<AuthUser, AuthError> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user?.toAuthUser()
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(AuthError.Unknown(""))
            }
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.Error(AuthError.WeakPassword)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.Error(AuthError.EmailAlreadyInUse)
        } catch (e: FirebaseAuthException) {
            val errorCode = e.errorCode
            when {
                errorCode.contains("NETWORK", ignoreCase = true) ||
                    errorCode.contains("INTERNAL", ignoreCase = true) ||
                    e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("recaptcha", ignoreCase = true) == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true -> {
                    Result.Error(AuthError.NetworkError)
                }
                errorCode.contains("WEAK_PASSWORD", ignoreCase = true) -> {
                    Result.Error(AuthError.WeakPassword)
                }
                errorCode.contains("EMAIL_ALREADY_IN_USE", ignoreCase = true) -> {
                    Result.Error(AuthError.EmailAlreadyInUse)
                }
                else -> Result.Error(AuthError.Unknown(e.message.orEmpty()))
            }
        } catch (e: FirebaseException) {
            if (e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("recaptcha", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true
            ) {
                Result.Error(AuthError.NetworkError)
            } else {
                Result.Error(AuthError.Unknown(e.message.orEmpty()))
            }
        } catch (e: UnknownHostException) {
            Result.Error(AuthError.NetworkError)
        } catch (e: SocketTimeoutException) {
            Result.Error(AuthError.NetworkError)
        } catch (e: Exception) {
            if (e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("unreachable", ignoreCase = true) == true ||
                e.message?.contains("recaptcha", ignoreCase = true) == true
            ) {
                Result.Error(AuthError.NetworkError)
            } else {
                Result.Error(AuthError.Unknown(e.message.orEmpty()))
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
            user.delete().await()
            Result.Success(Unit)
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Result.Error(AuthError.RequiresRecentLogin)
        } catch (e: FirebaseAuthException) {
            val errorCode = e.errorCode ?: ""
            when {
                errorCode.contains("NETWORK", ignoreCase = true) ||
                    errorCode.contains("INTERNAL", ignoreCase = true) ||
                    e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("recaptcha", ignoreCase = true) == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true -> {
                    Result.Error(AuthError.NetworkError)
                }
                else -> Result.Error(AuthError.Unknown(e.message.orEmpty()))
            }
        } catch (e: FirebaseException) {
            if (e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("recaptcha", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true
            ) {
                Result.Error(AuthError.NetworkError)
            } else {
                Result.Error(AuthError.Unknown(e.message.orEmpty()))
            }
        } catch (e: Exception) {
            if (e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("unreachable", ignoreCase = true) == true ||
                e.message?.contains("recaptcha", ignoreCase = true) == true
            ) {
                Result.Error(AuthError.NetworkError)
            } else {
                Result.Error(AuthError.Unknown(e.message.orEmpty()))
            }
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit, AuthError> {
        val user = firebaseAuth.currentUser ?: return Result.Error(AuthError.UserNotLoggedIn)
        return try {
            user.sendEmailVerification().await()
            Result.Success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.Error(AuthError.UserNotFound)
        } catch (e: FirebaseAuthException) {
            Result.Error(AuthError.Unknown(e.message.orEmpty()))
        } catch (e: Exception) {
            Result.Error(AuthError.Unknown(e.message.orEmpty()))
        }
    }

    override suspend fun reloadCurrentUser(): Result<AuthUser, AuthError> {
        val user = firebaseAuth.currentUser ?: return Result.Error(AuthError.UserNotLoggedIn)
        return try {
            user.reload().await()
            val refreshedUser = firebaseAuth.currentUser
            val refreshed = refreshedUser?.let { firebaseUser ->
                // Force-refresh token claims; use email_verified claim when available.
                val emailVerifiedFromToken = try {
                    firebaseUser.getIdToken(true).await()
                        .claims["email_verified"] as? Boolean
                } catch (_: Exception) {
                    null
                }
                val authUser = firebaseUser.toAuthUser()
                if (emailVerifiedFromToken != null && emailVerifiedFromToken != authUser.isEmailVerified) {
                    authUser.copy(isEmailVerified = emailVerifiedFromToken)
                } else {
                    authUser
                }
            }
            if (refreshed != null) {
                Result.Success(refreshed)
            } else {
                Result.Error(AuthError.UserNotFound)
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.Error(AuthError.UserNotFound)
        } catch (e: FirebaseAuthException) {
            Result.Error(AuthError.Unknown(e.message.orEmpty()))
        } catch (e: Exception) {
            Result.Error(AuthError.Unknown(e.message.orEmpty()))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit, AuthError> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.Success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.Error(AuthError.UserNotFound)
        } catch (e: Exception) {
            Result.Error(AuthError.Unknown(e.message.orEmpty()))
        }
    }

    override fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    private fun com.google.firebase.auth.FirebaseUser.toAuthUser(): AuthUser {
        return AuthUser(
            uid = uid,
            email = email,
            displayName = displayName,
            isEmailVerified = isEmailVerified
        )
    }
}

private fun mapFirebaseSignInError(errorCode: String?, errorMessage: String): AuthError? {
    val normalizedCode = errorCode.orEmpty()
    val normalizedMessage = errorMessage.lowercase()

    return when {
        normalizedCode.contains("INVALID_EMAIL", ignoreCase = true) ||
            normalizedMessage.contains("invalid-email") ||
            normalizedMessage.contains("badly formatted") -> {
            AuthError.InvalidEmail
        }

        normalizedCode.contains("TOO_MANY_REQUESTS", ignoreCase = true) ||
            normalizedMessage.contains("too-many-requests") ||
            normalizedMessage.contains("too many requests") ||
            normalizedMessage.contains("too many unsuccessful login attempts") ||
            normalizedMessage.contains("temporarily disabled") -> {
            AuthError.TooManyRequests
        }

        normalizedCode.contains("USER_DISABLED", ignoreCase = true) ||
            normalizedMessage.contains("user-disabled") ||
            normalizedMessage.contains("account has been disabled") -> {
            AuthError.AccountDisabled
        }

        normalizedCode.contains("NETWORK", ignoreCase = true) ||
            normalizedCode.contains("INTERNAL", ignoreCase = true) ||
            normalizedMessage.contains("network") ||
            normalizedMessage.contains("timeout") ||
            normalizedMessage.contains("recaptcha") ||
            normalizedMessage.contains("unreachable") -> {
            AuthError.NetworkError
        }

        normalizedCode.contains("INVALID_CREDENTIAL", ignoreCase = true) ||
            normalizedCode.contains("WRONG_PASSWORD", ignoreCase = true) ||
            normalizedMessage.contains("invalid-credential") ||
            normalizedMessage.contains("wrong-password") ||
            normalizedMessage.contains("invalid login credentials") -> {
            AuthError.InvalidCredentials
        }

        normalizedCode.contains("USER_NOT_FOUND", ignoreCase = true) ||
            normalizedMessage.contains("user-not-found") ||
            normalizedMessage.contains("no user record") -> {
            AuthError.UserNotFound
        }

        else -> null
    }
}
