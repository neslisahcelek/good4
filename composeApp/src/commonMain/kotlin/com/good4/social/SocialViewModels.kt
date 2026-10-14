package com.good4.social

import com.good4.analytics.*

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}

// ---------------------------------------------------------------------------
// Create
// ---------------------------------------------------------------------------

data class SocialCreateState(
    val kind: String = "social",
    /** Chosen on step 1; null keeps the type list on screen. */
    val type: String? = null,
    /** null while the generated title is used. */
    val customTitle: String? = null,
    val note: String = "",
    val date: LocalDate,
    val hour: Int,
    val minute: Int,
    val level: String = "any",
    /** Board or digital games; null while no game is chosen. */
    val game: String? = null,
    val capacity: Int = 1,
    val submitting: Boolean = false,
    val error: UiText? = null,
    val createdActivityId: String? = null
) {
    val startsAt: Instant
        get() = LocalDateTime(date, LocalTime(hour, minute)).toInstant(TimeZone.currentSystemDefault())
    val isSport: Boolean get() = kind == "sport"
}

class SocialCreateViewModel(private val repository: SocialRepository, private val analytics: AnalyticsTracker = NoOpAnalyticsTracker) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state = _state.asStateFlow()

    /**
     * Two whole hours from now, so the default leaves time to find people; at
     * night that would be an odd hour, so it moves to 18:00 instead.
     */
    private fun initialState(): SocialCreateState {
        val start = (Clock.System.now() + 120.minutes).toLocalDateTime(TimeZone.currentSystemDefault())
        return when {
            start.hour in 8..21 -> SocialCreateState(date = start.date, hour = start.hour, minute = 0)
            start.hour < 8 -> SocialCreateState(date = start.date, hour = 18, minute = 0)
            else -> SocialCreateState(date = start.date.plus(DatePeriod(days = 1)), hour = 18, minute = 0)
        }
    }

    fun selectKind(kind: String) = _state.update { it.copy(kind = kind) }

    fun selectType(type: SocialType) = _state.update {
        it.copy(
            kind = type.kind, type = type.id, level = if (type.kind == "sport") it.level else "any",
            game = it.game.takeIf { game ->
                val options = when (type.id) {
                    "board-games" -> SOCIAL_BOARD_GAMES
                    "video-games" -> SOCIAL_VIDEO_GAMES
                    else -> emptyList()
                }
                options.any { option -> option.first == game }
            }, error = null
        )
    }

    /** Back from step 2 to the type list, keeping everything else. */
    fun changeType() = _state.update { it.copy(type = null, error = null) }

    fun setTitle(value: String) = _state.update { it.copy(customTitle = value.take(SocialLimits.MAX_TITLE), error = null) }
    fun setNote(value: String) = _state.update { it.copy(note = value.take(SocialLimits.MAX_NOTE), error = null) }
    fun setDate(date: LocalDate) = _state.update { it.copy(date = date, error = null) }
    fun setTime(hour: Int, minute: Int) = _state.update { it.copy(hour = hour.coerceIn(0, 23), minute = minute.coerceIn(0, 59), error = null) }
    fun setLevel(level: String) = _state.update { it.copy(level = level) }
    /** Choosing the chosen game again clears it, since the field is optional. */
    fun setGame(game: String) = _state.update { it.copy(game = if (it.game == game) null else game, error = null) }
    fun setCapacity(value: Int) = _state.update { it.copy(capacity = value.coerceIn(1, SocialLimits.MAX_CAPACITY)) }

    /** [generatedTitle] is the localized "Cumartesi akşamı tenis" the screen shows while no custom title is typed. */
    fun submit(generatedTitle: String) {
        val snapshot = _state.value
        if (snapshot.submitting) return
        val tracking = analytics.click(Interaction.SOCIAL_PUBLISH)
        val type = snapshot.type ?: run {
            analytics.result(tracking, InteractionStatus.VALIDATION_ERROR)
            return
        }
        val title = (snapshot.customTitle ?: generatedTitle).trim()
        val now = Clock.System.now()
        val problem = when {
            title.length < SocialLimits.MIN_TITLE -> Res.string.social_error_title
            snapshot.startsAt < now + SocialLimits.MIN_LEAD_MINUTES.minutes -> Res.string.social_error_starts_at
            snapshot.startsAt > now + SocialLimits.MAX_LEAD_DAYS.days -> Res.string.social_error_starts_at
            else -> null
        }
        if (problem != null) {
            analytics.result(tracking, InteractionStatus.VALIDATION_ERROR)
            _state.update { it.copy(error = UiText.StringResourceId(problem)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            attempt {
                repository.create(SocialActivityDraft(
                    kind = snapshot.kind, type = type, title = title, note = snapshot.note.trim(),
                    startsAt = snapshot.startsAt, level = snapshot.level.takeIf { snapshot.isSport },
                    game = snapshot.game.takeIf { type == "board-games" || type == "video-games" }, capacity = snapshot.capacity
                ))
            }
                .onSuccess { id -> analytics.result(tracking, InteractionStatus.SUCCESS); _state.update { it.copy(submitting = false, createdActivityId = id) } }
                .onFailure { error -> analytics.result(tracking, InteractionStatus.ERROR); _state.update { it.copy(submitting = false, error = socialErrorMessage(error)) } }
        }
    }

    /** The next seven days for the quick date chips. */
    fun quickDates(): List<LocalDate> {
        val today = today()
        return (0 until 7).map { today.plus(DatePeriod(days = it)) }
    }
}

// ---------------------------------------------------------------------------
// Activity detail
// ---------------------------------------------------------------------------

data class SocialActivityState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val detail: SocialActivityDetail? = null,
    val requestNote: String = "",
    val busy: Boolean = false,
    val info: UiText? = null,
    val error: UiText? = null,
    val closed: Boolean = false
)

