package com.good4.user.data.repository

import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.core.domain.UnknownError
import com.good4.user.User
import com.good4.user.data.dto.UserDto
import com.good4.user.domain.UserRole
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_credit_reset_failed
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.getString

class UserRepository(
    private val firestoreRepository: FirestoreRepository,
    private val configRepository: AppConfigRepository
) {
    suspend fun createUser(userId: String, userDto: UserDto): Result<Unit, Error> {
        return firestoreRepository.updateDocument("users", userId, userDto)
    }

    suspend fun getUser(userId: String): Result<User, Error> {
        return when (val result = firestoreRepository.getDocument("users", userId, UserDto::class)) {
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
        return when (val result = firestoreRepository.getDocument("users", userId, UserDto::class)) {
            is Result.Success -> Result.Success(UserRole.fromValue(result.data.role))
            is Result.Error -> result
        }
    }

    suspend fun getAllUsers(): Result<List<User>, Error> {
        return when (val result = firestoreRepository.getCollectionWithIds("users", UserDto::class)) {
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

    suspend fun refreshStudentCreditIfNeeded(userId: String): Result<User, Error> {
        return when (val result = getUserDto(userId)) {
            is Result.Success -> {
                val userDto = result.data
                val role = UserRole.fromValue(userDto.role)
                if (role != UserRole.STUDENT) {
                    return Result.Success(userDto.toUser(userId))
                }

                val now = Clock.System.now()
                val intervalDays = configRepository.getCreditResetIntervalDays()
                val weeklyCredit = userDto.weeklyCreditOverride
                    ?: configRepository.getStudentWeeklyCredit()
                val lastResetAt = userDto.lastCreditResetAt
                val registrationDate = userDto.registrationDate

                if (lastResetAt == null) {
                    val daysSinceRegistration = registrationDate?.let { (now - it).inWholeDays }
                    val shouldReset = daysSinceRegistration != null && daysSinceRegistration >= intervalDays
                    val needsCreditInit = userDto.credit == null || registrationDate == null

                    val updatedDto = when {
                        shouldReset || needsCreditInit -> userDto.copy(
                            credit = weeklyCredit,
                            lastCreditResetAt = now
                        )
                        registrationDate != null -> userDto.copy(
                            lastCreditResetAt = registrationDate
                        )
                        else -> userDto.copy(lastCreditResetAt = now)
                    }

                    return when (val updateResult = updateUser(userId, updatedDto)) {
                        is Result.Success -> Result.Success(updatedDto.toUser(userId))
                        is Result.Error -> updateResult
                    }
                }

                val daysSinceReset = (now - lastResetAt).inWholeDays
                if (daysSinceReset >= intervalDays) {
                    val updatedDto = userDto.copy(
                        credit = weeklyCredit,
                        lastCreditResetAt = now
                    )
                    when (val updateResult = updateUser(userId, updatedDto)) {
                        is Result.Success -> Result.Success(updatedDto.toUser(userId))
                        is Result.Error -> updateResult
                    }
                } else {
                    Result.Success(userDto.toUser(userId))
                }
            }
            is Result.Error -> result
        }
    }

    suspend fun resetStudentCreditsWeekly(): Result<Unit, Error> {
        return when (val studentsResult = getUsersByRole(UserRole.STUDENT)) {
            is Result.Success -> {
                val students = studentsResult.data
                val now = Clock.System.now()
                val intervalDays = configRepository.getCreditResetIntervalDays()
                val weeklyCredit = configRepository.getStudentWeeklyCredit()
                var hasError = false
                var lastError: Error? = null

                students.forEach { student ->
                    val lastResetAt = student.lastCreditResetAt ?: student.registrationDate
                    val daysSinceReset = lastResetAt?.let { (now - it).inWholeDays } ?: 0
                    val targetCredit = student.weeklyCreditOverride ?: weeklyCredit

                    if (daysSinceReset >= intervalDays) {
                        val updatedDto = UserDto(
                            email = student.email,
                            fullName = student.fullName,
                            phoneNumber = student.phoneNumber,
                            role = UserRole.STUDENT.value,
                            verified = student.verified,
                            university = student.university,
                            major = student.major,
                            educationLevel = student.educationLevel,
                            credit = targetCredit,
                            weeklyCreditOverride = student.weeklyCreditOverride,
                            lastCreditResetAt = now,
                            registrationDate = student.registrationDate
                        )

                        when (val updateResult = updateUser(student.id, updatedDto)) {
                            is Result.Error -> {
                                hasError = true
                                lastError = updateResult.error
                            }
                            is Result.Success -> Unit
                        }
                    }
                }

                if (hasError) {
                    Result.Error(
                        lastError ?: UnknownError(getString(Res.string.error_credit_reset_failed))
                    )
                } else {
                    Result.Success(Unit)
                }
            }
            is Result.Error -> studentsResult
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
        lastCreditResetAt = lastCreditResetAt,
        registrationDate = registrationDate
    )
}
