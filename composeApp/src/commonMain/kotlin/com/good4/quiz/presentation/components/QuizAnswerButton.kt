package com.good4.quiz.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource

@Composable
fun QuizShapeIcon(
    shape: QuizAnswerShape,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (shape) {
            QuizAnswerShape.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(w / 2f, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path, color)
            }
            QuizAnswerShape.DIAMOND -> {
                val path = Path().apply {
                    moveTo(w / 2f, 0f)
                    lineTo(w, h / 2f)
                    lineTo(w / 2f, h)
                    lineTo(0f, h / 2f)
                    close()
                }
                drawPath(path, color)
            }
            QuizAnswerShape.CIRCLE -> {
                drawCircle(color, radius = minOf(w, h) / 2f)
            }
            QuizAnswerShape.SQUARE -> {
                drawRoundRect(color, cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
            }
        }
    }
}

/**
 * Answer tile: flat answer colour on a darker base edge, symbol on the left, bold label that
 * shrinks to fit long answers. The tile sinks into its base on press.
 */
@Composable
fun QuizAnswerButton(
    index: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    isCorrect: Boolean? = null, // null: normal, true: show correct, false: show wrong
    selectionCount: Int? = null, // count of players who chose this during reveal
    dimmed: Boolean = false
) {
    val shape = QuizAnswerShape.fromIndex(index)
    val shapeLabel = stringResource(shape.labelRes)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (isPressed && enabled) QuizStageDefaults.PressScale else 1f,
        spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "press_scale"
    )
    val sink by animateFloatAsState(if (isPressed && enabled) 1f else 0f, tween(80), label = "press_sink")
    val fade by animateFloatAsState(
        when {
            dimmed || isCorrect == false -> 0.32f
            !enabled && !isSelected && isCorrect == null -> 0.6f
            else -> 1f
        },
        tween(220),
        label = "answer_fade"
    )

    val tileShape = RoundedCornerShape(QuizStageDefaults.CornerMedium)
    val depth = QuizStageDefaults.ButtonDepth
    val contentColor = shape.contentColor

    Box(
        modifier = modifier
            .heightIn(min = 84.dp)
            .scale(pressScale)
            .alpha(fade)
            .semantics {
                this.role = Role.Button
                this.contentDescription = "$shapeLabel: $text"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = depth)
                .clip(tileShape)
                .background(shape.color.darker())
        )
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = depth * (1f - sink))
                .padding(top = depth * sink)
                .clip(tileShape)
                .background(shape.color)
                .then(
                    if (isSelected || isCorrect == true) {
                        Modifier.border(BorderStroke(4.dp, Color.White), tileShape)
                    } else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuizShapeIcon(
                shape = shape,
                modifier = Modifier.size(30.dp),
                color = contentColor
            )
            BasicText(
                text = text,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    color = contentColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    lineHeight = 24.sp
                ),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 20.sp, stepSize = 1.sp)
            )
            when {
                selectionCount != null -> Text(
                    text = "$selectionCount",
                    color = contentColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                isSelected || isCorrect == true -> Text(
                    text = "✓",
                    color = contentColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
            }
        }
    }
}
