package com.good4.quiz.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.good4.quiz.domain.QuizLastResult
import com.good4.quiz.domain.QuizPlayer
import com.good4.quiz.domain.QuizQuestion
import com.good4.quiz.domain.QuizReveal
import com.good4.quiz.domain.QuizSession
import com.good4.quiz.domain.QuizTopPlayer
import com.good4.quiz.presentation.play.PlayerScreenPhase
import com.good4.quiz.presentation.play.QuizPlayerActions
import com.good4.quiz.presentation.play.QuizPlayerContent
import com.good4.quiz.presentation.play.QuizPlayerUiState
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.good4.quiz.presentation.components.CharacterExpression
import com.good4.quiz.presentation.components.CharacterFraming
import com.good4.quiz.presentation.components.CharacterPose
import com.good4.quiz.presentation.components.QuizAccessory
import com.good4.quiz.presentation.components.QuizAvatar
import com.good4.quiz.presentation.components.QuizCharacter
import com.good4.quiz.presentation.components.QuizCharacterArt
import com.good4.quiz.presentation.components.QuizPodium
import com.good4.quiz.presentation.components.QuizStageBackground

/**
 * Design-only preview of the live quiz with sample data: no sign-in, Koin or network.
 * Debug iOS builds open it with the `-quizPreview <stage>` launch argument.
 */
object QuizUiPreviewData {
    val stages = listOf(
        "nickname", "lobby", "getready", "question4", "questiontf", "questionlong",
        "submitted", "reveal-correct", "reveal-wrong", "reveal-missed", "finished", "cancelled", "kicked",
        "characters", "podium-all"
    )

    /** Sample characters for the design-only podium; real games only know the viewer's own. */
    val sampleAvatars = mapOf(
        "Can" to QuizAvatar(QuizCharacter.FOX, QuizAccessory.SUNGLASSES),
        "Ece" to QuizAvatar(QuizCharacter.PANDA, QuizAccessory.HEADPHONES),
        "Mert" to QuizAvatar(QuizCharacter.DRAGON, QuizAccessory.PARTY_HAT)
    )

    private val top5 = listOf(
        QuizTopPlayer("Can", 1695, 1),
        QuizTopPlayer("Ece", 1540, 2),
        QuizTopPlayer("Mert", 1320, 3),
        QuizTopPlayer("Zeynep", 980, 4),
        QuizTopPlayer("Bora", 610, 5)
    )

    private val question4 = QuizQuestion(
        index = 0,
        type = "multiple_choice",
        text = "Akdeniz Üniversitesi hangi yıl kuruldu?",
        options = listOf("1975", "1982", "1990", "2001"),
        timeLimitSec = 20
    )
    private val questionTf = QuizQuestion(
        index = 1,
        type = "true_false",
        text = "Merkezi Yemekhane cumartesi günleri açıktır.",
        options = listOf("true", "false"),
        timeLimitSec = 10
    )
    private val questionLong = QuizQuestion(
        index = 2,
        type = "multiple_choice",
        text = "Kampüs Dolabı'nda bir ilan verdikten sonra alıcıyla buluşmadan önce hangi adımı tamamlaman önerilir?",
        options = listOf(
            "Uygulama içi mesajlardan buluşma yerini netleştirmek",
            "Ürünün fotoğrafını sosyal medyada paylaşmak",
            "Ödemeyi önceden havale ile almak",
            "İlanı kaldırıp yeniden yayınlamak"
        ),
        timeLimitSec = 30
    )

    private fun session(status: String, question: QuizQuestion?, index: Int, reveal: QuizReveal? = null, podium: List<QuizTopPlayer>? = null) =
        QuizSession(
            id = "preview",
            title = "Kampüs Bilgi Yarışması",
            status = status,
            totalQuestions = 4,
            currentIndex = index,
            currentQuestion = question,
            reveal = reveal,
            podium = podium
        )

    private fun player(score: Int, rank: Int, lastResult: QuizLastResult?, streak: Int = 0, correct: Int = 0) =
        QuizPlayer(userId = "me", nickname = "Can", score = score, rank = rank, streak = streak, correctCount = correct, lastResult = lastResult)

