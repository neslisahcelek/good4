package com.good4.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.campuscloset.TiltedIcon
import com.good4.community.SmallToggle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import good4.composeapp.generated.resources.*
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SocialCreateScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: SocialCreateViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdActivityId) { state.createdActivityId?.let(onCreated) }
    val generatedTitle = generatedTitle(state.date, state.hour, state.type, state.game)
    SocialCreateContent(
        state = state,
        generatedTitle = generatedTitle,
        quickDates = remember { viewModel.quickDates() },
        onBack = { if (state.type != null) viewModel.changeType() else onBack() },
        onClose = onBack,
        onSelectKind = viewModel::selectKind,
        onSelectType = viewModel::selectType,
        onTitle = viewModel::setTitle,
        onNote = viewModel::setNote,
        onDate = viewModel::setDate,
        onTime = viewModel::setTime,
        onLevel = viewModel::setLevel,
        onGame = viewModel::setGame,
        onCapacity = viewModel::setCapacity,
        onSubmit = { viewModel.submit(generatedTitle) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SocialCreateContent(
    state: SocialCreateState,
    generatedTitle: String,
    quickDates: List<LocalDate>,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onSelectKind: (String) -> Unit,
    onSelectType: (SocialType) -> Unit,
    onTitle: (String) -> Unit,
    onNote: (String) -> Unit,
    onDate: (LocalDate) -> Unit,
    onTime: (Int, Int) -> Unit,
    onLevel: (String) -> Unit,
    onGame: (String) -> Unit,
    onCapacity: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    val type = state.type
    Good4Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            Good4TopBar(
                title = stringResource(if (type == null) Res.string.social_create else Res.string.social_details),
                navigationIcon = {
                    IconButton(onClick = if (type == null) onClose else onBack) {
                        Icon(if (type == null) Icons.Outlined.Close else Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.social_back))
                    }
                },
                actions = {
                    Text(stringResource(Res.string.social_step, if (type == null) 1 else 2), color = TextSecondary, fontSize = 13.sp,
                        modifier = Modifier.padding(end = 16.dp))
                }
            )
        },
        bottomBar = {
            if (type != null) {
                SocialBottomBar {
                    state.error?.let { Text(it.asString(), color = ErrorRed, fontSize = 13.sp, lineHeight = 18.sp) }
                    SocialPrimaryButton(stringResource(Res.string.social_publish), onSubmit, loading = state.submitting)
                }
            }
        }
    ) { padding ->
        if (type == null) {
            TypeList(state.kind, onSelectKind, onSelectType, Modifier.fillMaxSize().background(AppBackground).padding(padding))
        } else {
            Column(
                Modifier.fillMaxSize().background(AppBackground).padding(padding).verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                PosterPreview(state, type, generatedTitle, onTitle, onBack)
                if (state.type == "board-games" || state.type == "video-games") {
                    Field(stringResource(Res.string.social_which_game)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val games = if (state.type == "board-games") SOCIAL_BOARD_GAMES else SOCIAL_VIDEO_GAMES
                            games.forEach { (id, label) ->
                                GameChoice(stringResource(label), state.game == id, id == "okey") { onGame(id) }
                            }
                        }
                    }
                }
                Field(stringResource(Res.string.social_when)) { WhenPicker(state, quickDates, onDate, onTime) }
                if (state.isSport) {
                    Field(stringResource(Res.string.social_level)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SOCIAL_LEVELS.forEach { (id, label) -> SmallToggle(stringResource(label), state.level == id) { onLevel(id) } }
                        }
                    }
                }
                CapacityStepper(state.capacity, onCapacity)
                Field(stringResource(Res.string.social_note_optional)) {
                    OutlinedTextField(
                        value = state.note, onValueChange = onNote, minLines = 2, maxLines = 5,
                        placeholder = { Text(stringResource(Res.string.social_note_hint)) },
                        supportingText = { Text("${state.note.length}/${SocialLimits.MAX_NOTE} · ${stringResource(Res.string.social_no_phone)}") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderMuted.copy(alpha = 0.55f))
                    )
                }
                Text(stringResource(Res.string.social_place_in_chat), color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                Text(stringResource(Res.string.social_auto_close), color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun TypeList(kind: String, onSelectKind: (String) -> Unit, onSelectType: (SocialType) -> Unit, modifier: Modifier) {
    val types = SOCIAL_TYPES.filter { it.kind == kind }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp)) {
        item(key = "title") {
            Text(stringResource(Res.string.social_what_to_do), color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                lineHeight = 30.sp, modifier = Modifier.padding(bottom = 14.dp))
            SocialSegmented(
                listOf(stringResource(Res.string.social_kind_social), stringResource(Res.string.social_kind_sport)),
                if (kind == "social") 0 else 1, { onSelectKind(if (it == 0) "social" else "sport") },
                Modifier.padding(bottom = 6.dp)
            )
        }
        items(types, key = { it.id }) { type ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelectType(type) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TiltedIcon(type.icon, kindAccent(type.kind), size = 40, iconSize = 21)
                Text(stringResource(type.label), color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextSecondary)
            }
            HorizontalDivider(color = BorderMuted.copy(alpha = 0.35f))
        }
    }
}

