package com.good4.admin.presentation.editstudentcredit

data class EditStudentCreditState(
    val userIdInput: String = "",
    val emailInput: String = "",
    val weeklyCreditInput: String = "",
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