    fun state(stage: String, remainingSeconds: Int): QuizPlayerUiState = when (stage) {
        "nickname" -> QuizPlayerUiState(phase = PlayerScreenPhase.ENTER_NICKNAME, session = session("lobby", null, -1), nicknameInput = "")
        "lobby" -> QuizPlayerUiState(phase = PlayerScreenPhase.LOBBY, session = session("lobby", null, -1), player = player(0, 0, null))
        "getready" -> QuizPlayerUiState(
            phase = PlayerScreenPhase.GET_READY,
            session = session("question", question4, 0),
            remainingSeconds = remainingSeconds,
            totalSeconds = 3
        )
        "question4" -> QuizPlayerUiState(
            phase = PlayerScreenPhase.QUESTION,
            session = session("question", question4, 0),
            remainingSeconds = remainingSeconds,
            totalSeconds = 20
        )
        "questiontf" -> QuizPlayerUiState(
            phase = PlayerScreenPhase.QUESTION,
            session = session("question", questionTf, 1),
            remainingSeconds = remainingSeconds.coerceAtMost(10),
            totalSeconds = 10
        )
        "questionlong" -> QuizPlayerUiState(
            phase = PlayerScreenPhase.QUESTION,
            session = session("question", questionLong, 2),
            remainingSeconds = remainingSeconds,
            totalSeconds = 30
        )
        "submitted" -> QuizPlayerUiState(
            phase = PlayerScreenPhase.SUBMITTED,
            session = session("question", question4, 0),
            selectedOption = 1,
            remainingSeconds = remainingSeconds,
            totalSeconds = 20
        )
        "reveal-correct" -> QuizPlayerUiState(
            phase = PlayerScreenPhase.REVEAL,
            session = session("reveal", question4, 0, QuizReveal(0, 1, listOf(3, 18, 4, 1), 2, top5)),
            player = player(1695, 1, QuizLastResult(0, true, 747, 1), streak = 2),
            selectedOption = 1
        )
        "reveal-wrong" -> QuizPlayerUiState(
            phase = PlayerScreenPhase.REVEAL,
            session = session("reveal", question4, 0, QuizReveal(0, 1, listOf(3, 18, 4, 1), 2, top5)),
            player = player(980, 4, QuizLastResult(0, false, 0, 2)).copy(nickname = "Zeynep"),
            selectedOption = 2
        )
        "reveal-missed" -> QuizPlayerUiState(
            phase = PlayerScreenPhase.REVEAL,
            session = session("reveal", questionTf, 1, QuizReveal(1, 1, listOf(5, 20), 3, top5)),
            // Same shape the server writes for a player who did not answer.
            player = player(610, 5, QuizLastResult(1, false, 0, -1)).copy(nickname = "Bora")
        )
        "finished" -> QuizPlayerUiState(
            phase = PlayerScreenPhase.FINISHED,
            session = session("finished", null, 3, podium = top5.take(3)),
            player = player(1695, 1, null, correct = 3)
        )
        "cancelled" -> QuizPlayerUiState(phase = PlayerScreenPhase.CANCELLED, session = session("cancelled", null, -1))
        "kicked" -> QuizPlayerUiState(phase = PlayerScreenPhase.KICKED, session = session("lobby", null, -1))
        else -> state("question4", remainingSeconds)
    }
}

@Composable
fun QuizUiPreview(stage: String) {
    // Timed stages tick so the countdown animations can be seen; others ignore the value.
    val start = when (stage) {
        "getready" -> 3
        "questiontf" -> 10
        "questionlong" -> 30
        else -> 20
    }
    var remaining by remember(stage) { mutableIntStateOf(start) }
    LaunchedEffect(stage) {
        while (true) {
            delay(1000)
            remaining = if (remaining <= 1) start else remaining - 1
        }
    }
    var nickname by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf(QuizAvatar(QuizCharacter.FOX, QuizAccessory.PARTY_HAT)) }
    when (stage) {
        "characters" -> CharacterSheet()
        "podium-all" -> QuizStageBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                QuizPodium(
                    podium = QuizUiPreviewData.state("finished", 0).session?.podium.orEmpty(),
                    avatarFor = { QuizUiPreviewData.sampleAvatars[it.nickname] }
                )
            }
        }
        else -> {
            val base = QuizUiPreviewData.state(stage, remaining)
            QuizPlayerContent(
                state = if (stage == "nickname") base.copy(nicknameInput = nickname) else base,
                actions = QuizPlayerActions(onNicknameChange = { nickname = it.take(16) }),
                onBack = {},
                avatar = avatar,
                onAvatarChange = { avatar = it }
            )
        }
    }
}

/** Every character in every expression, for reviewing the art in one place. */
@Composable
private fun CharacterSheet() {
    QuizStageBackground(showShapes = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuizCharacter.entries.forEachIndexed { index, character ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                    val accessory = QuizAccessory.entries[index % QuizAccessory.entries.size]
                    QuizCharacterArt(
                        avatar = QuizAvatar(character, accessory),
                        pose = CharacterPose.CHEER,
                        expression = CharacterExpression.HAPPY,
                        modifier = Modifier.height(78.dp)
                    )
                    listOf(
                        CharacterExpression.NEUTRAL,
                        CharacterExpression.WINK,
                        CharacterExpression.SAD,
                        CharacterExpression.SURPRISED,
                        CharacterExpression.DETERMINED
                    ).forEach { expression ->
                        QuizCharacterArt(
                            avatar = QuizAvatar(character),
                            expression = expression,
                            framing = CharacterFraming.HEAD,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
            }
        }
    }
}