class SocialActivityViewModel(private val repository: SocialRepository, private val analytics: AnalyticsTracker = NoOpAnalyticsTracker) : ViewModel() {
    private val _state = MutableStateFlow(SocialActivityState())
    val state = _state.asStateFlow()
    private var activityId = ""
    private var pendingJoinTracking: AnalyticsAttempt? = null
    private var joinStarted = false
    fun beginJoin() {
        if (_state.value.busy) return
        joinStarted = true
        pendingJoinTracking = analytics.click(Interaction.SOCIAL_JOIN)
    }
    fun dismissJoin() {
        joinStarted = false
        pendingJoinTracking = null
    }

    fun load(id: String) {
        activityId = id
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.detail == null, loadError = null) }
            attempt { repository.activity(id) }
                .onSuccess { detail -> _state.update { it.copy(isLoading = false, detail = detail) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, loadError = socialErrorMessage(error)) } }
        }
    }

    fun setRequestNote(value: String) = _state.update { it.copy(requestNote = value.take(SocialLimits.MAX_REQUEST_NOTE), error = null) }

    fun requestToJoin() = perform(Interaction.SOCIAL_JOIN) {
        repository.requestToJoin(activityId, _state.value.requestNote)
        _state.update { it.copy(requestNote = "") }
    }

    /** Accepts the rules shown in the sheet, then sends the waiting request. */
    fun acceptTermsAndJoin(version: Int, showName: Boolean) = perform(Interaction.SOCIAL_JOIN) {
        repository.acceptTerms(version, showName)
        repository.requestToJoin(activityId, _state.value.requestNote)
        _state.update { it.copy(requestNote = "") }
    }

    fun withdraw() = perform(Interaction.SOCIAL_WITHDRAW) { repository.withdraw(activityId) }

    fun cancelActivity() = perform(Interaction.SOCIAL_CANCEL) {
        repository.cancel(activityId)
        _state.update { it.copy(info = UiText.StringResourceId(Res.string.social_cancelled_info)) }
    }

    fun report(reason: String, note: String) = perform {
        repository.report("activity", activityId, reason, note)
        _state.update { it.copy(info = UiText.StringResourceId(Res.string.social_report_sent)) }
    }

    fun clearInfo() = _state.update { it.copy(info = null) }

    private fun perform(interaction: Interaction? = null, block: suspend () -> Unit) {
        if (_state.value.busy) return
        val tracking = if (interaction == Interaction.SOCIAL_JOIN && joinStarted) pendingJoinTracking
            else interaction?.let { analytics.click(it) }
        if (interaction == Interaction.SOCIAL_JOIN) dismissJoin()
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            attempt { block() }
                .onSuccess {
                    analytics.result(tracking, InteractionStatus.SUCCESS)
                    _state.update { it.copy(busy = false) }
                    attempt { repository.activity(activityId) }.onSuccess { detail -> _state.update { it.copy(detail = detail) } }
                }
                .onFailure { error ->
                    analytics.result(tracking, InteractionStatus.ERROR)
                    _state.update { it.copy(busy = false, error = socialErrorMessage(error)) }
                    // A changed activity (filled up, cancelled) is shown as it is now.
                    attempt { repository.activity(activityId) }.onSuccess { detail -> _state.update { it.copy(detail = detail) } }
                }
        }
    }
}

// ---------------------------------------------------------------------------
// Organizer's requests
// ---------------------------------------------------------------------------

data class SocialRequestsState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val list: SocialRequestList? = null,
    val busyUid: String? = null,
    val error: UiText? = null
)

class SocialRequestsViewModel(
    private val repository: SocialRepository,
    private val analytics: AnalyticsTracker = NoOpAnalyticsTracker
) : ViewModel() {
    private val _state = MutableStateFlow(SocialRequestsState())
    val state = _state.asStateFlow()
    private var activityId = ""

    fun load(id: String) {
        activityId = id
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        attempt { repository.requests(activityId) }
            .onSuccess { list -> _state.update { it.copy(isLoading = false, loadError = null, list = list) } }
            .onFailure { error -> _state.update { it.copy(isLoading = false, loadError = if (it.list == null) socialErrorMessage(error) else null) } }
    }

    fun respond(requesterUid: String, accept: Boolean) {
        if (_state.value.busyUid != null) return
        val target = if (accept) AnalyticsTarget.ACCEPT else AnalyticsTarget.REJECT
        val tracking = analytics.click(Interaction.SOCIAL_REQUEST_RESPOND, target)
        viewModelScope.launch {
            _state.update { it.copy(busyUid = requesterUid, error = null) }
            attempt { repository.respond(activityId, requesterUid, accept) }
                .onSuccess { analytics.result(tracking, InteractionStatus.SUCCESS) }
                .onFailure { error ->
                    analytics.result(tracking, InteractionStatus.ERROR)
                    _state.update { it.copy(error = socialErrorMessage(error)) }
                }
            refresh()
            _state.update { it.copy(busyUid = null) }
        }
    }
}

