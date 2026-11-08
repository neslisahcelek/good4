package com.good4.quiz.presentation.play

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.QuizCorrectScene
import com.good4.core.presentation.QuizStage
import com.good4.core.presentation.QuizStageAccent
import com.good4.core.presentation.QuizStageInk
import com.good4.core.presentation.QuizWrongScene
import com.good4.core.presentation.components.Good4ConfirmDialog
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.quiz.domain.QuizQuestion
import com.good4.quiz.presentation.components.AnimatedQuizCharacter
import com.good4.quiz.presentation.components.QuizAvatar
import com.good4.quiz.presentation.components.QuizCharacter
import com.good4.quiz.presentation.components.QuizCharacterAvatar
import com.good4.quiz.presentation.components.QuizCharacterMood
import com.good4.quiz.presentation.components.QuizCharacterPicker
import com.good4.quiz.presentation.components.QuizAnswerButton
import com.good4.quiz.presentation.components.QuizAnswerShape
import com.good4.quiz.presentation.components.QuizChunkyButton
import com.good4.quiz.presentation.components.QuizCountUpText
import com.good4.quiz.presentation.components.QuizPodium
import com.good4.quiz.presentation.components.QuizScoreboard
import com.good4.quiz.presentation.components.QuizShapeIcon
import com.good4.quiz.presentation.components.QuizStageBackground
import com.good4.quiz.presentation.components.QuizStageDefaults
import com.good4.quiz.presentation.components.QuizStagePill
import com.good4.quiz.presentation.components.QuizStatusScene
import com.good4.quiz.presentation.components.QuizSymbolDisc
import com.good4.quiz.presentation.components.QuizTimerBadge
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Events the player screen sends back; the content itself never talks to the ViewModel. */
data class QuizPlayerActions(
    val onNicknameChange: (String) -> Unit = {},
    val onJoin: () -> Unit = {},
    val onSelectOption: (Int) -> Unit = {},
    val onBackupReveal: () -> Unit = {},
    val onExit: () -> Unit = {}
)

