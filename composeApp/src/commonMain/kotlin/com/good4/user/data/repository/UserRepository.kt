package com.good4.user.data.repository

import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.core.domain.UnknownError
import com.good4.user.User
import com.good4.user.data.dto.UserDto
import com.good4.user.domain.UserRole
import kotlinx.datetime.Clock

class UserRepository(
    private val firestoreRepository: FirestoreRepository
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

    suspend fun resetStudentCreditsWeekly(): Result<Unit, Error> {
        return when (val studentsResult = getUsersByRole(UserRole.STUDENT)) {
            is Result.Success -> {
                val students = studentsResult.data
                val now = Clock.System.now()
                var hasError = false
                var lastError: Error? = null

                students.forEach { student ->
                    val registrationDate = student.registrationDate
                    if (registrationDate != null) {
                        val timeSinceRegistration = now - registrationDate
                        val daysSinceRegistration = timeSinceRegistration.inWholeDays
                        val weeksSinceRegistration = daysSinceRegistration / 7

                        if (weeksSinceRegistration >= 1 && student.credit != 1) {
                            val updatedDto = UserDto(
                                email = student.email,
                                fullName = student.fullName,
                                phoneNumber = student.phoneNumber,
                                role = UserRole.STUDENT.value,
                                verified = student.verified,
                                university = student.university,
                                major = student.major,
                                educationLevel = student.educationLevel,
                                credit = 1,
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
                }

                if (hasError) {
                    Result.Error(lastError ?: UnknownError("Credit reset failed"))
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
        registrationDate = registrationDate
    )
}

