package com.good4.quiz.presentation.host

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AccentYellow
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4ConfirmDialog
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.quiz.presentation.components.QuizAnswerShape
import com.good4.quiz.domain.QuizPlayer
import com.good4.quiz.presentation.components.QuizCountdownBar
import com.good4.quiz.presentation.components.QuizPodium
import com.good4.quiz.presentation.components.QuizScoreboard
import com.good4.quiz.presentation.components.QuizShapeIcon
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun QuizHostScreen(
    viewModel: QuizHostViewModel,
    onBackClick: () -> Unit,
    onViewReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiMessage = state.uiMessage?.asString()

    val currentSession = state.session
    val isHostActive = currentSession != null && !currentSession.isFinished && !currentSession.isCancelled
    com.good4.community.CommunityBackHandler {
        if (isHostActive) {
            viewModel.requestCancelSession()
        } else {
            onBackClick()
        }
    }

    LaunchedEffect(uiMessage) {
        if (uiMessage != null) {
            snackbarHostState.showSnackbar(uiMessage)
            viewModel.clearUiMessage()
        }
    }

    // Kick Player Dialog
    state.playerToKick?.let { player ->
        Good4ConfirmDialog(
            title = stringResource(Res.string.quiz_host_kick_confirm_title),
            message = stringResource(Res.string.quiz_host_kick_confirm_desc, player.nickname),
            confirmLabel = stringResource(Res.string.quiz_host_kick_player),
            dismissLabel = stringResource(Res.string.back),
            confirmColor = ErrorRed,
            onConfirm = viewModel::confirmKickPlayer,
            onDismiss = viewModel::dismissKickDialog
        )
    }

    // Cancel Session Dialog
    if (state.showCancelDialog) {
        Good4ConfirmDialog(
            title = stringResource(Res.string.quiz_host_cancel_session),
            message = stringResource(Res.string.quiz_host_cancel_confirm_desc),
            confirmLabel = stringResource(Res.string.quiz_host_cancel_session),
            dismissLabel = stringResource(Res.string.back),
            confirmColor = ErrorRed,
            onConfirm = {
                viewModel.confirmCancelSession()
                onBackClick()
            },
            onDismiss = viewModel::dismissCancelDialog
        )
    }

    Good4Scaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.quiz_host_title),
                navigationIcon = {
                    IconButton(onClick = {
                        val session = state.session
                        if (session != null && !session.isFinished && !session.isCancelled) {
                            viewModel.requestCancelSession()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    val session = state.session
                    if (session != null && !session.isFinished && !session.isCancelled) {
                        TextButton(onClick = viewModel::requestCancelSession) {
                            Text(
                                text = stringResource(Res.string.quiz_host_cancel_session),
                                color = ErrorRed,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val session = state.session

            if (session == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGreen)
                }
            } else {
                when (session.status) {
                    "lobby" -> {
                        HostLobbyContent(
                            state = state,
                            onToggleLock = viewModel::toggleLock,
                            onStartGame = viewModel::startGame,
                            onKickPlayer = viewModel::requestKickPlayer
                        )
                    }
                    "question" -> {
                        HostQuestionContent(
                            state = state,
                            onEarlyReveal = viewModel::revealQuestion
                        )
                    }
                    "reveal" -> {
                        HostRevealContent(
                            state = state,
                            onNext = viewModel::nextQuestion
                        )
                    }
                    "finished" -> {
                        HostFinishedContent(
                            state = state,
                            onViewReport = { onViewReport(session.id) },
                            onExit = onBackClick
                        )
                    }
                    else -> {
                        // Cancelled
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.quiz_session_cancelled_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HostLobbyContent(
    state: QuizHostUiState,
    onToggleLock: () -> Unit,
    onStartGame: () -> Unit,
    onKickPlayer: (QuizPlayer) -> Unit
) {
    val session = state.session ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Lobby Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDefault,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = session.title.ifBlank { stringResource(Res.string.quiz_title) },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.quiz_lobby_player_count, state.players.size),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )

                        // Lock / Unlock button
                        OutlinedButton(
                            onClick = onToggleLock,
                            enabled = !state.isTogglingLock,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (session.locked) stringResource(Res.string.quiz_host_unlock_session)
                                else stringResource(Res.string.quiz_host_lock_session),
                                color = if (session.locked) ErrorRed else PrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.quiz_lobby_players_title),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.players.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceMuted
                ) {
                    Text(
                        text = stringResource(Res.string.quiz_lobby_waiting_host),
                        modifier = Modifier.padding(16.dp),
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.players, key = { it.userId }) { player ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceDefault,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = player.nickname.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Text(
                                        text = player.nickname,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                }

                                TextButton(
                                    onClick = { onKickPlayer(player) }
                                ) {
                                    Text(
                                        text = stringResource(Res.string.quiz_host_kick_player),
                                        color = ErrorRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onStartGame,
            enabled = !state.isStartingGame && state.players.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = Color.White
            )
        ) {
            if (state.isStartingGame) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(Res.string.quiz_host_start_game),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun HostQuestionContent(
    state: QuizHostUiState,
    onEarlyReveal: () -> Unit
) {
    val session = state.session ?: return
    val question = session.currentQuestion ?: return
    val currentIndex = (session.currentIndex + 1).coerceAtLeast(1)
    val totalCount = session.totalQuestions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.quiz_question_counter, currentIndex, totalCount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextSecondary
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = stringResource(Res.string.quiz_host_answered_count, state.answeredCount, state.players.size),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            QuizCountdownBar(
                remainingSeconds = state.remainingSeconds,
                totalSeconds = state.totalSeconds
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDefault,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = question.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    question.options.forEachIndexed { i, opt ->
                        val shape = QuizAnswerShape.fromIndex(i)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuizShapeIcon(
                                shape = shape,
                                modifier = Modifier.size(20.dp),
                                color = shape.color
                            )
                            Text(
                                text = if (question.isTrueFalse) {
                                    if (i == 0) stringResource(Res.string.quiz_true)
                                    else stringResource(Res.string.quiz_false)
                                } else opt,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onEarlyReveal,
            enabled = !state.isRevealing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = Color.White
            )
        ) {
            if (state.isRevealing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(Res.string.quiz_host_early_reveal),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun HostRevealContent(
    state: QuizHostUiState,
    onNext: () -> Unit
) {
    val session = state.session ?: return
    val reveal = session.reveal
    val currentQuestion = session.currentQuestion
    val isLastQuestion = session.currentIndex >= session.totalQuestions - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "distribution") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDefault,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.quiz_host_distribution_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )

                        if (reveal != null && currentQuestion != null) {
                            val totalVotes = reveal.distribution.sum() + reveal.unanswered
                            currentQuestion.options.forEachIndexed { i, opt ->
                                val shape = QuizAnswerShape.fromIndex(i)
                                val count = reveal.distribution.getOrElse(i) { 0 }
                                val isCorrect = i == reveal.correctIndex
                                val pct = if (totalVotes > 0) count * 100 / totalVotes else 0

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCorrect) PrimaryGreen.copy(alpha = 0.12f) else SurfaceMuted)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        QuizShapeIcon(
                                            shape = shape,
                                            modifier = Modifier.size(18.dp),
                                            color = shape.color
                                        )
                                        Text(
                                            text = if (currentQuestion.isTrueFalse) {
                                                if (i == 0) stringResource(Res.string.quiz_true)
                                                else stringResource(Res.string.quiz_false)
                                            } else opt,
                                            fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isCorrect) {
                                            Text(
                                                text = "✓",
                                                color = PrimaryGreen,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = "$count (%$pct)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (isCorrect) PrimaryGreen else TextSecondary
                                    )
                                }
                            }

                            if (reveal.unanswered > 0) {
                                Text(
                                    text = stringResource(Res.string.quiz_host_unanswered_count, reveal.unanswered),
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "scoreboard") {
                if (reveal != null) {
                    QuizScoreboard(topPlayers = reveal.top5)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            enabled = !state.isNexting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = Color.White
            )
        ) {
            if (state.isNexting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isLastQuestion) stringResource(Res.string.quiz_host_finish_quiz)
                    else stringResource(Res.string.quiz_host_next_question),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun HostFinishedContent(
    state: QuizHostUiState,
    onViewReport: () -> Unit,
    onExit: () -> Unit
) {
    val podium = state.session?.podium ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "podium") {
            QuizPodium(podium = podium)
        }

        item(key = "actions") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onViewReport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.quiz_host_view_report),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.quiz_exit_button),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
