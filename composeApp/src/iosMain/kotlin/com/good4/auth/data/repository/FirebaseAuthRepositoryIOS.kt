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
            val mappedError = mapFirebaseSignInError(message = e.message.orEmpty())
            if (mappedError != null) {
                Result.Error(mappedError)
            } else {
                Result.Error(AuthError.Unknown(e.message.orEmpty()))
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
            mapFirebaseAuthError(e.message.orEmpty())?.let { mappedError ->
                Result.Error(mappedError)
            } ?: Result.Error(AuthError.Unknown(e.message.orEmpty()))
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
            mapFirebaseAuthError(e.message.orEmpty())?.let { mappedError ->
                Result.Error(mappedError)
            } ?: Result.Error(AuthError.Unknown(e.message.orEmpty()))
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit, AuthError> {
        val user = firebaseAuth.currentUser ?: return Result.Error(AuthError.UserNotLoggedIn)
        return try {
            user.sendEmailVerification()
            Result.Success(Unit)
        } catch (e: Exception) {
            mapFirebaseAuthError(e.message.orEmpty())?.let { mappedError ->
                Result.Error(mappedError)
            } ?: Result.Error(AuthError.Unknown(e.message.orEmpty()))
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
            mapFirebaseAuthError(e.message.orEmpty())?.let { mappedError ->
                Result.Error(mappedError)
            } ?: Result.Error(AuthError.Unknown(e.message.orEmpty()))
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

private fun mapFirebaseSignInError(message: String): AuthError? {
    return mapFirebaseAuthError(message)
}

private fun mapFirebaseAuthError(message: String): AuthError? {
    val firebaseCode = extractFirebaseIosAuthErrorCode(message)
    val normalizedMessage = message.lowercase()

    val fromCode = when (firebaseCode) {
        17004, 17009 -> AuthError.InvalidCredentials
        17005 -> AuthError.AccountDisabled
        17007 -> AuthError.EmailAlreadyInUse
        17008 -> AuthError.InvalidEmail
        17010 -> AuthError.TooManyRequests
        17011 -> AuthError.UserNotFound
        17014 -> AuthError.RequiresRecentLogin
        17020 -> AuthError.NetworkError
        17026 -> AuthError.WeakPassword
        else -> null
    }
    if (fromCode != null) {
        return fromCode
    }

    return when {
        normalizedMessage.contains("invalid-email") ||
            normalizedMessage.contains("invalid_email") ||
            normalizedMessage.contains("badly formatted") -> {
            AuthError.InvalidEmail
        }

        normalizedMessage.contains("too-many-requests") ||
            normalizedMessage.contains("too_many_requests") ||
            normalizedMessage.contains("error_too_many_requests") ||
            normalizedMessage.contains("too many requests") ||
            normalizedMessage.contains("too many unsuccessful login attempts") ||
            normalizedMessage.contains("temporarily disabled") -> {
            AuthError.TooManyRequests
        }

        normalizedMessage.contains("user-disabled") ||
            normalizedMessage.contains("user_disabled") ||
            normalizedMessage.contains("error_user_disabled") ||
            normalizedMessage.contains("account has been disabled") -> {
            AuthError.AccountDisabled
        }

        normalizedMessage.contains("invalid-credential") ||
            normalizedMessage.contains("invalid_credential") ||
            normalizedMessage.contains("error_invalid_credential") ||
            normalizedMessage.contains("invalid_login_credentials") ||
            normalizedMessage.contains("error_invalid_login_credentials") ||
            normalizedMessage.contains("wrong-password") ||
            normalizedMessage.contains("wrong password") ||
            normalizedMessage.contains("password is invalid") ||
            normalizedMessage.contains("invalid login credentials") -> {
            AuthError.InvalidCredentials
        }

        normalizedMessage.contains("user-not-found") ||
            normalizedMessage.contains("user_not_found") ||
            normalizedMessage.contains("error_user_not_found") ||
            normalizedMessage.contains("no user record") ||
            normalizedMessage.contains("there is no user record") -> {
            AuthError.UserNotFound
        }

        normalizedMessage.contains("weak-password") ||
            normalizedMessage.contains("weak_password") ||
            normalizedMessage.contains("error_weak_password") -> {
            AuthError.WeakPassword
        }

        normalizedMessage.contains("email-already-in-use") ||
            normalizedMessage.contains("email_already_in_use") ||
            normalizedMessage.contains("error_email_already_in_use") -> {
            AuthError.EmailAlreadyInUse
        }

        normalizedMessage.contains("network") ||
            normalizedMessage.contains("network-request-failed") ||
            normalizedMessage.contains("network_request_failed") ||
            normalizedMessage.contains("timeout") ||
            normalizedMessage.contains("unreachable") ||
            normalizedMessage.contains("recaptcha") -> {
            AuthError.NetworkError
        }

        normalizedMessage.contains("requires-recent-login") ||
            normalizedMessage.contains("requires_recent_login") ||
            normalizedMessage.contains("error_requires_recent_login") ->
            AuthError.RequiresRecentLogin

        else -> null
    }
}

private fun extractFirebaseIosAuthErrorCode(message: String): Int? {
    val codeRegex = Regex("""\bcode\s*=\s*(\d{4,6})\b""", RegexOption.IGNORE_CASE)
    return codeRegex.find(message)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
}
