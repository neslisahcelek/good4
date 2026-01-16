package com.good4.student.presentation.profile

import com.good4.user.User

data class StudentProfileState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val errorMessage: String? = null
)

