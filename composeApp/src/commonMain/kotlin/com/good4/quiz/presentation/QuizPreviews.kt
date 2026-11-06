package com.good4.quiz.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.Good4Theme
import com.good4.quiz.domain.QuizTopPlayer
import com.good4.quiz.presentation.components.QuizAnswerButton
import com.good4.quiz.presentation.components.QuizCountdownBar
import com.good4.quiz.presentation.components.QuizPodium
import com.good4.quiz.presentation.components.QuizScoreboard
import com.good4.quiz.presentation.join.LiveQuizCard
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun LiveQuizCardPreview() = Good4Theme {
    Column(modifier = Modifier.padding(16.dp)) {
        LiveQuizCard(
            quizTitle = "Hacettepe Genel Kültür Quiz #3",
            communityName = "Mühendislik Topluluğu",
            onJoinClick = {}
        )
    }
}

@Preview
@Composable
private fun QuizAnswerButtonsPreview() = Good4Theme {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuizAnswerButton(
                index = 0,
                text = "Kırmızı Şık (Üçgen)",
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            QuizAnswerButton(
                index = 1,
                text = "Mavi Şık (Elmas)",
                onClick = {},
                isSelected = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuizAnswerButton(
                index = 2,
                text = "Sarı Şık (Daire)",
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            QuizAnswerButton(
                index = 3,
                text = "Yeşil Şık (Kare)",
                onClick = {},
                isCorrect = true,
                selectionCount = 18,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview
@Composable
private fun QuizCountdownBarPreview() = Good4Theme {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuizCountdownBar(remainingSeconds = 18, totalSeconds = 20)
        QuizCountdownBar(remainingSeconds = 6, totalSeconds = 20)
        QuizCountdownBar(remainingSeconds = 2, totalSeconds = 20)
    }
}

@Preview
@Composable
private fun QuizScoreboardPreview() = Good4Theme {
    Column(modifier = Modifier.padding(16.dp)) {
        QuizScoreboard(
            topPlayers = listOf(
                QuizTopPlayer("Ahmet", score = 3840, rank = 1),
                QuizTopPlayer("Zeynep", score = 3420, rank = 2),
                QuizTopPlayer("Mehmet", score = 2950, rank = 3),
                QuizTopPlayer("Elif", score = 2400, rank = 4),
                QuizTopPlayer("Can", score = 1980, rank = 5)
            ),
            highlightNickname = "Zeynep"
        )
    }
}

@Preview
@Composable
private fun QuizPodiumPreview() = Good4Theme {
    Column(modifier = Modifier.padding(16.dp)) {
        QuizPodium(
            podium = listOf(
                QuizTopPlayer("Ahmet", score = 9240, rank = 1),
                QuizTopPlayer("Zeynep", score = 8650, rank = 2),
                QuizTopPlayer("Mehmet", score = 7800, rank = 3)
            )
        )
    }
}
