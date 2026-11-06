package com.good4.quiz.presentation.play

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.quiz.presentation.components.QuizAnswerButton
import com.good4.quiz.presentation.components.QuizCountdownBar
import com.good4.quiz.presentation.components.QuizPodium
import com.good4.quiz.presentation.components.QuizScoreboard
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun QuizPlayerScreen(
    viewModel: QuizPlayerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiMessage = state.uiMessage?.asString()

    val isGameActive = state.phase == PlayerScreenPhase.GET_READY ||
        state.phase == PlayerScreenPhase.QUESTION ||
        state.phase == PlayerScreenPhase.SUBMITTED
    com.good4.community.CommunityBackHandler {
        if (!isGameActive) {
            onBackClick()
        }
    }

    LaunchedEffect(uiMessage) {
        if (uiMessage != null) {
            snackbarHostState.showSnackbar(uiMessage)
            viewModel.clearUiMessage()
        }
    }

    Good4Scaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = state.session?.title?.ifBlank { stringResource(Res.string.quiz_title) }
                    ?: stringResource(Res.string.quiz_title),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            tint = TextPrimary
                        )
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
            when (state.phase) {
                PlayerScreenPhase.ENTER_NICKNAME -> {
                    EnterNicknameContent(
                        nickname = state.nicknameInput,
                        error = state.nicknameError?.asString(),
                        isLoading = state.isJoining,
                        onNicknameChange = viewModel::onNicknameChange,
                        onSubmit = viewModel::joinSession
                    )
                }
                PlayerScreenPhase.LOBBY -> {
                    LobbyWaitingContent(
                        nickname = state.player?.nickname ?: state.nicknameInput,
                        sessionTitle = state.session?.title.orEmpty()
                    )
                }
                PlayerScreenPhase.GET_READY -> {
                    GetReadyContent(
                        remainingSeconds = state.remainingSeconds,
                        questionIndex = (state.session?.currentIndex ?: 0) + 1,
                        totalQuestions = state.session?.totalQuestions ?: 0
                    )
                }
                PlayerScreenPhase.QUESTION -> {
                    QuestionActiveContent(
                        state = state,
                        onSelectOption = viewModel::submitAnswer
                    )
                }
                PlayerScreenPhase.SUBMITTED -> {
                    AnswerSubmittedContent(
                        remainingSeconds = state.remainingSeconds,
                        totalSeconds = state.totalSeconds,
                        canBackupReveal = state.canBackupReveal,
                        onBackupReveal = viewModel::triggerBackupReveal
                    )
                }
                PlayerScreenPhase.REVEAL -> {
                    RevealContent(
                        state = state
                    )
                }
                PlayerScreenPhase.FINISHED -> {
                    FinishedContent(
                        state = state,
                        onExit = onBackClick
                    )
                }
                PlayerScreenPhase.CANCELLED -> {
                    CancelledContent(
                        onExit = onBackClick
                    )
                }
            }
        }
    }
}

