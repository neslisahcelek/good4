package com.good4.community.discover

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.LightGood4Colors
import com.good4.core.presentation.QuizStage
import com.good4.core.presentation.QuizStageRaised
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

internal const val QuizPinLength = 6

private val PinShape = RoundedCornerShape(20.dp)
private val StageInk = LightGood4Colors.textPrimary

/**
 * Kahoot-style entry: a student types the 6-digit game code shown on the host screen. Uses the live quiz
 * stage colours so it reads as "the quiz", and starts collapsed so it never pushes the feed down much.
 */
@Composable
internal fun QuizPinCard(onJoin: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var pin by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PinShape)
            .background(QuizStage)
            .animateContentSize(tween(220))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(DeepGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Dialpad, contentDescription = null, tint = StageInk, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(Res.string.community_discover_pin_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(stringResource(Res.string.community_discover_pin_body), fontSize = 12.sp, lineHeight = 16.sp, color = Color.White.copy(alpha = 0.72f))
            }
            Spacer(Modifier.width(8.dp))
            AnimatedContent(expanded, transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) }, label = "pin_toggle") { open ->
                if (open) {
                    val close = stringResource(Res.string.community_discover_pin_close)
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(QuizStageRaised)
                            .clickable(role = Role.Button) { expanded = false; pin = "" }
                            .semantics { contentDescription = close },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                } else {
                    StagePill(stringResource(Res.string.community_discover_pin_open), enabled = true) { expanded = true }
                }
            }
        }
        if (expanded) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PinBoxes(pin, onPinChange = { pin = it }, onDone = { if (pin.length == QuizPinLength) onJoin(pin) }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                StagePill(
                    stringResource(Res.string.community_discover_pin_join),
                    enabled = pin.length == QuizPinLength,
                    height = 48
                ) { onJoin(pin) }
            }
        }
    }
}

/** Six digit cells over one hidden text field, so paste and the number pad behave normally. */
@Composable
private fun PinBoxes(pin: String, onPinChange: (String) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val fieldLabel = stringResource(Res.string.community_discover_pin_field)
    BasicTextField(
        value = pin,
        onValueChange = { raw -> onPinChange(raw.filter(Char::isDigit).take(QuizPinLength)) },
        singleLine = true,
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = TextStyle(color = Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onDone() }),
        modifier = modifier
            .focusRequester(focus)
            .semantics { contentDescription = fieldLabel },
        decorationBox = { inner ->
            Box {
                // The real field stays in place for focus and input; the cells draw what was typed.
                Box(Modifier.size(1.dp)) { inner() }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(QuizPinLength) { index ->
                        val digit = pin.getOrNull(index)
                        val current = index == pin.length
                        Box(
                            Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(QuizStageRaised)
                                .border(
                                    if (current) 1.5.dp else 1.dp,
                                    if (current) DeepGreen else Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(digit?.toString().orEmpty(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun StagePill(label: String, enabled: Boolean, height: Int = 40, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .height(height.dp)
            .pressable(interaction, onClick = { if (enabled) onClick() })
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) DeepGreen else Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (enabled) StageInk else Color.White.copy(alpha = 0.5f))
    }
}
