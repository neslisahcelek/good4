package com.good4.quiz.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.QuizStage
import com.good4.core.presentation.QuizStageAccent
import com.good4.core.presentation.QuizStageInk

/** Shared measurements of the quiz stage, so screens do not repeat their own numbers. */
object QuizStageDefaults {
    val ScreenPadding = 20.dp
    val CornerLarge = 22.dp
    val CornerMedium = 16.dp
    val ButtonHeight = 58.dp
    val ButtonDepth = 5.dp
    val PressScale = 0.95f
}

internal fun Color.darker(fraction: Float = 0.28f): Color = lerp(this, Color.Black, fraction)

/**
 * Full-screen game stage: a flat dark scene with the four answer symbols drawn faintly in the
 * corners. No gradients; the colour carries the mood (stage, correct, wrong).
 */
@Composable
fun QuizStageBackground(
    modifier: Modifier = Modifier,
    color: Color = QuizStage,
    showShapes: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color)
    ) {
        if (showShapes) {
            val faint = Color.White.copy(alpha = 0.06f)
            QuizShapeIcon(QuizAnswerShape.TRIANGLE, Modifier.size(150.dp).offset(x = (-46).dp, y = 36.dp).rotate(-14f), faint)
            QuizShapeIcon(QuizAnswerShape.CIRCLE, Modifier.size(120.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = (-24).dp), faint)
            QuizShapeIcon(QuizAnswerShape.DIAMOND, Modifier.size(132.dp).align(Alignment.BottomStart).offset(x = (-36).dp, y = 30.dp), faint)
            QuizShapeIcon(QuizAnswerShape.SQUARE, Modifier.size(112.dp).align(Alignment.BottomEnd).offset(x = 30.dp, y = (-90).dp).rotate(16f), faint)
        }
        content()
    }
}

/** Raised game button with a darker base edge; the face sinks into it while pressed. */
@Composable
fun QuizChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = QuizStageAccent,
    contentColor: Color = QuizStageInk,
    height: Dp = QuizStageDefaults.ButtonHeight
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = QuizStageDefaults.ButtonDepth
    val sink by animateFloatAsState(if (pressed && enabled) 1f else 0f, tween(90), label = "button_sink")
    val shape = RoundedCornerShape(QuizStageDefaults.CornerMedium)
    // Disabled buttons turn into a quiet outline-free slab instead of a muddy tint of the accent.
    val face = if (enabled) containerColor else Color.White.copy(alpha = 0.14f)
    val label = if (enabled) contentColor else Color.White.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + depth)
            .semantics { role = Role.Button }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled && !loading, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .align(Alignment.BottomCenter)
                .clip(shape)
                .background(if (enabled) face.darker() else Color.Transparent)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = depth * sink)
                .clip(shape)
                .background(face),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = label, strokeWidth = 3.dp)
            } else {
                Text(
                    text = text,
                    color = label,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }
    }
}

/** Circular time badge: the ring drains as the question runs out, colour shifts near the end. */
@Composable
fun QuizTimerBadge(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val progress = if (totalSeconds > 0) (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(progress, tween(300, easing = LinearEasing), label = "timer_ring")
    val ringColor = when {
        progress > 0.45f -> QuizStageAccent
        progress > 0.2f -> com.good4.core.presentation.QuizAnswerYellow
        else -> com.good4.core.presentation.QuizAnswerRed
    }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawCircle(QuizStageInk.copy(alpha = 0.55f), radius = this.size.minDimension / 2f)
            drawArc(Color.White.copy(alpha = 0.15f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(ringColor, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(
            text = "${remainingSeconds.coerceAtLeast(0)}",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.38f).sp
        )
    }
}

/** Number that counts up from zero once it appears, used for points won in a round. */
@Composable
fun QuizCountUpText(
    target: Int,
    format: @Composable (Int) -> String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 44.sp
) {
    val value = remember(target) { Animatable(0f) }
    LaunchedEffect(target) {
        value.animateTo(target.toFloat(), tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }
    Text(
        text = format(value.value.toInt()),
        modifier = modifier,
        color = color,
        fontWeight = FontWeight.Black,
        fontSize = fontSize,
        textAlign = TextAlign.Center
    )
}

/** Centred status scene (cancelled, removed, waiting): big symbol, title, message and one action. */
@Composable
fun QuizStatusScene(
    symbol: @Composable () -> Unit,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(QuizStageDefaults.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        symbol()
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
        footer?.let {
            Spacer(Modifier.height(24.dp))
            it()
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(36.dp))
            QuizChunkyButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Round symbol disc used by status scenes and result banners. */
@Composable
fun QuizSymbolDisc(
    text: String,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    contentColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.5f).sp
        )
    }
}

/** Small translucent pill on the stage (question counter, rank, streak). */
@Composable
fun QuizStagePill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Color.White.copy(alpha = 0.14f),
    contentColor: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text = text, color = contentColor, fontWeight = FontWeight.Bold, fontSize = fontSize, modifier = Modifier.alpha(1f))
    }
}