@Composable
private fun EnterNicknameContent(
    nickname: String,
    error: String?,
    isLoading: Boolean,
    onNicknameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceDefault,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.quiz_nickname_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(Res.string.quiz_nickname_subtitle),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(Res.string.quiz_nickname_placeholder),
                            color = TextSecondary
                        )
                    },
                    isError = error != null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.15f)
                    )
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = error,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onSubmit,
                    enabled = !isLoading && nickname.trim().length >= 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.quiz_nickname_submit),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyWaitingContent(
    nickname: String,
    sessionTitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceDefault,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉",
                    fontSize = 44.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.quiz_lobby_ready_message),
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Nickname pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = nickname,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryGreen
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = PrimaryGreen,
                    strokeWidth = 3.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.quiz_lobby_waiting_host),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GetReadyContent(
    remainingSeconds: Int,
    questionIndex: Int,
    totalQuestions: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.quiz_question_counter, questionIndex, totalQuestions),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.quiz_get_ready_title),
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(PrimaryGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$remainingSeconds",
                fontWeight = FontWeight.Black,
                fontSize = 44.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(Res.string.quiz_get_ready_subtitle),
            fontSize = 15.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun QuestionActiveContent(
    state: QuizPlayerUiState,
    onSelectOption: (Int) -> Unit
) {
    val question = state.session?.currentQuestion ?: return
    val currentIndex = (state.session.currentIndex + 1).coerceAtLeast(1)
    val totalCount = state.session.totalQuestions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Counter + Timer + Question Text
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.quiz_question_counter, currentIndex, totalCount),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
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
                Text(
                    text = question.text,
                    modifier = Modifier.padding(18.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom section: Answer buttons 2x2 grid
        val options = question.options
        if (question.isTrueFalse) {
            // True / False: 2 options stacked or row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuizAnswerButton(
                    index = 0,
                    text = stringResource(Res.string.quiz_true),
                    onClick = { onSelectOption(0) },
                    modifier = Modifier.weight(1f),
                    isSelected = state.selectedOption == 0,
                    enabled = state.selectedOption == null
                )
                QuizAnswerButton(
                    index = 1,
                    text = stringResource(Res.string.quiz_false),
                    onClick = { onSelectOption(1) },
                    modifier = Modifier.weight(1f),
                    isSelected = state.selectedOption == 1,
                    enabled = state.selectedOption == null
                )
            }
        } else {
            // Multiple Choice: 2x2 grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuizAnswerButton(
                        index = 0,
                        text = options.getOrElse(0) { "" },
                        onClick = { onSelectOption(0) },
                        modifier = Modifier.weight(1f),
                        isSelected = state.selectedOption == 0,
                        enabled = state.selectedOption == null
                    )
                    QuizAnswerButton(
                        index = 1,
                        text = options.getOrElse(1) { "" },
                        onClick = { onSelectOption(1) },
                        modifier = Modifier.weight(1f),
                        isSelected = state.selectedOption == 1,
                        enabled = state.selectedOption == null
                    )
                }

                if (options.size > 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuizAnswerButton(
                            index = 2,
                            text = options.getOrElse(2) { "" },
                            onClick = { onSelectOption(2) },
                            modifier = Modifier.weight(1f),
                            isSelected = state.selectedOption == 2,
                            enabled = state.selectedOption == null
                        )
                        if (options.size > 3) {
                            QuizAnswerButton(
                                index = 3,
                                text = options.getOrElse(3) { "" },
                                onClick = { onSelectOption(3) },
                                modifier = Modifier.weight(1f),
                                isSelected = state.selectedOption == 3,
                                enabled = state.selectedOption == null
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerSubmittedContent(
    remainingSeconds: Int,
    totalSeconds: Int,
    canBackupReveal: Boolean,
    onBackupReveal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceDefault,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = PrimaryGreen
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(Res.string.quiz_answer_submitted),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(Res.string.quiz_waiting_others),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                QuizCountdownBar(
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalSeconds
                )

                if (canBackupReveal) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onBackupReveal,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.quiz_reveal_backup_button),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RevealContent(
    state: QuizPlayerUiState
) {
    val lastResult = state.player?.lastResult
    val isCorrect = lastResult?.correct == true
    val points = lastResult?.points ?: 0
    val rank = state.player?.rank ?: 0
    val streak = state.player?.streak ?: 0
    val top5 = state.session?.reveal?.top5 ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "result_card") {
            val bannerBg = if (isCorrect) PrimaryGreen else ErrorRed

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = bannerBg,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isCorrect) "✓" else "✕",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isCorrect) stringResource(Res.string.quiz_result_correct)
                        else stringResource(Res.string.quiz_result_wrong),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(Res.string.quiz_points_earned, points),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (rank > 0) {
                            Text(
                                text = stringResource(Res.string.quiz_rank_display, rank),
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        if (streak > 1) {
                            Text(
                                text = stringResource(Res.string.quiz_streak_display, streak),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        item(key = "scoreboard") {
            QuizScoreboard(
                topPlayers = top5,
                highlightNickname = state.player?.nickname
            )
        }
    }
}

@Composable
private fun FinishedContent(
    state: QuizPlayerUiState,
    onExit: () -> Unit
) {
    val podium = state.session?.podium ?: emptyList()
    val player = state.player

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

        if (player != null) {
            item(key = "player_final_card") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDefault,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${player.nickname} — ${stringResource(Res.string.quiz_rank_display, player.rank)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Toplam Puan: ${player.score} • Doğru: ${player.correctCount}",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        item(key = "exit_button") {
            Button(
                onClick = onExit,
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
                    text = stringResource(Res.string.quiz_exit_button),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CancelledContent(
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceDefault,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "⚠️", fontSize = 40.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.quiz_session_cancelled_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(Res.string.quiz_session_cancelled_message),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.back),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
