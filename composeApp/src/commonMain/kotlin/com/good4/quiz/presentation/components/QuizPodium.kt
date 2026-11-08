package com.good4.quiz.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.QuizAnswerBlue
import com.good4.core.presentation.QuizAnswerGreen
import com.good4.core.presentation.QuizAnswerRed
import com.good4.core.presentation.QuizAnswerYellow
import com.good4.core.presentation.QuizBronze
import com.good4.core.presentation.QuizGold
import com.good4.core.presentation.QuizSilver
import com.good4.core.presentation.QuizStage
import com.good4.core.presentation.QuizStageAccent
import com.good4.core.presentation.QuizStageInk
import com.good4.quiz.domain.QuizTopPlayer
import good4.composeapp.generated.resources.*
import kotlin.math.sin
import kotlin.random.Random
import org.jetbrains.compose.resources.stringResource

private data class ConfettiParticle(
    val xRatio: Float,
    val startRatio: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val spin: Float
)

/** Confetti that falls for a few seconds and then settles off-screen; it does not loop forever. */
@Composable
fun QuizConfetti(
    modifier: Modifier = Modifier,
    durationMillis: Int = 4200
) {
    val particles = remember {
        val colors = listOf(QuizGold, QuizAnswerBlue, QuizAnswerGreen, QuizAnswerRed, QuizAnswerYellow, QuizStageAccent, Color.White)
        val rnd = Random(42)
        List(60) {
            ConfettiParticle(
                xRatio = rnd.nextFloat(),
                startRatio = -rnd.nextFloat() * 0.8f,
                speed = rnd.nextFloat() * 0.6f + 0.7f,
                size = rnd.nextFloat() * 7f + 6f,
                color = colors[rnd.nextInt(colors.size)],
                spin = rnd.nextFloat() * 720f - 360f
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis, easing = LinearEasing))
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val t = progress.value
        particles.forEach { p ->
            val y = (p.startRatio + t * p.speed * 1.6f) * h
            if (y < -20f || y > h + 20f) return@forEach
            val x = p.xRatio * w + sin((t * 10f + p.xRatio * 6f)) * 14f
            rotate(degrees = p.spin * t, pivot = Offset(x, y)) {
                drawRect(color = p.color, topLeft = Offset(x, y), size = Size(p.size, p.size * 0.55f))
            }
        }
    }
}

/**
 * Final podium: a self-contained dark scene (so it also works on light host screens).
 * Steps rise one after another — third, second, then the winner — and names drop in on top.
 */
@Composable
fun QuizPodium(
    podium: List<QuizTopPlayer>,
    modifier: Modifier = Modifier,
    /** Character for a podium entry when it is actually known; null keeps the initial badge. */
    avatarFor: (QuizTopPlayer) -> QuizAvatar? = { null }
) {
    val first = podium.firstOrNull { it.rank == 1 } ?: podium.getOrNull(0)
    val second = podium.firstOrNull { it.rank == 2 } ?: podium.getOrNull(1)
    val third = podium.firstOrNull { it.rank == 3 } ?: podium.getOrNull(2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(QuizStageDefaults.CornerLarge))
            .background(QuizStage)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.quiz_podium_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                PodiumStep(second, second?.let(avatarFor), place = 2, stepHeight = 110.dp, color = QuizSilver, delayMillis = 350, modifier = Modifier.weight(1f))
                PodiumStep(first, first?.let(avatarFor), place = 1, stepHeight = 150.dp, color = QuizGold, delayMillis = 700, modifier = Modifier.weight(1.15f))
                PodiumStep(third, third?.let(avatarFor), place = 3, stepHeight = 80.dp, color = QuizBronze, delayMillis = 0, modifier = Modifier.weight(1f))
            }
        }
        QuizConfetti(modifier = Modifier.matchParentSize())
    }
}

@Composable
private fun PodiumStep(
    player: QuizTopPlayer?,
    avatar: QuizAvatar?,
    place: Int,
    stepHeight: Dp,
    color: Color,
    delayMillis: Int,
    modifier: Modifier = Modifier
) {
    val rise = remember { Animatable(0f) }
    val label = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        rise.animateTo(1f, tween(durationMillis = 520, delayMillis = delayMillis, easing = FastOutSlowInEasing))
        label.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    val empty = player == null

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = label.value
                translationY = (1f - label.value) * -40f
                scaleX = 0.6f + 0.4f * label.value
                scaleY = 0.6f + 0.4f * label.value
            }
        ) {
            if (avatar != null) {
                // Each place celebrates differently: the winner cheers, second waves, third bounces.
                AnimatedQuizCharacter(
                    avatar = if (place == 1 && avatar.accessory == QuizAccessory.NONE) avatar.copy(accessory = QuizAccessory.CROWN) else avatar,
                    mood = when (place) {
                        1 -> QuizCharacterMood.CHEER
                        2 -> QuizCharacterMood.WAVE
                        else -> QuizCharacterMood.EXCITED
                    },
                    modifier = Modifier.height(if (place == 1) 112.dp else 84.dp)
                )
            } else {
                if (place == 1 && !empty) {
                    Text(text = "👑", fontSize = 26.sp)
                }
                Box(
                    modifier = Modifier
                        .size(if (place == 1) 52.dp else 44.dp)
                        .clip(CircleShape)
                        .background(if (empty) Color.White.copy(alpha = 0.12f) else color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player?.nickname?.firstOrNull()?.uppercase() ?: "–",
                        color = QuizStageInk,
                        fontWeight = FontWeight.Black,
                        fontSize = if (place == 1) 24.sp else 20.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = player?.nickname ?: "—",
                fontWeight = FontWeight.Black,
                fontSize = if (place == 1) 16.sp else 14.sp,
                color = Color.White.copy(alpha = if (empty) 0.4f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (player != null) {
                Text(
                    text = stringResource(Res.string.quiz_score_points, player.score),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stepHeight * rise.value)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(if (empty) Color.White.copy(alpha = 0.10f) else color),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "$place",
                modifier = Modifier.padding(top = 10.dp),
                fontWeight = FontWeight.Black,
                fontSize = if (place == 1) 44.sp else 34.sp,
                color = if (empty) Color.White.copy(alpha = 0.4f) else QuizStageInk.copy(alpha = 0.85f)
            )
        }
    }
}
