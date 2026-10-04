package com.good4.notification

import androidx.lifecycle.ViewModelStore
import com.good4.auth.domain.AuthUser
import com.good4.core.data.repository.NumericPageCursor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    private class Source : NotificationDataSource {
        override val authStateFlow = MutableStateFlow<AuthUser?>(AuthUser("student", null, null, true))
        override val currentUserId get() = authStateFlow.value?.uid
        override val supported = true
        val cursors = mutableListOf<NumericPageCursor?>()
        val singleIds = mutableListOf<String>()
        val readIds = mutableListOf<Set<String>>()
        var page: suspend (NumericPageCursor?) -> NotificationPage = { NotificationPage(emptyList(), null, 0) }
        var single: suspend (String) -> StudentNotification? = { notification(it, 500) }
        override suspend fun inbox(cursor: NumericPageCursor?): NotificationPage { cursors += cursor; return page(cursor) }
        override suspend fun notification(id: String): StudentNotification? { singleIds += id; return single(id) }
        override suspend fun preferences() = NotificationPreferencesDto()
        override suspend fun savePreferences(value: NotificationPreferencesDto) = Unit
        override suspend fun markRead(ids: Set<String>) { readIds += ids }
        override suspend fun syncDevice() = PushSnapshot()
    }

    @Test fun aLatePageCannotReplaceARefreshedInboxOrItsCursor() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val source = Source()
            val old = CompletableDeferred<NotificationPage>()
            val firstCursor = NumericPageCursor(10, "old")
            var refreshed = false
            source.page = { cursor ->
                if (cursor != null) withContext(NonCancellable) { old.await() }
                else if (refreshed) NotificationPage(listOf(notification("new", 30)), NumericPageCursor(30, "new"), 20)
                else NotificationPage(listOf(notification("old", 10)), firstCursor, 20)
            }
            val vm = NotificationsViewModel(source).also { store.put("inbox", it) }
            runCurrent(); vm.refresh(); runCurrent(); vm.loadMore(); runCurrent()
            refreshed = true
            vm.refresh(); runCurrent()
            old.complete(NotificationPage(listOf(notification("stale", 1)), NumericPageCursor(1, "stale"), 20))
            runCurrent()
            assertEquals(listOf("new"), vm.state.value.notifications.map { it.id })
            assertEquals(NumericPageCursor(30, "new"), vm.state.value.nextCursor)
            assertFalse(vm.state.value.loadingMore)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun pageFailureCanRetryWithoutDuplicatesAndReadOnlyMarksLoadedUnreadItems() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val source = Source()
            var fail = true
            source.page = { cursor ->
                if (cursor == null) NotificationPage(listOf(notification("first", 20)), NumericPageCursor(20, "first"), 20)
                else if (fail) error("offline")
                else NotificationPage(listOf(notification("first", 20), notification("second", 10)), null, 2)
            }
            val vm = NotificationsViewModel(source).also { store.put("inbox", it) }
            runCurrent(); vm.refresh(); runCurrent(); vm.read("first"); runCurrent()
            vm.loadMore(); runCurrent()
            assertNotNull(vm.state.value.pageError)
            assertNotNull(vm.state.value.nextCursor)
            fail = false; vm.loadMore(); runCurrent()
            assertNull(vm.state.value.pageError)
            assertEquals(listOf("first", "second"), vm.state.value.notifications.map { it.id })
            assertEquals(1L, vm.state.value.notifications.first().data.readAt)
            vm.read(); runCurrent()
            assertEquals(setOf("second"), source.readIds.last())
            assertFalse(NotificationInbox.hasUnseen.value)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun aForegroundPushLoadsOnlyItsDocumentAndSurvivesAnInFlightFirstPage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val source = Source()
            val first = CompletableDeferred<NotificationPage>()
            source.page = { first.await() }
            val vm = NotificationsViewModel(source).also { store.put("inbox", it) }
            runCurrent(); vm.setForeground(true); runCurrent()
            PushSignals.received("push", "student"); runCurrent()
            first.complete(NotificationPage(listOf(notification("old", 10)), null, 1)); runCurrent()
            assertEquals(listOf("push", "old"), vm.state.value.notifications.map { it.id })
            assertEquals(listOf<String>("push"), source.singleIds)
            assertEquals(1, source.cursors.size)
            PushSignals.received("foreign", "other"); runCurrent()
            assertEquals(listOf<String>("push"), source.singleIds)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun historyStopsAfterFivePagesEvenWhenTheBackendHasMore() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val source = Source()
            var calls = 0
            source.page = {
                val offset = calls++ * 20
                val items = (offset until offset + 20).map { notification("item-$it", (1000 - it).toLong()) }
                NotificationPage(items, NumericPageCursor(items.last().data.createdAt, items.last().id), 20)
            }
            val vm = NotificationsViewModel(source).also { store.put("inbox", it) }
            runCurrent(); vm.refresh(); runCurrent()
            repeat(6) { vm.loadMore(); runCurrent() }
            assertEquals(5, source.cursors.size)
            assertEquals(100, vm.state.value.notifications.size)
            assertNull(vm.state.value.nextCursor)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    companion object {
        private fun notification(id: String, createdAt: Long) = StudentNotification(id, NotificationDto(createdAt = createdAt))
    }
}
