package com.good4.quiz.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun QuizAnswerButton(
    index: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    isCorrect: Boolean? = null, // null: normal, true: show correct, false: show wrong
    selectionCount: Int? = null // count of players who chose this during reveal
) {
    val shape = QuizAnswerShape.fromIndex(index)
    val shapeLabel = stringResource(shape.labelRes)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && enabled) 0.96f else 1f, label = "press_scale")

    val baseColor = shape.color
    val contentColor = if (shape == QuizAnswerShape.CIRCLE) Color(0xFF1D1D1B) else Color.White

    val border = when {
        isSelected -> BorderStroke(4.dp, Color.White)
        isCorrect == true -> BorderStroke(4.dp, Color(0xFF00FF66))
        isCorrect == false -> BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
        else -> null
    }

    val alpha = if (!enabled && !isSelected && isCorrect == null) 0.65f else 1f

    Surface(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 120.dp)
            .semantics {
                this.role = Role.Button
                this.contentDescription = "$shapeLabel: $text"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = baseColor.copy(alpha = alpha),
        border = border,
        shadowElevation = if (isSelected) 8.dp else 3.dp
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuizShapeIcon(
                    shape = shape,
                    modifier = Modifier.size(28.dp),
                    color = contentColor
                )

                if (selectionCount != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = contentColor.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "$selectionCount",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else if (isSelected) {
                    Text(
                        text = "✓",
                        color = contentColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
