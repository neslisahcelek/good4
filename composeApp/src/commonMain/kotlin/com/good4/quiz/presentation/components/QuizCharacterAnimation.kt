package com.good4.quiz.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource

/** How a character behaves on screen; each mood maps to an expression, a pose and a short motion. */
enum class QuizCharacterMood { IDLE, WAVE, EXCITED, CONFIDENT, CHEER, SAD, SURPRISED }

@Composable
fun AnimatedQuizCharacter(
    avatar: QuizAvatar,
    mood: QuizCharacterMood,
    modifier: Modifier = Modifier,
    framing: CharacterFraming = CharacterFraming.FULL
) {
    val period = when (mood) {
        QuizCharacterMood.EXCITED -> 560
        QuizCharacterMood.CHEER -> 720
        QuizCharacterMood.WAVE -> 900
        else -> 2200
    }
    val transition = rememberInfiniteTransition(label = "character_${mood.name}")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(period, easing = LinearEasing), RepeatMode.Restart),
        label = "character_cycle"
    )
    // Eyes close for a moment every few seconds so idle characters feel alive.
    val blinkPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3800
                0f at 0
                0f at 3550
                1f at 3600
                1f at 3700
                0f at 3750
            }
        ),
        label = "character_blink"
    )
    // Mood changes pop in with a small spring so reactions read as a reaction.
    val entry = remember(mood) { Animatable(0.82f) }
    LaunchedEffect(mood, avatar) {
        entry.snapTo(0.82f)
        entry.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    val wave = sin(t * 2f * PI.toFloat())
    val hop = abs(sin(t * PI.toFloat()))
    val expression = when (mood) {
        QuizCharacterMood.IDLE -> CharacterExpression.NEUTRAL
        QuizCharacterMood.WAVE -> CharacterExpression.NEUTRAL
        QuizCharacterMood.EXCITED -> CharacterExpression.DETERMINED
        QuizCharacterMood.CONFIDENT -> CharacterExpression.WINK
        QuizCharacterMood.CHEER -> CharacterExpression.HAPPY
        QuizCharacterMood.SAD -> CharacterExpression.SAD
        QuizCharacterMood.SURPRISED -> CharacterExpression.SURPRISED
    }
    val pose = when (mood) {
        QuizCharacterMood.CHEER -> CharacterPose.CHEER
        QuizCharacterMood.WAVE -> CharacterPose.WAVE
        else -> CharacterPose.REST
    }
    val name = stringResource(avatar.character.nameRes)

    QuizCharacterArt(
        avatar = avatar,
        expression = expression,
        pose = pose,
        framing = framing,
        blink = blinkPhase > 0.5f,
        wave = if (pose == CharacterPose.WAVE) t else 0f,
        modifier = modifier
            .semantics { contentDescription = name }
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                val pop = entry.value
                when (mood) {
                    QuizCharacterMood.IDLE, QuizCharacterMood.WAVE -> {
                        translationY = -wave * 2.dp.toPx()
                        scaleX = pop
                        scaleY = pop * (1f + wave * 0.012f)
                    }
                    QuizCharacterMood.EXCITED -> {
                        translationY = -hop * 7.dp.toPx()
                        scaleX = pop * (1f + (1f - hop) * 0.04f)
                        scaleY = pop * (1f - (1f - hop) * 0.04f)
                    }
                    QuizCharacterMood.CONFIDENT -> {
                        rotationZ = wave * 4f
                        scaleX = pop
                        scaleY = pop
                    }
                    QuizCharacterMood.CHEER -> {
                        translationY = -hop * 14.dp.toPx()
                        rotationZ = wave * 5f
                        scaleX = pop * (1f + (1f - hop) * 0.06f)
                        scaleY = pop * (1f - (1f - hop) * 0.06f)
                    }
                    QuizCharacterMood.SAD -> {
                        translationY = 3.dp.toPx()
                        rotationZ = -5f + wave * 2f
                        scaleX = pop
                        scaleY = pop * 0.97f
                    }
                    QuizCharacterMood.SURPRISED -> {
                        scaleX = pop * 1.02f
                        scaleY = pop * 1.04f
                    }
                }
            }
    )
}

/** Round head-only avatar used in lists, headers and the scoreboard. */
@Composable
fun QuizCharacterAvatar(
    avatar: QuizAvatar,
    size: Dp,
    modifier: Modifier = Modifier,
    background: Color = Color.White.copy(alpha = 0.16f),
    expression: CharacterExpression = CharacterExpression.NEUTRAL
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        QuizCharacterArt(
            avatar = avatar,
            expression = expression,
            framing = CharacterFraming.HEAD,
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.06f)
        )
    }
}