/** The poster as it will look in the feed, next to the title it will carry. */
@Composable
private fun PosterPreview(state: SocialCreateState, type: String, generatedTitle: String, onTitle: (String) -> Unit, onChangeType: () -> Unit) {
    val startsAt = state.startsAt
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SocialActivityPoster(
                type, state.kind, Modifier.width(92.dp).height(115.dp).clip(RoundedCornerShape(12.dp)),
                dayLabel = dayLabel(localDateOf(startsAt)), timeLabel = clockLabel(startsAt), timeSize = 17.sp,
                textPadding = 7.dp, game = state.game
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(typeLabel(type), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onChangeType) {
                    Text(stringResource(Res.string.social_change_type))
                }
            }
        }
        OutlinedTextField(
            value = state.customTitle ?: generatedTitle, onValueChange = onTitle,
            label = { Text(stringResource(Res.string.social_activity_title)) },
            trailingIcon = { Icon(Icons.Outlined.Edit, stringResource(Res.string.social_edit_title)) },
            supportingText = { Text(stringResource(Res.string.social_title_edit_hint)) },
            minLines = 1, maxLines = 2,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderMuted.copy(alpha = 0.75f))
        )
    }
}

@Composable
private fun GameChoice(label: String, selected: Boolean, okey: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(12.dp), color = SurfaceDefault,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else BorderMuted.copy(alpha = 0.55f))
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (okey) Icon(Icons.Outlined.Casino, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 14.sp, fontWeight = if (okey) FontWeight.Bold else if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else TextPrimary)
        }
    }
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun WhenPicker(state: SocialCreateState, quickDates: List<LocalDate>, onDate: (LocalDate) -> Unit, onTime: (Int, Int) -> Unit) {
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DateTimeField(stringResource(Res.string.social_date), fullWhenLabel(state.startsAt).substringBefore(" · "), Icons.Outlined.CalendarMonth) { pickingDate = true }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickDates.take(2).forEach { date -> SmallToggle(dayLabel(date), state.date == date) { onDate(date) } }
        }
        DateTimeField(stringResource(Res.string.social_time), clockLabel(state.startsAt), Icons.Outlined.Schedule) { pickingTime = true }
    }

    if (pickingDate) {
        val zone = TimeZone.UTC
        val first = quickDates.first()
        val last = first.plus(DatePeriod(days = SocialLimits.MAX_LEAD_DAYS))
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDayIn(zone).toEpochMilliseconds(),
            yearRange = first.year..last.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateTime(zone).date
                    return date >= first && date <= last
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(enabled = pickerState.selectedDateMillis != null, onClick = {
                    pickerState.selectedDateMillis?.let { onDate(Instant.fromEpochMilliseconds(it).toLocalDateTime(zone).date) }
                    pickingDate = false
                }) { Text(stringResource(Res.string.social_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(Res.string.social_cancel)) } }
        ) {
            DatePicker(pickerState,
                title = { Text(stringResource(Res.string.social_pick_date), modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                headline = { Text(dayHeading(Instant.fromEpochMilliseconds(pickerState.selectedDateMillis ?: state.date.atStartOfDayIn(zone).toEpochMilliseconds()).toLocalDateTime(zone).date),
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp), fontSize = 24.sp) }
            )
        }
    }
    if (pickingTime) {
        val timeState = rememberTimePickerState(initialHour = state.hour, initialMinute = state.minute, is24Hour = true)
        var useDial by remember { mutableStateOf(false) }
        var hourInput by remember { mutableStateOf(TextFieldValue(state.hour.toString().padStart(2, '0'))) }
        var minuteInput by remember { mutableStateOf(TextFieldValue(state.minute.toString().padStart(2, '0'))) }
        val validInput = hourInput.text.toIntOrNull() in 0..23 && minuteInput.text.toIntOrNull() in 0..59
        val minuteFocus = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text(stringResource(Res.string.social_pick_time)) },
            confirmButton = { TextButton(enabled = useDial || validInput, onClick = {
                onTime(timeState.hour, timeState.minute); pickingTime = false
            }) { Text(stringResource(Res.string.social_ok)) } },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(Res.string.social_cancel)) } },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(Res.string.social_time_hint), color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))
                    if (useDial) TimePicker(timeState) else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeNumberField(hourInput, {
                            hourInput = it
                            it.text.toIntOrNull()?.takeIf { value -> value in 0..23 }?.let { value -> timeState.hour = value }
                        }, stringResource(Res.string.social_time), stringResource(Res.string.social_hour_range),
                            Modifier.weight(1f), ImeAction.Next, { minuteFocus.requestFocus() })
                        TimeNumberField(minuteInput, {
                            minuteInput = it
                            it.text.toIntOrNull()?.takeIf { value -> value in 0..59 }?.let { value -> timeState.minute = value }
                        }, stringResource(Res.string.social_minute), stringResource(Res.string.social_minute_range),
                            Modifier.weight(1f).focusRequester(minuteFocus), ImeAction.Done, { focusManager.clearFocus() })
                    }
                    TextButton(enabled = useDial || validInput, onClick = {
                        focusManager.clearFocus()
                        if (useDial) {
                            hourInput = TextFieldValue(timeState.hour.toString().padStart(2, '0'))
                            minuteInput = TextFieldValue(timeState.minute.toString().padStart(2, '0'))
                        }
                        useDial = !useDial
                    }) {
                        Text(stringResource(if (useDial) Res.string.social_time_keyboard else Res.string.social_time_dial))
                    }
                }
            }
        )
    }
}

