package com.good4.schedule

import com.good4.schedule.domain.ClassSchedules
import com.good4.schedule.presentation.AcademicProfileField
import com.good4.schedule.presentation.ClassScheduleState
import com.good4.schedule.presentation.academicSelectionMessageResource
import com.good4.user.User
import com.good4.user.domain.UserRole
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.schedule_missing_all
import good4.composeapp.generated.resources.schedule_missing_class
import good4.composeapp.generated.resources.schedule_missing_department
import good4.composeapp.generated.resources.schedule_missing_department_class
import good4.composeapp.generated.resources.schedule_missing_faculty
import good4.composeapp.generated.resources.schedule_missing_faculty_class
import good4.composeapp.generated.resources.schedule_missing_faculty_department
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AcademicSelectionStateTest {
    private val completeUser = User(
        id = "student", email = "student@example.com", fullName = "Student",
        role = UserRole.STUDENT, verified = true,
        faculty = ClassSchedules.businessFirstYear.faculty,
        major = ClassSchedules.businessFirstYear.department,
        classYear = ClassSchedules.businessFirstYear.classYear
    )

    @Test
    fun everyMissingFieldCombinationPromptsWithTheMatchingExplanationAndKeepsScheduleVisible() {
        val explanations = listOf(
            listOf(AcademicProfileField.FACULTY) to Res.string.schedule_missing_faculty,
            listOf(AcademicProfileField.DEPARTMENT) to Res.string.schedule_missing_department,
            listOf(AcademicProfileField.CLASS_YEAR) to Res.string.schedule_missing_class,
            listOf(AcademicProfileField.FACULTY, AcademicProfileField.DEPARTMENT) to Res.string.schedule_missing_faculty_department,
            listOf(AcademicProfileField.FACULTY, AcademicProfileField.CLASS_YEAR) to Res.string.schedule_missing_faculty_class,
            listOf(AcademicProfileField.DEPARTMENT, AcademicProfileField.CLASS_YEAR) to Res.string.schedule_missing_department_class,
            AcademicProfileField.entries.toList() to Res.string.schedule_missing_all
        )
        explanations.forEach { (missing, message) ->
            val user = completeUser.copy(
                faculty = if (AcademicProfileField.FACULTY in missing) null else completeUser.faculty,
                major = if (AcademicProfileField.DEPARTMENT in missing) "" else completeUser.major,
                classYear = if (AcademicProfileField.CLASS_YEAR in missing) "  " else completeUser.classYear
            )
            val state = ClassScheduleState().withLoadedUser(user)
            assertEquals(missing, state.missingAcademicFields)
            assertEquals(message, academicSelectionMessageResource(state.missingAcademicFields))
            assertTrue(state.isAcademicSelectionSheetVisible)
            assertTrue(state.hasPromptedAcademicSelection)
            assertEquals(ClassSchedules.businessFirstYear, state.schedule)
        }
    }

    @Test
    fun returningWithoutSavingDoesNotReopenDismissedSheet() {
        val user = completeUser.copy(classYear = null)
        val dismissed = ClassScheduleState().withLoadedUser(user)
            .copy(isAcademicSelectionSheetVisible = false)
        val refreshed = dismissed.withLoadedUser(user)
        assertTrue(refreshed.isAcademicProfileMissing)
        assertFalse(refreshed.isAcademicSelectionSheetVisible)
        assertEquals(ClassSchedules.businessFirstYear, refreshed.schedule)
        assertTrue(ClassScheduleState().withLoadedUser(user).isAcademicSelectionSheetVisible)
    }

    @Test
    fun completingProfileLoadsPersonalScheduleAndClearsThePrompt() {
        val missing = ClassScheduleState().withLoadedUser(completeUser.copy(major = null))
        val savedUser = completeUser.copy(classYear = ClassSchedules.SECOND_YEAR)
        val completed = missing.withLoadedUser(savedUser)
        assertFalse(completed.isAcademicProfileMissing)
        assertFalse(completed.isAcademicSelectionSheetVisible)
        assertTrue(completed.isProfileSelectionComplete)
        assertEquals(ClassSchedules.find(savedUser.faculty, savedUser.major, savedUser.classYear), completed.schedule)
    }

    @Test
    fun completeProfileNeverPromptsEvenWhenItsScheduleIsUnavailable() {
        val available = ClassScheduleState().withLoadedUser(completeUser)
        assertFalse(available.isAcademicSelectionSheetVisible)
        assertFalse(available.hasPromptedAcademicSelection)
        val unavailable = ClassScheduleState().withLoadedUser(completeUser.copy(major = "Unavailable"))
        assertFalse(unavailable.isAcademicProfileMissing)
        assertFalse(unavailable.isAcademicSelectionSheetVisible)
        assertFalse(unavailable.isProfileSelectionComplete)
        assertEquals(ClassSchedules.businessFirstYear, unavailable.schedule)
    }

    @Test
    fun loadingAndFailedInitialLoadNeverPromptForMissingSelections() {
        val loading = ClassScheduleState()
        val failed = loading.copy(isLoading = false, errorMessage = "Network error")
        assertFalse(loading.isAcademicSelectionSheetVisible)
        assertFalse(failed.isAcademicProfileMissing)
        assertFalse(failed.isAcademicSelectionSheetVisible)
    }
}