// ---------------------------------------------------------------------------
// Chats
// ---------------------------------------------------------------------------

data class SocialInboxState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val inbox: SocialInbox? = null
)

class SocialInboxViewModel(private val repository: SocialRepository) : ViewModel() {
    private val _state = MutableStateFlow(SocialInboxState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.inbox == null, loadError = null) }
            attempt { repository.inbox() }
                .onSuccess { inbox -> _state.update { it.copy(isLoading = false, inbox = inbox) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, loadError = socialErrorMessage(error)) } }
        }
    }
}

data class SocialChatState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val conversation: SocialConversation? = null,
    val messages: List<SocialMessage> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,
    val error: UiText? = null,
    val info: UiText? = null
)

class SocialChatViewModel(
    private val repository: SocialRepository,
    private val analytics: AnalyticsTracker = NoOpAnalyticsTracker
) : ViewModel() {
    private val _state = MutableStateFlow(SocialChatState())
    val state = _state.asStateFlow()
    private var conversationId = ""
    private var pollJob: Job? = null
    private var lastActivityAt = 0L
    private var messageCursor: String? = null
    private val refreshMutex = Mutex()

    fun load(id: String) {
        conversationId = id
        viewModelScope.launch { refresh(full = true) }
    }

    /** Polls while the chat is on screen: every 4 s after recent activity, every 15 s when quiet. */
    fun startPolling() {
        pollJob?.cancel()
        markActivity()
        pollJob = viewModelScope.launch {
            while (isActive) {
                val quiet = Clock.System.now().toEpochMilliseconds() - lastActivityAt > 120_000
                delay(if (quiet) 15_000 else 4_000)
                if (_state.value.loadError == null && !_state.value.isLoading) refresh(full = false)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun markActivity() {
        lastActivityAt = Clock.System.now().toEpochMilliseconds()
    }

    private suspend fun refresh(full: Boolean) = refreshMutex.withLock {
        var firstPage = true
        do {
            val after = if (full && firstPage) null else (messageCursor
                ?: _state.value.messages.lastOrNull()?.createdAt)
            val result = attempt { repository.messages(conversationId, after) }
            val thread = result.getOrNull()
            if (thread == null) {
                if (full) _state.update { it.copy(isLoading = false, loadError = socialErrorMessage(result.exceptionOrNull()!!)) }
                break
            }
            if (thread.messages.isNotEmpty() && !full) markActivity()
            messageCursor = thread.nextAfter
            _state.update {
                it.copy(isLoading = false, loadError = null, conversation = thread.conversation,
                    messages = if (full && firstPage) thread.messages
                        else (it.messages + thread.messages).distinctBy(SocialMessage::id))
            }
            firstPage = false
        } while (thread.hasMore)
    }

    fun setDraft(value: String) = _state.update { it.copy(draft = value.take(SocialLimits.MAX_MESSAGE), error = null) }

    fun send() {
        val text = _state.value.draft.trim()
        if (text.isEmpty()) return
        runSend(clearDraft = true, interaction = Interaction.SOCIAL_CHAT_MESSAGE) { repository.sendMessage(conversationId, text) }
    }

    fun block() = runSend(clearDraft = false) {
        repository.block(conversationId)
        _state.update { it.copy(info = UiText.StringResourceId(Res.string.social_blocked_info)) }
    }

    fun unblock() = runSend(clearDraft = false) {
        repository.unblock(conversationId)
        _state.update { it.copy(info = UiText.StringResourceId(Res.string.social_engel_kaldirildi)) }
    }

    fun report(reason: String, note: String) = runSend(clearDraft = false) {
        repository.report("conversation", conversationId, reason, note)
        _state.update { it.copy(info = UiText.StringResourceId(Res.string.social_report_sent)) }
    }

    fun clearInfo() = _state.update { it.copy(info = null) }

    private fun runSend(clearDraft: Boolean, interaction: Interaction? = null, block: suspend () -> Unit) {
        if (_state.value.sending) return
        val tracking = interaction?.let { analytics.click(it) }
        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }
            attempt { block() }
                .onSuccess {
                    analytics.result(tracking, InteractionStatus.SUCCESS)
                    markActivity()
                    _state.update { it.copy(sending = false, draft = if (clearDraft) "" else it.draft) }
                    refresh(full = true)
                }
                .onFailure { error ->
                    analytics.result(tracking, InteractionStatus.ERROR)
                    _state.update { it.copy(sending = false, error = socialErrorMessage(error)) }
                }
        }
    }
}
