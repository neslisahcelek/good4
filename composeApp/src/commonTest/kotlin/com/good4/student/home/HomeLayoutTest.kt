package com.good4.student.home

import com.good4.user.domain.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeLayoutTest {
    private val definitions = HomeShortcut.entries.map { it.definition }
    private val defaults = HomeLayout().resolve(definitions)

    private class MemoryStorage : HomeLayoutStorage {
        val values = mutableMapOf<String, String>()
        override fun read(uid: String) = values[uid]
        override fun write(uid: String, value: String) { values[uid] = value }
    }

    @Test fun defaultOrderPreservesCurrentHomeAndHidesMenuItems() {
        assertEquals(listOf("communities", "classSchedule", "campusMap", "academicCalendar", "suspendedMeals", "campusCloset"), defaults.visible)
        assertEquals(listOf("topUp", "tennis", "phoneNumbers", "feedback"), defaults.hidden)
    }

    @Test fun storedOrderAndVisibilitySurviveAStoreRestart() {
        val storage = MemoryStorage()
        val edited = defaults.move("phoneNumbers", true, 0)!!
            .move("campusMap", false, 1)!!
        HomeLayoutStore(storage).write("student-a", edited)
        assertEquals(edited, HomeLayoutStore(storage).read("student-a", definitions))
    }

    @Test fun unknownRemovedAndDuplicateIdsAreIgnored() {
        val saved = HomeLayout(listOf("removed", "campusMap", "campusMap"), listOf("campusMap", "feedback", "unknown"))
        val loaded = saved.resolve(definitions)
        assertEquals("campusMap", loaded.visible.first())
        assertEquals("feedback", loaded.hidden.first())
        assertEquals(definitions.size, (loaded.visible + loaded.hidden).distinct().size)
        assertFalse("removed" in loaded.visible)
        assertFalse("unknown" in loaded.hidden)
    }

    @Test fun newShortcutsAppendToTheirDefaultSectionWithoutMovingExistingOnes() {
        val extended = definitions + HomeShortcutDefinition("newShown", true) + HomeShortcutDefinition("newHidden", false)
        val loaded = defaults.resolve(extended)
        assertEquals(defaults.visible + "newShown", loaded.visible)
        assertEquals(defaults.hidden + "newHidden", loaded.hidden)
    }

    @Test fun lastVisibleCannotBeHiddenButCanStillBeReorderedOrReplaced() {
        val layout = HomeLayout(listOf("communities"), listOf("feedback"))
        assertNull(layout.move("communities", false, 0))
        assertEquals(layout, layout.move("communities", true, 0))
        val withSecond = layout.move("feedback", true, 1)!!
        assertEquals(listOf("feedback"), withSecond.move("communities", false, 0)!!.visible)
    }

    @Test fun featureDisabledShortcutIsExcludedEvenWhenSavedVisible() {
        val allowed = availableHomeShortcuts(UserRole.STUDENT, suspendedMealsEnabled = false)
        assertFalse(HomeShortcut.SUSPENDED_MEALS in allowed)
        val loaded = defaults.resolve(allowed.map { it.definition })
        assertFalse("suspendedMeals" in loaded.visible + loaded.hidden)
    }

    @Test fun campusClosetIsOfferedOnlyWhenEnabled() {
        assertFalse(HomeShortcut.CAMPUS_CLOSET in availableHomeShortcuts(UserRole.STUDENT, suspendedMealsEnabled = false))
        assertTrue(HomeShortcut.CAMPUS_CLOSET in availableHomeShortcuts(UserRole.STUDENT, false, campusClosetEnabled = true))
    }

    @Test fun studentShortcutsAreNotAvailableForStaffOrLegacySupporterRoles() {
        listOf(UserRole.ADMIN, UserRole.BUSINESS, UserRole.SUPPORTER).forEach {
            assertTrue(availableHomeShortcuts(it, true).isEmpty())
        }
    }

    @Test fun layoutsAreIsolatedByUidAndSurviveSwitchingBack() {
        val store = HomeLayoutStore(MemoryStorage())
        val a = defaults.move("topUp", true, 0)!!
        val b = defaults.move("feedback", true, 1)!!
        store.write("student-a", a)
        assertEquals(defaults, store.read("student-b", definitions))
        store.write("student-b", b)
        assertEquals(a, store.read("student-a", definitions))
        assertEquals(b, store.read("student-b", definitions))
        assertEquals(HomeLayout(), store.read("", definitions))
    }

    @Test fun corruptOrAllHiddenStorageRecoversToANonEmptyHome() {
        val storage = MemoryStorage()
        storage.values["student-a"] = "invalid json"
        assertEquals(defaults, HomeLayoutStore(storage).read("student-a", definitions))
        val allHidden = HomeLayout(hidden = definitions.map { it.id }).resolve(definitions)
        assertEquals(listOf("communities"), allHidden.visible)
        assertFalse("communities" in allHidden.hidden)
    }

    @Test fun movesWithinAndAcrossSectionsUseDestinationIndices() {
        val moved = defaults.move("communities", true, 3)!!
        assertEquals(listOf("classSchedule", "campusMap", "academicCalendar", "communities", "suspendedMeals", "campusCloset"), moved.visible)
        val hidden = moved.move("communities", false, 2)!!
        assertEquals(listOf("topUp", "tennis", "communities", "phoneNumbers", "feedback"), hidden.hidden)
        assertEquals("communities", hidden.move("communities", true, 0)!!.visible.first())
        assertNull(hidden.move("unknown", true, 0))
    }
}
