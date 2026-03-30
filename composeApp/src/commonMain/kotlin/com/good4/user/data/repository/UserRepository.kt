package com.good4.user.data.repository

import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.core.domain.ValidationError
import com.good4.user.User
import com.good4.user.data.dto.UserDto
import com.good4.user.domain.UserRole
import kotlinx.datetime.Instant

class UserRepository(
    private val firestoreRepository: FirestoreRepository,
    private val configRepository: AppConfigRepository
) {
    suspend fun createUser(userId: String, userDto: UserDto): Result<Unit, Error> {
        return firestoreRepository.updateDocument("users", userId, userDto)
    }

    suspend fun getUser(userId: String): Result<User, Error> {
        return when (val result =
            firestoreRepository.getDocument("users", userId, UserDto::class)) {
            is Result.Success -> Result.Success(result.data.toUser(userId))
            is Result.Error -> result
        }
    }

    suspend fun getUserDto(userId: String): Result<UserDto, Error> {
        return firestoreRepository.getDocument("users", userId, UserDto::class)
    }

    suspend fun updateUser(userId: String, userDto: UserDto): Result<Unit, Error> {
        return firestoreRepository.updateDocument("users", userId, userDto)
    }

    suspend fun updateStudentWeeklyCreditOverride(
        userId: String,
        weeklyCreditOverride: Int?
    ): Result<Unit, Error> {
        return when (val result = getUserDto(userId)) {
            is Result.Success -> {
                val currentUser = result.data
                val role = UserRole.fromValue(currentUser.role)
                if (role != UserRole.STUDENT) {
                    return Result.Error(ValidationError("User is not a student"))
                }

                val syncedCredit = weeklyCreditOverride ?: configRepository.getStudentWeeklyCredit()
                val updatedDto = currentUser.copy(
                    weeklyCreditOverride = weeklyCreditOverride,
                    credit = syncedCredit
                )
                updateUser(userId, updatedDto)
            }

            is Result.Error -> result
        }
    }

    suspend fun updateStudentWeeklyCreditOverrideByIdentifier(
        userId: String?,
        email: String?,
        weeklyCreditOverride: Int?
    ): Result<Unit, Error> {
        val resolvedUserId = when {
            !userId.isNullOrBlank() -> userId
            !email.isNullOrBlank() -> {
                when (val result = resolveUserIdByEmail(email)) {
                    is Result.Success -> result.data
                    is Result.Error -> return result
                }
            }

            else -> return Result.Error(ValidationError("User id or email is required"))
        }

        return updateStudentWeeklyCreditOverride(
            userId = resolvedUserId,
            weeklyCreditOverride = weeklyCreditOverride
        )
    }

    private suspend fun resolveUserIdByEmail(email: String): Result<String, Error> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            return Result.Error(ValidationError("User not found for provided email"))
        }

        val candidateEmails = buildList {
            add(trimmedEmail)
            val lower = trimmedEmail.lowercase()
            if (lower != trimmedEmail) add(lower)
        }

        candidateEmails.forEach { candidate ->
            when (
                val result = firestoreRepository.queryCollectionWithIds(
                    collectionPath = "users",
                    field = "email",
                    value = candidate,
                    clazz = UserDto::class
                )
            ) {
                is Result.Success -> {
                    val match = result.data.firstOrNull()
                    if (match != null) {
                        return Result.Success(match.id)
                    }
                }

                is Result.Error -> {
                    // Continue with other normalized candidate before returning an error.
                }
            }
        }

        return Result.Error(ValidationError("User not found for provided email"))
    }

    suspend fun deleteUser(userId: String): Result<Unit, Error> {
        return firestoreRepository.deleteDocument("users", userId)
    }

    suspend fun markUserVerified(userId: String): Result<Unit, Error> {
        return when (val result = getUserDto(userId)) {
            is Result.Success -> {
                val current = result.data
                if (current.verified == true) {
                    Result.Success(Unit)
                } else {
                    updateUser(userId, current.copy(verified = true))
                }
            }

            is Result.Error -> result
        }
    }

    suspend fun decrementUserCredit(userId: String): Result<Unit, Error> {
        return when (val result = getUserDto(userId)) {
            is Result.Success -> {
                val currentCredit = result.data.credit ?: 0
                val updatedCredit = (currentCredit - 1).coerceAtLeast(0)
                val updatedDto = result.data.copy(credit = updatedCredit)
                updateUser(userId, updatedDto)
            }

            is Result.Error -> result
        }
    }

    suspend fun incrementUserDonations(userId: String, meals: Int): Result<Unit, Error> {
        return when (val result = getUserDto(userId)) {
            is Result.Success -> {
                val dto = result.data
                val updated = dto.copy(
                    totalDonations = (dto.totalDonations ?: 0) + 1,
                    totalMeals = (dto.totalMeals ?: 0) + meals
                )
                updateUser(userId, updated)
            }
            is Result.Error -> result
        }
    }

    suspend fun incrementUserCredit(userId: String): Result<Unit, Error> {
        return when (val result = getUserDto(userId)) {
            is Result.Success -> {
                val userDto = result.data
                val role = UserRole.fromValue(userDto.role)
                if (role != UserRole.STUDENT) {
                    return Result.Success(Unit)
                }

                val currentCredit = userDto.credit ?: 0
                val maxCredit = userDto.weeklyCreditOverride
                    ?: configRepository.getStudentWeeklyCredit()
                val updatedCredit = (currentCredit + 1).coerceAtMost(maxCredit)
                val updatedDto = userDto.copy(credit = updatedCredit)
                updateUser(userId, updatedDto)
            }

            is Result.Error -> result
        }
    }

    suspend fun getUserRole(userId: String): Result<UserRole, Error> {
        return when (val result =
            firestoreRepository.getDocument("users", userId, UserDto::class)) {
            is Result.Success -> Result.Success(UserRole.fromValue(result.data.role))
            is Result.Error -> result
        }
    }

    suspend fun getAllUsers(): Result<List<User>, Error> {
        return when (val result =
            firestoreRepository.getCollectionWithIds("users", UserDto::class)) {
            is Result.Success -> {
                val users = result.data.map { it.data.toUser(it.id) }
                Result.Success(users)
            }

            is Result.Error -> result
        }
    }

    suspend fun getUsersByRole(role: UserRole): Result<List<User>, Error> {
        return when (val result = getAllUsers()) {
            is Result.Success -> {
                val filtered = result.data.filter { it.role == role }
                Result.Success(filtered)
            }

            is Result.Error -> result
        }
    }

}

private fun UserDto.toUser(userId: String): User {
    return User(
        id = userId,
        email = email ?: "",
        fullName = fullName ?: "",
        phoneNumber = phoneNumber,
        role = UserRole.fromValue(role),
        verified = verified ?: false,
        university = university,
        major = major,
        educationLevel = educationLevel,
        credit = credit,
        weeklyCreditOverride = weeklyCreditOverride,
        lastCreditResetAt = lastCreditResetAt?.let { Instant.fromEpochSeconds(it) },
        registrationDate = registrationDate?.let { Instant.fromEpochSeconds(it) },
        createdAt = createdAt?.let { Instant.fromEpochSeconds(it) },
        totalDonations = totalDonations ?: 0,
        totalMeals = totalMeals ?: 0
    )
}
