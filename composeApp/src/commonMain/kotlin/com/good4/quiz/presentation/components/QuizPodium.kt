package com.good4.quiz.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.AccentYellow
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.quiz.domain.QuizTopPlayer
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

private data class ConfettiParticle(
    val xRatio: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val rotationOffset: Float
)

@Composable
fun QuizConfetti(
    modifier: Modifier = Modifier
) {
    val particles = remember {
        val colors = listOf(
            Color(0xFFFFD700), Color(0xFF1A73E8), Color(0xFF008556),
            Color(0xFFD6483B), Color(0xFFA7D80A), Color(0xFFE59AC0)
        )
        val rnd = Random(42)
        List(35) {
            ConfettiParticle(
                xRatio = rnd.nextFloat(),
                speed = rnd.nextFloat() * 0.7f + 0.3f,
                size = rnd.nextFloat() * 8f + 6f,
                color = colors[rnd.nextInt(colors.size)],
                rotationOffset = rnd.nextFloat() * 360f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti_anim")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val y = ((progress * p.speed + p.xRatio) % 1f) * h
            val x = (p.xRatio * w) + kotlin.math.sin((progress * 4f + p.rotationOffset) * 3.14f / 180f) * 20f
            drawRect(
                color = p.color,
                topLeft = Offset(x, y),
                size = Size(p.size, p.size * 0.6f)
            )
        }
    }
}

@Composable
fun QuizPodium(
    podium: List<QuizTopPlayer>,
    modifier: Modifier = Modifier
) {
    val first = podium.firstOrNull { it.rank == 1 } ?: podium.getOrNull(0)
    val second = podium.firstOrNull { it.rank == 2 } ?: podium.getOrNull(1)
    val third = podium.firstOrNull { it.rank == 3 } ?: podium.getOrNull(2)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceDefault,
        shadowElevation = 3.dp
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            QuizConfetti(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏆 " + stringResource(Res.string.quiz_podium_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 2nd Place (Silver)
                    PodiumStep(
                        player = second,
                        place = 2,
                        pillarHeight = 110,
                        badgeColor = Color(0xFFC0C0C0),
                        medal = "🥈",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 1st Place (Gold)
                    PodiumStep(
                        player = first,
                        place = 1,
                        pillarHeight = 150,
                        badgeColor = Color(0xFFFFD700),
                        medal = "👑",
                        modifier = Modifier.weight(1.1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 3rd Place (Bronze)
                    PodiumStep(
                        player = third,
                        place = 3,
                        pillarHeight = 80,
                        badgeColor = Color(0xFFCD7F32),
                        medal = "🥉",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumStep(
    player: QuizTopPlayer?,
    place: Int,
    pillarHeight: Int,
    badgeColor: Color,
    medal: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(text = medal, fontSize = 24.sp)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = player?.nickname ?: "—",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (player != null) "${player.score} P" else "",
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pillarHeight.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$place",
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = if (place <= 2) Color(0xFF1D1D1B) else Color.White
            )
        }
    }
}
