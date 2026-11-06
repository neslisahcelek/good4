package com.good4.quiz.presentation.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.analytics.AnalyticsTracker
import com.good4.analytics.Interaction
import com.good4.analytics.NoOpAnalyticsTracker
import com.good4.auth.data.repository.AuthRepository
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

enum class PlayerScreenPhase {
    ENTER_NICKNAME,
    LOBBY,
    GET_READY,
    QUESTION,
    SUBMITTED,
    REVEAL,
    FINISHED,
    CANCELLED
}

data class QuizPlayerUiState(
    val phase: PlayerScreenPhase = PlayerScreenPhase.ENTER_NICKNAME,
    val session: QuizSession? = null,
    val player: QuizPlayer? = null,
    val nicknameInput: String = "",
    val nicknameError: UiText? = null,
    val isJoining: Boolean = false,
    val isSubmittingAnswer: Boolean = false,
    val selectedOption: Int? = null,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val canBackupReveal: Boolean = false,
    val uiMessage: UiText? = null
)

class QuizPlayerViewModel(
    private val sessionId: String,
    private val repository: QuizRepository,
    private val gateway: QuizGateway = NativeQuizGateway,
    private val authRepository: AuthRepository,
    private val analytics: AnalyticsTracker = NoOpAnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizPlayerUiState())
    val uiState: StateFlow<QuizPlayerUiState> = _uiState.asStateFlow()

    private var clockOffsetMs: Long = 0L
    private var countdownJob: Job? = null
    private var playerJob: Job? = null
    private var sessionJob: Job? = null
    private var lastObservedQuestionIndex: Int = -1

    init {
        observeSession()
        observeCurrentUserPlayer()
    }

    private fun serverNow(): Long = Clock.System.now().toEpochMilliseconds() + clockOffsetMs

    private fun observeSession() {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            gateway.observeSession(sessionId)
                .catch { e ->
                    _uiState.update { it.copy(uiMessage = mapQuizErrorToUiText(e.message)) }
                }
                .collect { session ->
                    _uiState.update { it.copy(session = session) }
                    syncPhaseWithSession(session)
                }
        }
    }

    private fun observeCurrentUserPlayer() {
        val uid = authRepository.currentUser?.uid ?: return
        playerJob?.cancel()
        playerJob = viewModelScope.launch {
            gateway.observePlayer(sessionId, uid)
                .catch { e ->
                    _uiState.update { it.copy(uiMessage = mapQuizErrorToUiText(e.message)) }
                }
                .collect { player ->
                    _uiState.update { current ->
                        val newPhase = if (player != null && current.phase == PlayerScreenPhase.ENTER_NICKNAME) {
                            PlayerScreenPhase.LOBBY
                        } else current.phase
                        current.copy(player = player, phase = newPhase)
                    }
                }
        }
    }

    private fun syncPhaseWithSession(session: QuizSession?) {
        if (session == null) {
            stopCountdown()
            _uiState.update { current ->
                if (current.phase != PlayerScreenPhase.ENTER_NICKNAME && current.phase != PlayerScreenPhase.FINISHED) {
                    current.copy(phase = PlayerScreenPhase.CANCELLED)
                } else current
            }
            return
        }

        when (session.status) {
            "lobby" -> {
                stopCountdown()
                _uiState.update {
                    if (it.player != null) it.copy(phase = PlayerScreenPhase.LOBBY)
                    else it.copy(phase = PlayerScreenPhase.ENTER_NICKNAME)
                }
            }
            "question" -> {
                if (session.currentIndex != lastObservedQuestionIndex) {
                    lastObservedQuestionIndex = session.currentIndex
                    _uiState.update { it.copy(selectedOption = null) }
                }
                startQuestionCountdown(session)
            }
            "reveal" -> {
                stopCountdown()
                _uiState.update {
                    it.copy(
                        phase = PlayerScreenPhase.REVEAL,
                        canBackupReveal = false
                    )
                }
            }
            "finished" -> {
                stopCountdown()
                _uiState.update { it.copy(phase = PlayerScreenPhase.FINISHED) }
            }
            "cancelled" -> {
                stopCountdown()
                _uiState.update { it.copy(phase = PlayerScreenPhase.CANCELLED) }
            }
        }
    }

    private fun startQuestionCountdown(session: QuizSession) {
        countdownJob?.cancel()
        val startsAt = session.questionStartsAt ?: (serverNow() + 3000L)
        val endsAt = session.questionEndsAt ?: (startsAt + 20_000L)
        val totalSec = ((endsAt - startsAt) / 1000L).toInt().coerceAtLeast(1)

        countdownJob = viewModelScope.launch {
            while (isActive) {
                val now = serverNow()
                if (now < startsAt) {
                    val prepRemaining = ((startsAt - now) / 1000L).toInt().coerceAtLeast(1)
                    _uiState.update {
                        it.copy(
                            phase = PlayerScreenPhase.GET_READY,
                            remainingSeconds = prepRemaining,
                            totalSeconds = 3
                        )
                    }
                } else if (now <= endsAt) {
                    val remaining = ((endsAt - now) / 1000L).toInt().coerceAtLeast(0)
                    _uiState.update { current ->
                        val phase = if (current.selectedOption != null) PlayerScreenPhase.SUBMITTED else PlayerScreenPhase.QUESTION
                        current.copy(
                            phase = phase,
                            remainingSeconds = remaining,
                            totalSeconds = totalSec,
                            canBackupReveal = false
                        )
                    }
                } else {
                    // Time up! Player can backup reveal 2 seconds after deadline
                    val canReveal = now >= endsAt + 2000L
                    _uiState.update { current ->
                        val phase = if (current.selectedOption != null) PlayerScreenPhase.SUBMITTED else PlayerScreenPhase.QUESTION
                        current.copy(
                            phase = phase,
                            remainingSeconds = 0,
                            totalSeconds = totalSec,
                            canBackupReveal = canReveal
                        )
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    fun onNicknameChange(value: String) {
        _uiState.update { it.copy(nicknameInput = value, nicknameError = null) }
    }

    fun joinSession() {
        val nickname = _uiState.value.nicknameInput.trim()
        if (nickname.length < 2 || nickname.length > 16) {
            _uiState.update { it.copy(nicknameError = UiText.StringResourceId(Res.string.quiz_error_nickname_invalid)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, nicknameError = null) }
            analytics.click(Interaction.QUIZ_JOIN)
            when (val result = repository.joinQuizSession(sessionId, nickname)) {
                is Result.Success -> {
                    clockOffsetMs = result.data.serverTimeMs - Clock.System.now().toEpochMilliseconds()
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            phase = PlayerScreenPhase.LOBBY
                        )
                    }
                    observeCurrentUserPlayer()
                }
                is Result.Error -> {
                    val errorUiText = mapQuizErrorToUiText(result.error.message)
                    _uiState.update { it.copy(isJoining = false, nicknameError = errorUiText) }
                }
            }
        }
    }

    fun submitAnswer(optionIndex: Int) {
        val currentSession = _uiState.value.session ?: return
        if (_uiState.value.selectedOption != null || _uiState.value.isSubmittingAnswer) return
        val questionIndex = currentSession.currentIndex
        if (questionIndex < 0) return

        _uiState.update {
            it.copy(
                selectedOption = optionIndex,
                isSubmittingAnswer = true,
                phase = PlayerScreenPhase.SUBMITTED
            )
        }

        viewModelScope.launch {
            analytics.click(Interaction.QUIZ_SUBMIT_ANSWER)
            when (val result = repository.submitQuizAnswer(sessionId, questionIndex, optionIndex)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmittingAnswer = false) }
                }
                is Result.Error -> {
                    val isAlreadyAnswered = result.error.message.contains("QUIZ_ALREADY_ANSWERED", ignoreCase = true)
                    _uiState.update { current ->
                        current.copy(
                            isSubmittingAnswer = false,
                            selectedOption = if (isAlreadyAnswered) current.selectedOption else null,
                            phase = if (isAlreadyAnswered) current.phase else PlayerScreenPhase.QUESTION,
                            uiMessage = mapQuizErrorToUiText(result.error.message)
                        )
                    }
                }
            }
        }
    }

    fun triggerBackupReveal() {
        val session = _uiState.value.session ?: return
        if (!_uiState.value.canBackupReveal) return

        viewModelScope.launch {
            repository.revealQuizQuestion(sessionId, session.currentIndex)
        }
    }

    fun clearUiMessage() {
        _uiState.update { it.copy(uiMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopCountdown()
        playerJob?.cancel()
        sessionJob?.cancel()
    }
}