@Composable
private fun TimeNumberField(value: TextFieldValue, onValue: (TextFieldValue) -> Unit, label: String, hint: String,
    modifier: Modifier, imeAction: ImeAction, onIme: () -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { next ->
            if (next.text.length <= 2 && next.text.all { it in '0'..'9' }) onValue(next)
        },
        label = { Text(label) }, supportingText = { Text(hint) }, singleLine = true,
        textStyle = TextStyle(fontSize = 32.sp, textAlign = TextAlign.Center, color = TextPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onIme() }, onDone = { onIme() }),
        modifier = modifier.onFocusChanged { if (it.isFocused) onValue(value.copy(selection = TextRange(0, value.text.length))) },
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun DateTimeField(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label: $value" }, shape = RoundedCornerShape(12.dp), color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.65f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, color = TextSecondary, fontSize = 12.sp)
                Text(value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextSecondary)
        }
    }
}

@Composable
private fun CapacityStepper(capacity: Int, onCapacity: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.social_capacity), color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        StepButton(Icons.Outlined.Remove, enabled = capacity > 1) { onCapacity(capacity - 1) }
        Text(capacity.toString(), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp))
        StepButton(Icons.Outlined.Add, enabled = capacity < SocialLimits.MAX_CAPACITY) { onCapacity(capacity + 1) }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = CircleShape, color = SurfaceDefault, border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f))) {
        Icon(icon, null, tint = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.5f), modifier = Modifier.padding(8.dp).size(18.dp))
    }
}

private fun turkishLower(text: String): String = text.replace('I', 'ı').replace('İ', 'i').lowercase()

/**
 * "Cumartesi akşamı tenis", "Pazar sabahı koşu". The title is stored, so it
 * names the weekday (or the date) instead of words like "yarın" that go stale.
 */
@Composable
internal fun generatedTitle(date: LocalDate, hour: Int, typeId: String?, gameId: String? = null): String {
    val type = typeId?.let(::socialType)
    // A chosen board game names the activity ("okey") instead of the generic "masa oyunları".
    val game = gameId?.takeIf { it != "other" }?.let { id -> (SOCIAL_BOARD_GAMES + SOCIAL_VIDEO_GAMES).firstOrNull { it.first == id } }
    // "Yürüyüş/Doğa" reads as "yürüyüş" in a sentence.
    val label = (game?.second ?: type?.label)?.let { stringResource(it) }.orEmpty().substringBefore('/')
    val noun = when {
        type == null || type.id.startsWith("other") -> stringResource(Res.string.social_title_noun_other)
        label.take(2).all { it.isUpperCase() } -> label
        else -> turkishLower(label)
    }
    val part = when (hour) {
        in 5..10 -> Res.string.social_part_morning_of
        in 11..13 -> Res.string.social_part_noon
        in 14..17 -> Res.string.social_part_afternoon
        in 18..21 -> Res.string.social_part_evening_of
        else -> Res.string.social_part_night_of
    }
    val day = if (date < today().plus(DatePeriod(days = 7))) stringResource(weekdayResource(date)) else dayLabel(date)
    return stringResource(Res.string.social_title_day, day, stringResource(part), noun).take(SocialLimits.MAX_TITLE)
}
