package com.good4.quiz.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.QuizStageAccent
import com.good4.core.presentation.QuizStageInk
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private const val PICKER_COLUMNS = 5

/** "Karakter" / "Aksesuar" tabs over a grid; tapping a cell changes the avatar immediately. */
@Composable
fun QuizCharacterPicker(
    selected: QuizAvatar,
    onChange: (QuizAvatar) -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(4.dp)
        ) {
            PickerTab(stringResource(Res.string.quiz_picker_tab_character), tab == 0, Modifier.weight(1f)) { tab = 0 }
            PickerTab(stringResource(Res.string.quiz_picker_tab_accessory), tab == 1, Modifier.weight(1f)) { tab = 1 }
        }
        if (tab == 0) {
            PickerGrid(items = QuizCharacter.entries) { character ->
                PickerCell(
                    label = stringResource(character.nameRes),
                    isSelected = character == selected.character,
                    onClick = { onChange(selected.copy(character = character)) }
                ) {
                    QuizCharacterArt(
                        avatar = QuizAvatar(character, selected.accessory),
                        framing = CharacterFraming.HEAD,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            PickerGrid(items = QuizAccessory.entries) { accessory ->
                PickerCell(
                    label = stringResource(accessory.nameRes),
                    isSelected = accessory == selected.accessory,
                    onClick = { onChange(selected.copy(accessory = accessory)) }
                ) {
                    QuizCharacterArt(
                        avatar = selected.copy(accessory = accessory),
                        framing = CharacterFraming.HEAD,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerTab(text: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (active) QuizStageInk else Color.White.copy(alpha = 0.75f),
            fontWeight = FontWeight.Black,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun <T> PickerGrid(items: List<T>, cell: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(PICKER_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item -> Box(Modifier.weight(1f)) { cell(item) } }
                repeat(PICKER_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PickerCell(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    art: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        if (isSelected) 1.08f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "picker_cell_scale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics {
            role = Role.Button
            selected = isSelected
            contentDescription = label
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) Color.White.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.1f))
                .then(if (isSelected) Modifier.border(3.dp, QuizStageAccent, RoundedCornerShape(16.dp)) else Modifier)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            art()
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
