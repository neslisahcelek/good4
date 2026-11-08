package com.good4.community

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Production and staging test community visibility safeguards (AGENTS.md Rule 14):
 * Ensures that unlisted test communities (`listed: false`) are NEVER exposed to normal students,
 * protecting against regressions in future developments.
 */
class CommunityVisibilityTest {

    private fun community(id: String, name: String, listed: Boolean = true) = Community(
        id = id,
        data = CommunityDto(name = name, listed = listed)
    )

    private val publicClub = community("public-club", "Yazılım Topluluğu", listed = true)
    private val testClub = community("test-club", "Dahili Test Topluluğu", listed = false)
    private val hiddenClub = community("hidden-club", "Özel Topluluk", listed = false)

    @Test
    fun unlistedCommunitiesAreNeverVisibleToRegularStudents() {
        val all = listOf(publicClub, testClub, hiddenClub)
        val studentAccess = CommunityAccessDto(communityIds = emptyList(), active = false)

        val visible = filterVisibleCommunities(all, studentAccess)

        assertEquals(listOf("public-club"), visible.map { it.id })
        assertFalse(visible.any { it.id == "test-club" }, "Test community must never leak to regular students")
        assertFalse(visible.any { it.id == "hidden-club" })
    }

    @Test
    fun unlistedCommunityIsVisibleOnlyToItsAssignedManager() {
        val all = listOf(publicClub, testClub, hiddenClub)
        val managerAccess = CommunityAccessDto(communityIds = listOf("test-club"), active = true)

        val visible = filterVisibleCommunities(all, managerAccess)

        assertEquals(listOf("public-club", "test-club"), visible.map { it.id })
        assertTrue(visible.any { it.id == "test-club" }, "Manager must be able to see and test their own unlisted community")
        assertFalse(visible.any { it.id == "hidden-club" }, "Manager must NOT see unlisted communities they do not manage")
    }

    @Test
    fun unlistedCommunityIsNotVisibleToManagersOfOtherCommunities() {
        val all = listOf(publicClub, testClub, hiddenClub)
        val otherManagerAccess = CommunityAccessDto(communityIds = listOf("other-managed-club"), active = true)

        val visible = filterVisibleCommunities(all, otherManagerAccess)

        assertEquals(listOf("public-club"), visible.map { it.id })
        assertFalse(visible.any { it.id == "test-club" })
    }

    @Test
    fun blockedCommunitiesAreExcludedUnlessUserIsTheManager() {
        val all = listOf(publicClub, testClub)
        val blockedIds = setOf("public-club", "test-club")

        // Student with blocked communities
        val studentAccess = CommunityAccessDto(communityIds = emptyList())
        val studentVisible = filterVisibleCommunities(all, studentAccess, blockedIds)
        assertTrue(studentVisible.isEmpty())

        // Manager of test-club keeps test-club even if it was marked in blockedIds
        val managerAccess = CommunityAccessDto(communityIds = listOf("test-club"))
        val managerVisible = filterVisibleCommunities(all, managerAccess, blockedIds)
        assertEquals(listOf("test-club"), managerVisible.map { it.id })
    }
}
