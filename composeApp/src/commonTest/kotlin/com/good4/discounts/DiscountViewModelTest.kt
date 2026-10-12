package com.good4.discounts

import androidx.lifecycle.viewModelScope
import good4.composeapp.generated.resources.*
import com.good4.core.presentation.UiText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import kotlinx.datetime.Instant
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DiscountViewModelTest {
    private val start = Instant.fromEpochSeconds(100)
    private fun discount(id: String = "offer") = Discount(id, BusinessDiscountDto(status = "published", startsAt = 100, endsAt = 200, version = 1))
    private class FakeSource : DiscountDataSource {
        override val accounts = MutableSharedFlow<String?>(replay = 1)
        var loadAction: suspend (String?) -> DiscountPage = { DiscountPage(emptyList(), null) }
        var issueAction: suspend (Discount) -> DiscountCode = { DiscountCode("issued", "0012", Instant.fromEpochSeconds(700)) }
        override suspend fun load(cursor: String?) = loadAction(cursor)
        override suspend fun issue(discount: Discount) = issueAction(discount)
    }
    @Test
    fun validityIncludesStartExcludesEndAndFiltersEndedOrExhaustedOffers() {
        val data = discount().data
        assertFalse(data.isAvailable(99)); assertTrue(data.isAvailable(100)); assertTrue(data.isAvailable(199)); assertFalse(data.isAvailable(200))
        assertFalse(data.copy(status = "ended").isAvailable(100))
        assertFalse(data.copy(totalLimit = 1, redemptionCount = 1).isAvailable(100))
        assertTrue(data.copy(totalLimit = null, redemptionCount = 500).isAvailable(100))
    }
    @Test
    fun pagingFiltersInvalidOffersAndPreservesCursorAcrossEmptyPages() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = FakeSource()
        source.loadAction = { cursor -> if (cursor == null) DiscountPage(listOf(discount().copy(data = discount().data.copy(startsAt = 150))), "next") else DiscountPage(listOf(discount()), null) }
        val vm = DiscountViewModel(source) { start }
        try {
            source.accounts.emit("student"); runCurrent()
            assertTrue(vm.state.value.discounts.isEmpty()); assertEquals("next", vm.state.value.nextCursor)
            vm.loadMore(); runCurrent()
            assertEquals(listOf("offer"), vm.state.value.discounts.map { it.id }); assertNull(vm.state.value.nextCursor)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
    @Test
    fun expiredOffersDisappearAndCodeCountdownUsesDeadlineAfterTimeAway() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var clock = start
        val source = FakeSource().apply { loadAction = { DiscountPage(listOf(discount()), null) } }
        val vm = DiscountViewModel(source) { clock }
        try {
            source.accounts.emit("student"); runCurrent()
            vm.select(discount()); vm.issue(); runCurrent()
            assertEquals("0012", vm.state.value.code); assertEquals(600, vm.state.value.seconds)
            clock = Instant.fromEpochSeconds(700); advanceTimeBy(1000); runCurrent()
            assertEquals(0, vm.state.value.seconds); assertTrue(vm.state.value.discounts.isEmpty())
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
    @Test
    fun refreshDuringCodeRequestDoesNotLoseCodeOrLeaveBusyState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val pending = CompletableDeferred<DiscountCode>()
        val source = FakeSource().apply { issueAction = { pending.await() } }
        val vm = DiscountViewModel(source) { start }
        try {
            source.accounts.emit("student"); runCurrent(); vm.select(discount()); vm.issue(); runCurrent()
            vm.refresh(); runCurrent()
            pending.complete(DiscountCode("issued", "0012", Instant.fromEpochSeconds(700))); runCurrent()
            assertEquals("0012", vm.state.value.code); assertFalse(vm.state.value.issuing)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
    @Test
    fun oldSelectionCodeAndErrorCannotOverwriteNewSelection() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val pending = CompletableDeferred<DiscountCode>()
        val source = FakeSource().apply { issueAction = { withContext(NonCancellable) { pending.await() } } }
        val vm = DiscountViewModel(source) { start }
        try {
            source.accounts.emit("student"); runCurrent(); vm.select(discount("a")); vm.issue(); runCurrent()
            vm.select(discount("b")); pending.completeExceptionally(IllegalStateException("old error")); runCurrent()
            assertEquals("b", vm.state.value.selected?.id); assertNull(vm.state.value.codeMessage); assertFalse(vm.state.value.issuing)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
    @Test
    fun sameUidNewSessionRejectsOldLoadingErrorCursorAndCode() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val pending = CompletableDeferred<DiscountPage>()
        var calls = 0
        val source = FakeSource().apply { loadAction = { if (++calls == 1) withContext(NonCancellable) { pending.await() } else DiscountPage(listOf(discount("new")), null) } }
        val vm = DiscountViewModel(source) { start }
        try {
            source.accounts.emit("student"); runCurrent()
            source.accounts.emit(null); runCurrent(); source.accounts.emit("student"); runCurrent()
            pending.completeExceptionally(IllegalStateException("old failure")); runCurrent()
            assertEquals(listOf("new"), vm.state.value.discounts.map { it.id }); assertNull(vm.state.value.error); assertFalse(vm.state.value.loading); assertNull(vm.state.value.nextCursor)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
    @Test
    fun dailyLimitOutcomeHasLocalizedMessageAndReleasesBusyState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = FakeSource().apply { issueAction = { DiscountCode("already_used_today") } }
        val vm = DiscountViewModel(source) { start }
        try {
            source.accounts.emit("student"); runCurrent(); vm.select(discount()); vm.issue(); runCurrent()
            assertEquals(Res.string.community_coupon_used_today, (vm.state.value.codeMessage as UiText.StringResourceId).id)
            assertFalse(vm.state.value.issuing); assertNull(vm.state.value.code)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
}
