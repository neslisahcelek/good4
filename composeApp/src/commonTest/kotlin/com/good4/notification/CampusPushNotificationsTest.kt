package com.good4.notification

import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.domain.AuthError
import com.good4.auth.domain.AuthUser
import com.good4.core.domain.Result
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class CampusPushNotificationsTest {
    @Test fun onlyKnownCampusRoutesWithSafeIdsAreAccepted() {
        assertNotNull(campusPushDestination("uid", "market_message", "listing_buyer"))
        assertNotNull(campusPushDestination("uid", "market_listing", "listing"))
        assertNotNull(campusPushDestination("uid", "social_request", "activity"))
        assertNotNull(campusPushDestination("uid", "social_message", "activity_participant"))
        assertNull(campusPushDestination("uid", "social_message", "https://evil.example"))
        assertNull(campusPushDestination("uid", null, "activity"))
        assertNull(campusPushDestination(null, "market_message", "listing_buyer"))
        assertNull(campusPushDestination("uid", "admin", "listing"))
        assertNull(campusPushDestination("uid", "market_message", "https://evil.example"))
        assertNull(campusPushDestination("uid", "market_message", "users/other"))
        assertNull(campusPushDestination("uid", "market_message", "\nlisting"))
    }

    @Test fun permissionDenialNeverKeepsARegistrationAddress() {
        reset()
        CampusPushNotifications.updateDevice("private-token", "denied")
        assertNull(CampusPushNotifications.device.value.token)
    }

    @Test fun consumingAnOldTapCannotRemoveANewerNotification() {
        reset()
        CampusPushNotifications.receive("a", "market_message", "first")
        val first = CampusPushNotifications.pending.value!!
        CampusPushNotifications.receive("a", "market_message", "second")
        CampusPushNotifications.consume(first)
        assertEquals("second", CampusPushNotifications.pending.value?.targetId)
    }

    @Test fun signedOutPhonesAreNotRegisteredAndPermissionRevocationUnregisters() = runBlocking {
        reset()
        val auth = FakeAuth()
        val calls = mutableListOf<Pair<String, JsonObject>>()
        val manager = PushRegistrationManager(auth, { name, data -> calls.add(name to data); JsonObject(emptyMap()) })
        val observer = launch { manager.observe() }
        try {
            CampusPushNotifications.updateDevice("token-a", "granted")
            delay(30)
            assertEquals(0, calls.size)
            auth.users.value = user("a")
            until { calls.size == 1 }
            assertEquals("registerPushDevice", calls[0].first)
            CampusPushNotifications.updateDevice(null, "granted")
            delay(30)
            assertEquals(1, calls.size, "a transient SDK failure must not unregister a permitted phone")
            CampusPushNotifications.updateDevice(null, "denied")
            until { calls.size == 2 }
            assertEquals("unregisterPushDevice", calls[1].first)
            assertEquals("token-a", calls[1].second["token"]?.jsonPrimitive?.content)
        } finally { observer.cancelAndJoin(); reset() }
    }

    @Test fun rotationRemovesOldTokenAndRepeatedForegroundRefreshIsThrottled() = runBlocking {
        reset()
        val auth = FakeAuth().also { it.users.value = user("a") }
        val calls = mutableListOf<Pair<String, JsonObject>>()
        var clock = 1_000_000L
        val manager = PushRegistrationManager(auth, { name, data -> calls.add(name to data); JsonObject(emptyMap()) }, { clock })
        val observer = launch { manager.observe() }
        try {
            CampusPushNotifications.updateDevice("token-a", "granted")
            until { calls.size == 1 }
            CampusPushNotifications.updateDevice("token-a", "granted")
            delay(30)
            assertEquals(1, calls.size)
            CampusPushNotifications.updateDevice("token-b", "granted")
            until { calls.size == 3 }
            assertEquals("unregisterPushDevice", calls[1].first)
            assertEquals("token-a", calls[1].second["token"]?.jsonPrimitive?.content)
            assertEquals("token-b", calls[2].second["token"]?.jsonPrimitive?.content)
            clock += 7 * 60 * 60 * 1000
            CampusPushNotifications.updateDevice("token-b", "granted")
            until { calls.size == 4 }
        } finally { observer.cancelAndJoin(); reset() }
    }

    @Test fun changingPrimaryAccountRegistersTheDeviceForTheNewAccount() = runBlocking {
        reset()
        val auth = FakeAuth().also { it.users.value = user("a") }
        val registeredAccounts = mutableListOf<String?>()
        val manager = PushRegistrationManager(auth, { _, _ -> registeredAccounts.add(auth.currentUser?.uid); JsonObject(emptyMap()) })
        val observer = launch { manager.observe() }
        try {
            CampusPushNotifications.updateDevice("token-a", "granted")
            until { registeredAccounts.size == 1 }
            auth.users.value = user("b")
            until { registeredAccounts.size == 2 }
            assertEquals(listOf<String?>("a", "b"), registeredAccounts)
        } finally { observer.cancelAndJoin(); reset() }
    }

    private fun reset() {
        CampusPushNotifications.allowSession(null)
        CampusPushNotifications.updateDevice(null, "unknown")
        CampusPushNotifications.pending.value?.let(CampusPushNotifications::consume)
    }
    private fun user(uid: String) = AuthUser(uid, null, null, true)
    private suspend fun until(condition: () -> Boolean) { withTimeout(3_000) { while (!condition()) delay(10) } }

    private class FakeAuth : AuthRepository {
        val users = MutableStateFlow<AuthUser?>(null)
        override val currentUser get() = users.value
        override val authStateFlow: Flow<AuthUser?> get() = users
        override suspend fun signIn(email: String, password: String): Result<AuthUser, AuthError> = error("unused")
        override suspend fun signInWithGoogleToken(idToken: String, accessToken: String?): Result<AuthUser, AuthError> = error("unused")
        override suspend fun signInWithAppleToken(idToken: String, rawNonce: String): Result<AuthUser, AuthError> = error("unused")
        override suspend fun signUp(email: String, password: String): Result<AuthUser, AuthError> = error("unused")
        override suspend fun signOut(): Result<Unit, AuthError> = error("unused")
        override suspend fun deleteCurrentUser(): Result<Unit, AuthError> = error("unused")
        override suspend fun sendEmailVerification(): Result<Unit, AuthError> = error("unused")
        override suspend fun reloadCurrentUser(): Result<AuthUser, AuthError> = error("unused")
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit, AuthError> = error("unused")
        override fun isLoggedIn() = currentUser != null
    }
}
