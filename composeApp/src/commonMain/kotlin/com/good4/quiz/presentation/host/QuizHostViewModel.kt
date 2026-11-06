package com.good4.quiz.presentation.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.analytics.AnalyticsTracker
import com.good4.analytics.Interaction
import com.good4.analytics.NoOpAnalyticsTracker
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.quiz.data.NativeQuizGateway
import com.good4.quiz.data.QuizGateway
import com.good4.quiz.data.QuizRepository
import com.good4.quiz.data.mapQuizErrorToUiText
import com.good4.quiz.domain.QuizPlayer
import com.good4.quiz.domain.QuizSession
import good4.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class QuizHostUiState(
    val session: QuizSession? = null,
    val players: List<QuizPlayer> = emptyList(),
    val answeredCount: Int = 0,
    val isLoading: Boolean = false,
    val isStartingGame: Boolean = false,
    val isTogglingLock: Boolean = false,
    val isRevealing: Boolean = false,
    val isNexting: Boolean = false,
    val isKicking: Boolean = false,
    val playerToKick: QuizPlayer? = null,
    val showCancelDialog: Boolean = false,
    val isCancelling: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val uiMessage: UiText? = null
)

class QuizHostViewModel(
    private val sessionId: String,
    private val repository: QuizRepository,
    private val gateway: QuizGateway = NativeQuizGateway,
    private val analytics: AnalyticsTracker = NoOpAnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizHostUiState())
    val uiState: StateFlow<QuizHostUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null
    private var playersJob: Job? = null
    private var countdownJob: Job? = null
    private var answerCountJob: Job? = null

    init {
        observeSession()
        observePlayers()
    }

    private fun observeSession() {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            gateway.observeSession(sessionId)
                .catch { e ->
                    _uiState.update { it.copy(uiMessage = mapQuizErrorToUiText(e.message)) }
                }
                .collect { session ->
                    _uiState.update { it.copy(session = session) }
                    syncSessionState(session)
                }
        }
    }

    private fun observePlayers() {
        playersJob?.cancel()
        playersJob = viewModelScope.launch {
            gateway.observePlayers(sessionId)
                .catch { e ->
                    _uiState.update { it.copy(uiMessage = mapQuizErrorToUiText(e.message)) }
                }
                .collect { players ->
                    _uiState.update { it.copy(players = players.filter { p -> p.isActive }) }
                }
        }
    }

    private fun syncSessionState(session: QuizSession?) {
        if (session == null) return

        when (session.status) {
            "question" -> {
                observeAnswerCount(session.currentIndex)
                startHostCountdown(session)
            }
            else -> {
                stopCountdown()
            }
        }
    }

    private fun observeAnswerCount(questionIndex: Int) {
        answerCountJob?.cancel()
        answerCountJob = viewModelScope.launch {
            gateway.observeAnswerCount(sessionId, questionIndex)
                .catch { e ->
                    _uiState.update { it.copy(uiMessage = mapQuizErrorToUiText(e.message)) }
                }
                .collect { count ->
                    _uiState.update { it.copy(answeredCount = count) }
                }
        }
    }

    private fun startHostCountdown(session: QuizSession) {
        countdownJob?.cancel()
        val startsAt = session.questionStartsAt ?: (Clock.System.now().toEpochMilliseconds() + 3000L)
        val endsAt = session.questionEndsAt ?: (startsAt + 20_000L)
        val totalSec = ((endsAt - startsAt) / 1000L).toInt().coerceAtLeast(1)

        countdownJob = viewModelScope.launch {
            while (isActive) {
                val now = Clock.System.now().toEpochMilliseconds()
                val remaining = if (now < startsAt) {
                    totalSec
                } else {
                    ((endsAt - now) / 1000L).toInt().coerceAtLeast(0)
                }
                _uiState.update {
                    it.copy(
                        remainingSeconds = remaining,
                        totalSeconds = totalSec
                    )
                }
                delay(250)
            }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        answerCountJob?.cancel()
        answerCountJob = null
    }

    fun toggleLock() {
        val session = _uiState.value.session ?: return
        val newLocked = !session.locked
        viewModelScope.launch {
            _uiState.update { it.copy(isTogglingLock = true) }
            analytics.click(Interaction.QUIZ_HOST_LOCK)
            when (val result = repository.setQuizLock(sessionId, newLocked)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isTogglingLock = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isTogglingLock = false,
                            uiMessage = mapQuizErrorToUiText(result.error.message)
                        )
                    }
                }
            }
        }
    }

    fun startGame() {
        viewModelScope.launch {
            _uiState.update { it.copy(isStartingGame = true) }
            analytics.click(Interaction.QUIZ_HOST_START)
            when (val result = repository.startQuizGame(sessionId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isStartingGame = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isStartingGame = false,
                            uiMessage = mapQuizErrorToUiText(result.error.message)
                        )
                    }
                }
            }
        }
    }

    fun revealQuestion() {
        val session = _uiState.value.session ?: return
        val startsAt = session.questionStartsAt ?: 0L
        if (Clock.System.now().toEpochMilliseconds() < startsAt) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRevealing = true) }
            analytics.click(Interaction.QUIZ_HOST_REVEAL)
            when (val result = repository.revealQuizQuestion(sessionId, session.currentIndex)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRevealing = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isRevealing = false,
                            uiMessage = mapQuizErrorToUiText(result.error.message)
                        )
                    }
                }
            }
        }
    }

    fun nextQuestion() {
        viewModelScope.launch {
            _uiState.update { it.copy(isNexting = true) }
            analytics.click(Interaction.QUIZ_HOST_NEXT)
            when (val result = repository.nextQuizQuestion(sessionId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isNexting = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isNexting = false,
                            uiMessage = mapQuizErrorToUiText(result.error.message)
                        )
                    }
                }
            }
        }
    }

    fun requestKickPlayer(player: QuizPlayer) {
        _uiState.update { it.copy(playerToKick = player) }
    }

    fun dismissKickDialog() {
        _uiState.update { it.copy(playerToKick = null) }
    }

    fun confirmKickPlayer() {
        val player = _uiState.value.playerToKick ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isKicking = true) }
            analytics.click(Interaction.QUIZ_HOST_KICK)
            when (val result = repository.kickQuizPlayer(sessionId, player.userId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isKicking = false, playerToKick = null) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isKicking = false,
                            playerToKick = null,
                            uiMessage = mapQuizErrorToUiText(result.error.message)
                        )
                    }
                }
            }
        }
    }

    fun requestCancelSession() {
        _uiState.update { it.copy(showCancelDialog = true) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = false) }
    }

    fun confirmCancelSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, showCancelDialog = false) }
            analytics.click(Interaction.QUIZ_HOST_CANCEL)
            when (val result = repository.cancelQuizSession(sessionId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isCancelling = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            uiMessage = mapQuizErrorToUiText(result.error.message)
                        )
                    }
                }
            }
        }
    }

    fun clearUiMessage() {
        _uiState.update { it.copy(uiMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopCountdown()
        sessionJob?.cancel()
        playersJob?.cancel()
    }
}
