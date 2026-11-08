package com.good4.quiz.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.QuizBronze
import com.good4.core.presentation.QuizGold
import com.good4.core.presentation.QuizSilver
import com.good4.core.presentation.QuizStageAccent
import com.good4.core.presentation.QuizStageInk
import com.good4.quiz.domain.QuizTopPlayer
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

internal fun quizRankColor(rank: Int): Color = when (rank) {
    1 -> QuizGold
    2 -> QuizSilver
    3 -> QuizBronze
    else -> QuizStageInk.copy(alpha = 0.12f)
}

/**
 * Top-5 leaderboard on a white panel so it reads on both the dark stage and light host screens.
 * Rows slide in one after another; the viewer's own row is highlighted.
 */
@Composable
fun QuizScoreboard(
    topPlayers: List<QuizTopPlayer>,
    modifier: Modifier = Modifier,
    highlightNickname: String? = null,
    /** The viewer's own character; shown only on their row because others' choices are not synced. */
    highlightAvatar: QuizAvatar? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(QuizStageDefaults.CornerLarge))
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.quiz_top5_title),
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = QuizStageInk
        )

        if (topPlayers.isEmpty()) {
            Text(text = "—", color = QuizStageInk.copy(alpha = 0.5f), fontSize = 14.sp)
        } else {
            topPlayers.forEachIndexed { index, player ->
                val isUser = highlightNickname != null && player.nickname.equals(highlightNickname, ignoreCase = true)
                ScoreboardRow(player = player, isUser = isUser, order = index, avatar = highlightAvatar.takeIf { isUser })
            }
        }
    }
}

@Composable
private fun ScoreboardRow(player: QuizTopPlayer, isUser: Boolean, order: Int, avatar: QuizAvatar?) {
    val enter = remember(player.nickname) { Animatable(0f) }
    LaunchedEffect(player.nickname) {
        enter.animateTo(1f, tween(durationMillis = 320, delayMillis = order * 70, easing = FastOutSlowInEasing))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enter.value
                translationX = (1f - enter.value) * 60f
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (isUser) QuizStageAccent else QuizStageInk.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(quizRankColor(player.rank)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${player.rank}",
                color = QuizStageInk,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }
        if (avatar != null) {
            QuizCharacterAvatar(avatar = avatar, size = 34.dp, background = Color.White.copy(alpha = 0.6f))
        }
        Text(
            text = player.nickname,
            modifier = Modifier.weight(1f),
            fontWeight = if (isUser) FontWeight.Black else FontWeight.Bold,
            fontSize = 16.sp,
            color = QuizStageInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(Res.string.quiz_score_points, player.score),
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = QuizStageInk
        )
    }
}