@Composable
fun QuizPlayerScreen(
    viewModel: QuizPlayerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiMessage = state.uiMessage?.asString()

    var showExitConfirmDialog by remember { mutableStateOf(false) }
    // Chosen once (random) and kept across recompositions and process restore. It is local to
    // this device until the session data carries an avatar id.
    var avatar by rememberSaveable(stateSaver = QuizAvatar.Saver) {
        mutableStateOf(QuizAvatar(QuizCharacter.entries.random()))
    }

    val isGameActive = state.phase == PlayerScreenPhase.GET_READY ||
        state.phase == PlayerScreenPhase.QUESTION ||
        state.phase == PlayerScreenPhase.SUBMITTED
    val onBack = {
        if (isGameActive) showExitConfirmDialog = true else onBackClick()
    }
    com.good4.community.CommunityBackHandler { onBack() }

    if (showExitConfirmDialog) {
        Good4ConfirmDialog(
            title = stringResource(Res.string.quiz_exit_dialog_title),
            message = stringResource(Res.string.quiz_exit_dialog_desc),
            confirmLabel = stringResource(Res.string.quiz_exit_button),
            dismissLabel = stringResource(Res.string.quiz_stay_in_game),
            confirmColor = ErrorRed,
            onConfirm = {
                showExitConfirmDialog = false
                onBackClick()
            },
            onDismiss = { showExitConfirmDialog = false }
        )
    }

    LaunchedEffect(uiMessage) {
        if (uiMessage != null) {
            snackbarHostState.showSnackbar(uiMessage)
            viewModel.clearUiMessage()
        }
    }

    QuizPlayerContent(
        state = state,
        actions = QuizPlayerActions(
            onNicknameChange = viewModel::onNicknameChange,
            onJoin = viewModel::joinSession,
            onSelectOption = viewModel::submitAnswer,
            onBackupReveal = viewModel::triggerBackupReveal,
            onExit = onBackClick
        ),
        onBack = onBack,
        avatar = avatar,
        onAvatarChange = { avatar = it },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

private enum class RevealOutcome { CORRECT, WRONG, MISSED }

private fun QuizPlayerUiState.revealOutcome(): RevealOutcome {
    val result = player?.lastResult
    val revealIndex = session?.reveal?.index ?: session?.currentIndex
    return when {
        result == null || result.index != revealIndex -> RevealOutcome.MISSED
        result.correct -> RevealOutcome.CORRECT
        else -> RevealOutcome.WRONG
    }
}

/** Stateless player screen: renders [state] as a full-screen game stage. */
@Composable
fun QuizPlayerContent(
    state: QuizPlayerUiState,
    actions: QuizPlayerActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    avatar: QuizAvatar = QuizAvatar(QuizCharacter.FOX),
    onAvatarChange: (QuizAvatar) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val stageColor by animateColorAsState(
        targetValue = if (state.phase == PlayerScreenPhase.REVEAL) {
            when (state.revealOutcome()) {
                RevealOutcome.CORRECT -> QuizCorrectScene
                RevealOutcome.WRONG -> QuizWrongScene
                RevealOutcome.MISSED -> QuizStage
            }
        } else QuizStage,
        animationSpec = tween(400),
        label = "stage_color"
    )

    Good4Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        QuizStageBackground(
            color = stageColor,
            showShapes = state.phase != PlayerScreenPhase.QUESTION
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                QuizStageTopBar(
                    title = state.session?.title?.ifBlank { null } ?: stringResource(Res.string.quiz_title),
                    onBack = onBack
                )
                AnimatedContent(
                    targetState = state.phase,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        (fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.94f)) togetherWith
                            fadeOut(tween(160))
                    },
                    label = "player_phase"
                ) { phase ->
                    when (phase) {
                        PlayerScreenPhase.ENTER_NICKNAME -> EnterNicknameContent(
                            nickname = state.nicknameInput,
                            error = state.nicknameError?.asString(),
                            isLoading = state.isJoining,
                            avatar = avatar,
                            onAvatarChange = onAvatarChange,
                            onNicknameChange = actions.onNicknameChange,
                            onSubmit = actions.onJoin
                        )
                        PlayerScreenPhase.LOBBY -> LobbyWaitingContent(
                            nickname = state.player?.nickname ?: state.nicknameInput,
                            avatar = avatar
                        )
                        PlayerScreenPhase.GET_READY -> GetReadyContent(
                            avatar = avatar,
                            remainingSeconds = state.remainingSeconds,
                            questionIndex = (state.session?.currentIndex ?: 0) + 1,
                            totalQuestions = state.session?.totalQuestions ?: 0
                        )
                        PlayerScreenPhase.QUESTION -> QuestionActiveContent(
                            state = state,
                            avatar = avatar,
                            onSelectOption = actions.onSelectOption
                        )
                        PlayerScreenPhase.SUBMITTED -> AnswerSubmittedContent(
                            state = state,
                            avatar = avatar,
                            onBackupReveal = actions.onBackupReveal
                        )
                        PlayerScreenPhase.REVEAL -> RevealContent(state = state, avatar = avatar)
                        PlayerScreenPhase.FINISHED -> FinishedContent(state = state, avatar = avatar, onExit = actions.onExit)
                        PlayerScreenPhase.CANCELLED -> QuizStatusScene(
                            symbol = { QuizSymbolDisc(text = "!", background = Color.White, contentColor = QuizStage) },
                            title = stringResource(Res.string.quiz_session_cancelled_title),
                            message = stringResource(Res.string.quiz_session_cancelled_message),
                            actionLabel = stringResource(Res.string.back),
                            onAction = actions.onExit
                        )
                        PlayerScreenPhase.KICKED -> QuizStatusScene(
                            symbol = { QuizSymbolDisc(text = "✕", background = QuizWrongScene) },
                            title = stringResource(Res.string.quiz_kicked_title),
                            message = stringResource(Res.string.quiz_kicked_desc),
                            actionLabel = stringResource(Res.string.back),
                            onAction = actions.onExit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizStageTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.back),
                tint = Color.White
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StageColumn(
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = QuizStageDefaults.ScreenPadding, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun EnterNicknameContent(
    nickname: String,
    error: String?,
    isLoading: Boolean,
    avatar: QuizAvatar,
    onAvatarChange: (QuizAvatar) -> Unit,
    onNicknameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val canSubmit = !isLoading && nickname.trim().length >= 2
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = QuizStageDefaults.ScreenPadding, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Big preview; it bounces on every pick so the choice feels immediate.
        key(avatar.key) {
            AnimatedQuizCharacter(
                avatar = avatar,
                mood = QuizCharacterMood.WAVE,
                modifier = Modifier.height(150.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(avatar.character.nameRes),
            color = QuizStageAccent,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(14.dp))
        BasicTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            singleLine = true,
            textStyle = TextStyle(
                color = QuizStageInk,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(QuizStageInk),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSubmit() }),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .clip(RoundedCornerShape(QuizStageDefaults.CornerMedium))
                        .background(Color.White)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (nickname.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.quiz_nickname_placeholder),
                            color = QuizStageInk.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            QuizStagePill(text = error, background = QuizWrongScene, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        QuizCharacterPicker(selected = avatar, onChange = onAvatarChange)
        Spacer(Modifier.height(18.dp))
        QuizChunkyButton(
            text = stringResource(Res.string.quiz_join_with_character),
            onClick = onSubmit,
            enabled = canSubmit,
            loading = isLoading
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** The four answer symbols in a row; [bounce] makes them hop one after another while waiting. */
@Composable
private fun ShapeRow(size: Int, bounce: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "shape_row")
    Row(horizontalArrangement = Arrangement.spacedBy((size / 2).dp)) {
        QuizAnswerShape.entries.forEach { shape ->
            val hop by if (bounce) {
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1600, delayMillis = 0, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = androidx.compose.animation.core.StartOffset(shape.index * 180)
                    ),
                    label = "hop_${shape.name}"
                )
            } else remember { mutableStateOf(0f) }
            val lift = if (hop < 0.3f) kotlin.math.sin(hop / 0.3f * kotlin.math.PI).toFloat() else 0f
            Box(
                modifier = Modifier
                    .size((size + 16).dp)
                    .graphicsLayer { translationY = -lift * size * 0.6f }
                    .clip(RoundedCornerShape(10.dp))
                    .background(shape.color),
                contentAlignment = Alignment.Center
            ) {
                QuizShapeIcon(shape = shape, modifier = Modifier.size((size * 0.6f).dp), color = shape.contentColor)
            }
        }
    }
}

@Composable
private fun LobbyWaitingContent(nickname: String, avatar: QuizAvatar) {
    StageColumn(scrollable = true) {
        Spacer(Modifier.weight(1f))
        AnimatedQuizCharacter(
            avatar = avatar,
            mood = QuizCharacterMood.WAVE,
            modifier = Modifier.height(200.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(Res.string.quiz_lobby_in_game),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 40.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(QuizStageDefaults.CornerMedium))
                .background(Color.White)
                .padding(horizontal = 26.dp, vertical = 12.dp)
        ) {
            Text(
                text = nickname,
                color = QuizStageInk,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.quiz_lobby_watch_screen),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.quiz_lobby_waiting_host),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        ShapeRow(size = 18, bounce = true)
        Spacer(Modifier.weight(1.2f))
    }
}

@Composable
private fun GetReadyContent(
    avatar: QuizAvatar,
    remainingSeconds: Int,
    questionIndex: Int,
    totalQuestions: Int
) {
    StageColumn {
        QuizStagePill(text = stringResource(Res.string.quiz_question_counter, questionIndex, totalQuestions))
        Spacer(Modifier.weight(1f))
        AnimatedContent(
            targetState = remainingSeconds.coerceAtLeast(1),
            transitionSpec = {
                (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 1.8f) + fadeIn(tween(150))) togetherWith
                    (scaleOut(tween(180), targetScale = 0.5f) + fadeOut(tween(180)))
            },
            label = "get_ready_number"
        ) { number ->
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(QuizStageAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$number",
                    color = QuizStageInk,
                    fontWeight = FontWeight.Black,
                    fontSize = 112.sp
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(Res.string.quiz_get_ready_title),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 34.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.quiz_get_ready_subtitle),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 17.sp
        )
        Spacer(Modifier.weight(1f))
        AnimatedQuizCharacter(
            avatar = avatar,
            mood = QuizCharacterMood.EXCITED,
            modifier = Modifier.height(110.dp)
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** Option label as the player sees it; true/false slots are stored as fixed keys on the server. */
@Composable
private fun QuizQuestion.optionLabel(index: Int): String = if (isTrueFalse) {
    if (index == 0) stringResource(Res.string.quiz_true) else stringResource(Res.string.quiz_false)
} else options.getOrElse(index) { "" }

@Composable
private fun QuestionActiveContent(
    state: QuizPlayerUiState,
    avatar: QuizAvatar,
    onSelectOption: (Int) -> Unit
) {
    val session = state.session
    val question = session?.currentQuestion ?: return
    val currentIndex = (session.currentIndex + 1).coerceAtLeast(1)
    val optionCount = if (question.isTrueFalse) 2 else question.options.size.coerceIn(2, 4)
    // Long answers get a full-width row each so they stay readable on narrow phones.
    val singleColumn = !question.isTrueFalse && question.options.any { it.length > 30 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuizCharacterAvatar(avatar = avatar, size = 44.dp)
            Spacer(Modifier.width(8.dp))
            QuizStagePill(text = stringResource(Res.string.quiz_question_counter, currentIndex, session.totalQuestions))
            Spacer(Modifier.weight(1f))
            QuizTimerBadge(remainingSeconds = state.remainingSeconds, totalSeconds = state.totalSeconds)
        }
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.9f),
            contentAlignment = Alignment.Center
        ) {
            // A bounded height lets the question text shrink instead of being cut off.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .clip(RoundedCornerShape(QuizStageDefaults.CornerLarge))
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Size steps by length (server caps questions at 120 characters) so long ones still fit.
                val questionSize = when {
                    question.text.length > 95 -> 19.sp
                    question.text.length > 65 -> 23.sp
                    else -> 28.sp
                }
                Text(
                    text = question.text,
                    color = QuizStageInk,
                    fontWeight = FontWeight.Black,
                    fontSize = questionSize,
                    lineHeight = questionSize * 1.15f,
                    textAlign = TextAlign.Center,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        val rows = if (singleColumn) (0 until optionCount).map { listOf(it) } else (0 until optionCount).chunked(2)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (question.isTrueFalse) 0.75f else 1.25f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { index ->
                        QuizAnswerButton(
                            index = index,
                            text = question.optionLabel(index),
                            onClick = { onSelectOption(index) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            isSelected = state.selectedOption == index,
                            enabled = state.selectedOption == null,
                            dimmed = state.selectedOption != null && state.selectedOption != index
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerSubmittedContent(
    state: QuizPlayerUiState,
    avatar: QuizAvatar,
    onBackupReveal: () -> Unit
) {
    val selected = state.selectedOption
    val question = state.session?.currentQuestion
    StageColumn(scrollable = true) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            QuizTimerBadge(remainingSeconds = state.remainingSeconds, totalSeconds = state.totalSeconds, size = 52.dp)
        }
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.BottomEnd) {
            AnimatedQuizCharacter(
                avatar = avatar,
                mood = QuizCharacterMood.CONFIDENT,
                modifier = Modifier.height(190.dp)
            )
            if (selected != null) {
                // The chosen answer rides on the character as a badge: colour and symbol, no verdict.
                val shape = QuizAnswerShape.fromIndex(selected)
                Box(
                    modifier = Modifier
                        .offset(x = 18.dp, y = (-6).dp)
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(shape.color),
                    contentAlignment = Alignment.Center
                ) {
                    QuizShapeIcon(shape = shape, modifier = Modifier.size(30.dp), color = shape.contentColor)
                }
            }
        }
        if (selected != null && question != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = question.optionLabel(selected),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(Res.string.quiz_answer_submitted),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.quiz_submitted_hint),
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        if (state.canBackupReveal) {
            Spacer(Modifier.height(28.dp))
            QuizChunkyButton(
                text = stringResource(Res.string.quiz_reveal_backup_button),
                onClick = onBackupReveal,
                containerColor = Color.White
            )
        }
        Spacer(Modifier.weight(1.3f))
    }
}

@Composable
private fun RevealContent(state: QuizPlayerUiState, avatar: QuizAvatar) {
    val outcome = state.revealOutcome()
    val player = state.player
    val points = if (outcome == RevealOutcome.CORRECT) player?.lastResult?.points ?: 0 else 0
    val rank = player?.rank ?: 0
    val streak = player?.streak ?: 0
    val question = state.session?.currentQuestion
    val reveal = state.session?.reveal

    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }

    StageColumn(scrollable = true) {
        Spacer(Modifier.height(8.dp))
        Box(contentAlignment = Alignment.TopEnd) {
            AnimatedQuizCharacter(
                avatar = avatar,
                mood = when (outcome) {
                    RevealOutcome.CORRECT -> QuizCharacterMood.CHEER
                    RevealOutcome.WRONG -> QuizCharacterMood.SAD
                    RevealOutcome.MISSED -> QuizCharacterMood.SURPRISED
                },
                modifier = Modifier.height(170.dp)
            )
            QuizSymbolDisc(
                text = when (outcome) {
                    RevealOutcome.CORRECT -> "✓"
                    RevealOutcome.WRONG -> "✕"
                    RevealOutcome.MISSED -> "…"
                },
                background = Color.White,
                contentColor = when (outcome) {
                    RevealOutcome.CORRECT -> QuizCorrectScene
                    RevealOutcome.WRONG -> QuizWrongScene
                    RevealOutcome.MISSED -> QuizStage
                },
                size = 54.dp,
                modifier = Modifier
                    .offset(x = 22.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = when (outcome) {
                RevealOutcome.CORRECT -> stringResource(Res.string.quiz_result_correct)
                RevealOutcome.WRONG -> stringResource(Res.string.quiz_result_wrong)
                RevealOutcome.MISSED -> stringResource(Res.string.quiz_result_missed)
            },
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        if (outcome == RevealOutcome.CORRECT) {
            QuizCountUpText(
                target = points,
                format = { stringResource(Res.string.quiz_points_earned, it) },
                fontSize = 46.sp
            )
        } else if (player != null) {
            Text(
                text = stringResource(Res.string.quiz_points_total, player.score),
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (rank > 0) {
                QuizStagePill(
                    text = stringResource(Res.string.quiz_rank_display, rank),
                    background = QuizStageInk.copy(alpha = 0.35f),
                    fontSize = 16.sp
                )
            }
            if (streak > 1 && outcome == RevealOutcome.CORRECT) {
                QuizStagePill(
                    text = stringResource(Res.string.quiz_streak_display, streak),
                    background = QuizStageInk.copy(alpha = 0.35f),
                    fontSize = 16.sp
                )
            }
        }
        if (outcome != RevealOutcome.CORRECT && question != null && reveal != null) {
            Spacer(Modifier.height(20.dp))
            val correctShape = QuizAnswerShape.fromIndex(reveal.correctIndex)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(QuizStageDefaults.CornerMedium))
                    .background(correctShape.color)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuizShapeIcon(shape = correctShape, modifier = Modifier.size(26.dp), color = correctShape.contentColor)
                Text(
                    text = stringResource(Res.string.quiz_correct_answer_label, question.optionLabel(reveal.correctIndex)),
                    color = correctShape.contentColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        QuizScoreboard(
            topPlayers = reveal?.top5.orEmpty(),
            highlightNickname = player?.nickname,
            highlightAvatar = avatar
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun FinishedContent(
    state: QuizPlayerUiState,
    avatar: QuizAvatar,
    onExit: () -> Unit
) {
    val player = state.player
    StageColumn(scrollable = true) {
        // Only the viewer's own character is known on this device; others keep their initial.
        QuizPodium(
            podium = state.session?.podium.orEmpty(),
            avatarFor = { top -> avatar.takeIf { player != null && top.nickname.equals(player.nickname, ignoreCase = true) } }
        )
        if (player != null) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(QuizStageDefaults.CornerLarge))
                    .background(Color.White)
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FinalStat(value = if (player.rank > 0) "${player.rank}." else "—", label = stringResource(Res.string.quiz_final_rank))
                FinalStat(value = "${player.score}", label = stringResource(Res.string.quiz_final_score))
                FinalStat(value = "${player.correctCount}", label = stringResource(Res.string.quiz_final_correct))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.quiz_finished_message),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(24.dp))
        QuizChunkyButton(text = stringResource(Res.string.quiz_exit_button), onClick = onExit)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FinalStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = QuizStageInk, fontWeight = FontWeight.Black, fontSize = 28.sp)
        Text(text = label, color = QuizStageInk.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
