package com.good4.eduverification

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EduVerificationViewModelTest {
    private class Source : EduVerificationSource {
        override val currentUid = "edu-vm-test"
        var result = EduStatus(false, null, testAvailable = true, testEmail = "admin@example.com")
        var request: suspend () -> EduCodeRequestResult = { EduCodeRequestResult.Sent("admin@example.com", 180, "session") }
        var confirmation: suspend () -> EduCodeConfirmResult = { EduCodeConfirmResult.Verified("admin@example.com") }
        override suspend fun status() = result
        override suspend fun requestCode(email: String) = request()
        override suspend fun confirmCode(code: String, sessionId: String?) = confirmation()
        override suspend fun disconnectTest() { CampusAccessSession.changed(currentUid) }
    }

    @Test fun statusRefreshDoesNotDiscardDeliveryOrLeaveTheFormBusy() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val delivery = CompletableDeferred<EduCodeRequestResult>()
            val source = Source().apply { request = { delivery.await() } }
            val vm = EduVerificationViewModel(source).also { store.put("edu", it) }
            runCurrent(); vm.sendCode(); runCurrent()
            vm.refresh(); runCurrent()
            delivery.complete(EduCodeRequestResult.Sent("admin@example.com", 180, "session")); runCurrent()
            assertFalse(vm.state.value.isSending)
            assertEquals("session", vm.state.value.sessionId)
            assertEquals("admin@example.com", vm.state.value.codeSentTo)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun statusRefreshDoesNotDiscardConfirmation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val confirmation = CompletableDeferred<EduCodeConfirmResult>()
            val source = Source().apply { this.confirmation = { confirmation.await() } }
            val vm = EduVerificationViewModel(source).also { store.put("edu", it) }
            runCurrent(); vm.sendCode(); runCurrent()
            vm.onCodeChange("123456"); vm.confirmCode(); runCurrent()
            vm.refresh(); runCurrent()
            confirmation.complete(EduCodeConfirmResult.Verified("admin@example.com")); runCurrent()
            assertFalse(vm.state.value.isConfirming)
            assertTrue(vm.state.value.campusAccessAllowed)
            assertFalse(vm.state.value.isVerified)
            assertEquals("admin_test", vm.state.value.accessSource)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun accessInvalidationDiscardsAnOutstandingDelivery() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val delivery = CompletableDeferred<EduCodeRequestResult>()
            val source = Source().apply { request = { delivery.await() } }
            val vm = EduVerificationViewModel(source).also { store.put("edu", it) }
            runCurrent(); vm.sendCode(); runCurrent()
            source.result = source.result.copy(resendAfterSeconds = 180)
            CampusAccessSession.changed(source.currentUid); runCurrent()
            delivery.complete(EduCodeRequestResult.Sent("admin@example.com", 180, "old-session")); runCurrent()
            assertFalse(vm.state.value.isSending)
            assertNull(vm.state.value.sessionId)
            assertNull(vm.state.value.codeSentTo)
            assertEquals(180, vm.state.value.resendSeconds)
        } finally { store.clear(); Dispatchers.resetMain() }
    }
}
