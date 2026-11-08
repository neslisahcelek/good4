package com.good4.quiz.presentation.components

import androidx.compose.ui.graphics.Color
import com.good4.core.presentation.QuizAnswerBlue
import com.good4.core.presentation.QuizAnswerGreen
import com.good4.core.presentation.QuizAnswerRed
import com.good4.core.presentation.QuizAnswerYellow
import com.good4.core.presentation.QuizStageInk
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource

/** Fixed colour + symbol pair for each answer slot; the symbol keeps answers distinguishable without colour. */
enum class QuizAnswerShape(
    val index: Int,
    val color: Color,
    val contentColor: Color,
    val labelRes: StringResource
) {
    TRIANGLE(0, QuizAnswerRed, Color.White, Res.string.quiz_shape_triangle),
    DIAMOND(1, QuizAnswerBlue, Color.White, Res.string.quiz_shape_diamond),
    // Dark text on yellow: white would fall below readable contrast.
    CIRCLE(2, QuizAnswerYellow, QuizStageInk, Res.string.quiz_shape_circle),
    SQUARE(3, QuizAnswerGreen, Color.White, Res.string.quiz_shape_square);

    companion object {
        fun fromIndex(index: Int): QuizAnswerShape = when (index) {
            0 -> TRIANGLE
            1 -> DIAMOND
            2 -> CIRCLE
            3 -> SQUARE
            else -> TRIANGLE
        }
    }
}
