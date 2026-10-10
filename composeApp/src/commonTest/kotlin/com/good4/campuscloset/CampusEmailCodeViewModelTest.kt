package com.good4.campuscloset

import androidx.lifecycle.ViewModelStore
import com.good4.core.presentation.UiText
import com.good4.eduverification.EduCodeConfirmResult
import com.good4.eduverification.EduCodeRequestResult
import good4.composeapp.generated.resources.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CampusEmailCodeViewModelTest {
    private class Source : CampusEmailCodeSource {
        var requests = 0
        val codes = mutableListOf<String>()
        var request: suspend () -> EduCodeRequestResult = { EduCodeRequestResult.Sent("123@ogr.akdeniz.edu.tr", 60) }
        var confirm: suspend () -> EduCodeConfirmResult = { EduCodeConfirmResult.Verified("123@ogr.akdeniz.edu.tr") }
        override suspend fun requestCode(email: String): EduCodeRequestResult { requests++; return request() }
        override suspend fun confirmCode(code: String): EduCodeConfirmResult { codes += code; return confirm() }
        override suspend fun verifiedEmail(): String? = null
    }
    private fun errorId(vm: CampusEmailVerificationViewModel) = (vm.state.value.error as UiText.StringResourceId).id

    @Test fun sixDigitsConfirmOnceAndSuccessfulDeliveryStartsSixtySecondCountdown() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler)); val store = ViewModelStore()
        try {
            val source = Source(); val waiting = CompletableDeferred<EduCodeConfirmResult>()
            source.confirm = { waiting.await() }
            val vm = CampusEmailVerificationViewModel(source) { testScheduler.currentTime }.also { store.put("code", it) }
            vm.setEmail("123@ogr.akdeniz.edu.tr"); vm.sendCode(); vm.sendCode(); runCurrent()
            assertEquals(1, source.requests); assertEquals(60, vm.state.value.resendSeconds)
            vm.sendCode(); runCurrent(); assertEquals(1, source.requests)
            vm.setCode("12a345"); runCurrent(); assertTrue(source.codes.isEmpty())
            vm.setCode("123456"); vm.setCode("123456"); runCurrent()
            assertEquals(listOf("123456"), source.codes); assertTrue(vm.state.value.isConfirming)
            waiting.complete(EduCodeConfirmResult.Verified("123@ogr.akdeniz.edu.tr")); runCurrent()
            assertEquals("123@ogr.akdeniz.edu.tr", vm.state.value.verifiedEmail)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun failedSendDoesNotSpendCooldownAndAddressChangesDoNotBypassIt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler)); val store = ViewModelStore()
        try {
            val source = Source(); source.request = { error("EDU_EMAIL_SEND_FAILED") }
            val vm = CampusEmailVerificationViewModel(source) { testScheduler.currentTime }.also { store.put("code", it) }
            vm.setEmail("123@ogr.akdeniz.edu.tr"); vm.sendCode(); runCurrent()
            assertEquals(0, vm.state.value.resendSeconds); assertNull(vm.state.value.sentTo)
            assertEquals(Res.string.campus_email_code_send_failed, errorId(vm))
            source.request = { EduCodeRequestResult.Sent("123@ogr.akdeniz.edu.tr", 60) }
            vm.sendCode(); runCurrent(); vm.changeEmail(); vm.setEmail("456@ogr.akdeniz.edu.tr"); vm.sendCode(); runCurrent()
            assertEquals(2, source.requests); assertNull(vm.state.value.sentTo)
            advanceTimeBy(60_000); runCurrent(); assertEquals(0, vm.state.value.resendSeconds)
            vm.sendCode(); runCurrent(); assertEquals(3, source.requests)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun wrongExpiredAndLockedCodesHaveSeparateMessagesAndOnlyNewDeliveryUnlocks() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler)); val store = ViewModelStore()
        try {
            val source = Source(); val vm = CampusEmailVerificationViewModel(source) { testScheduler.currentTime }.also { store.put("code", it) }
            vm.setEmail("123@ogr.akdeniz.edu.tr"); vm.sendCode(); runCurrent()
            source.confirm = { EduCodeConfirmResult.InvalidCode(4) }; vm.setCode("123456"); runCurrent()
            assertEquals(Res.string.campus_email_code_wrong, errorId(vm))
            assertEquals(4, (vm.state.value.error as UiText.StringResourceId).args[0]); assertEquals("", vm.state.value.code)
            source.confirm = { EduCodeConfirmResult.Expired }; vm.setCode("123456"); runCurrent()
            assertEquals(Res.string.campus_email_code_expired, errorId(vm)); assertTrue(vm.state.value.codeBlocked)
            vm.setCode("111111"); runCurrent(); assertEquals(2, source.codes.size)
            advanceTimeBy(60_000); runCurrent(); vm.sendCode(); runCurrent(); assertFalse(vm.state.value.codeBlocked)
            source.confirm = { EduCodeConfirmResult.TooManyAttempts }; vm.setCode("123456"); runCurrent()
            assertEquals(Res.string.campus_email_code_limit, errorId(vm)); assertTrue(vm.state.value.codeBlocked)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun otherDomainsAreRejectedBeforeSendingAndExistingVerificationStillWorks() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler)); val store = ViewModelStore()
        try {
            val source = Source(); val vm = CampusEmailVerificationViewModel(source) { testScheduler.currentTime }.also { store.put("code", it) }
            vm.setEmail("123@other.edu.tr"); vm.sendCode(); runCurrent(); assertEquals(0, source.requests)
            assertNotNull(vm.state.value.error)
            source.request = { EduCodeRequestResult.AlreadyVerified("123@ogr.akdeniz.edu.tr") }
            vm.setEmail("123@ogr.akdeniz.edu.tr"); vm.sendCode(); runCurrent()
            assertEquals("123@ogr.akdeniz.edu.tr", vm.state.value.verifiedEmail)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun zeroAttemptsLocksImmediatelyAndTransportFailuresAllowExplicitRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler)); val store = ViewModelStore()
        try {
            val source = Source(); val vm = CampusEmailVerificationViewModel(source) { testScheduler.currentTime }.also { store.put("code", it) }
            vm.setEmail("123@ogr.akdeniz.edu.tr"); vm.sendCode(); runCurrent()
            source.confirm = { error("network") }; vm.setCode("123456"); runCurrent()
            assertFalse(vm.state.value.isConfirming); assertEquals("123456", vm.state.value.code)
            source.confirm = { EduCodeConfirmResult.InvalidCode(0) }; vm.confirmCode(); runCurrent()
            assertEquals(Res.string.campus_email_code_limit, errorId(vm)); assertTrue(vm.state.value.codeBlocked)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun resumeRecalculatesCooldownAfterTheAppWasInTheBackground() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler)); val store = ViewModelStore()
        try {
            var clock = 0L
            val vm = CampusEmailVerificationViewModel(Source()) { clock }.also { store.put("code", it) }
            vm.setEmail("123@ogr.akdeniz.edu.tr"); vm.sendCode(); runCurrent()
            assertEquals(60, vm.state.value.resendSeconds)
            vm.onPause(); clock = 61_000; vm.onResume(); runCurrent()
            assertEquals(0, vm.state.value.resendSeconds)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test
    fun abuseLimitsHaveTheirOwnMessages() {
        assertEquals(Res.string.campus_email_code_recipient_limit, (campusCodeError(Exception("EDU_CODE_RECIPIENT_LIMIT")) as UiText.StringResourceId).id)
        assertEquals(Res.string.campus_email_code_account_daily_limit, (campusCodeError(Exception("EDU_CODE_ACCOUNT_DAILY_LIMIT")) as UiText.StringResourceId).id)
        assertEquals(Res.string.campus_email_code_daily_limit, (campusCodeError(Exception("EDU_CODE_DAILY_LIMIT")) as UiText.StringResourceId).id)
    }
}
