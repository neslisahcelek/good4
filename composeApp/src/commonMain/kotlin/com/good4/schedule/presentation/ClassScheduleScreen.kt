package com.good4.schedule.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.SurfaceCanvasWarm
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.schedule.domain.ClassSchedule
import com.good4.schedule.domain.ScheduleDay
import com.good4.schedule.domain.ScheduleEntry
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.back
import good4.composeapp.generated.resources.schedule_select_profile
import good4.composeapp.generated.resources.schedule_title
import good4.composeapp.generated.resources.schedule_unavailable_fallback
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClassScheduleScreen(
    onBackClick: () -> Unit,
    onSelectAcademicProfile: () -> Unit,
    viewModel: ClassScheduleViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = rememberSaveable { Clock.System.todayIn(TimeZone.currentSystemDefault()).toString() }
    var selectedDateValue by rememberSaveable { mutableStateOf(today) }
    val selectedDate = LocalDate.parse(selectedDateValue)
    val selectedDay = selectedDate.toScheduleDay()
    val uriHandler = LocalUriHandler.current
    val reviewListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val schedule = state.schedule
    com.good4.review.ReviewFeaturePrompt(
        feature = com.good4.review.ReviewFeature.CLASS_SCHEDULE,
        contentReady = !state.isLoading && state.errorMessage == null && !schedule?.entries.isNullOrEmpty(),
        isScrolling = reviewListState.isScrollInProgress,
        canPrompt = !state.isAcademicSelectionSheetVisible
    )
    val availableSections = schedule?.entries.orEmpty().mapNotNull { it.section }.distinct()
    var selectedSection by rememberSaveable { mutableStateOf("Tümü") }
    var awaitingProfileUpdate by rememberSaveable { mutableStateOf(false) }
    val openAcademicProfile = {
        viewModel.dismissAcademicSelectionSheet()
        awaitingProfileUpdate = true
        onSelectAcademicProfile()
    }

    // Reloads only after the student visited the academic selection; returning from the source PDF,
    // the app switcher or the lock screen keeps the schedule without another Firestore read.
    LifecycleResumeEffect(Unit) {
        if (awaitingProfileUpdate) {
            awaitingProfileUpdate = false
            viewModel.refresh()
        }
        onPauseOrDispose { }
    }

    if (state.isAcademicSelectionSheetVisible) {
        AcademicSelectionBottomSheet(
            missingFields = state.missingAcademicFields,
            onOpenSettings = openAcademicProfile,
            onDismiss = viewModel::dismissAcademicSelectionSheet
        )
    }

    Good4NestedScaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.schedule_title),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = reviewListState,
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceCanvasWarm)
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                schedule?.let { ScheduleHeader(it) }
            }

            val emptyScheduleWarning = schedule
                ?.takeIf { !state.isLoading && it.entries.isEmpty() && it.sourceWarning == null }
                ?.let { "Bölümün yayımladığı programda ${it.classYear} için ders bulunmuyor." }
            (schedule?.sourceWarning ?: emptyScheduleWarning)?.let { warning ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = warning,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (!state.isLoading && !state.isProfileSelectionComplete && !state.isAcademicProfileMissing) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableWithoutRipple(openAcademicProfile),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = stringResource(Res.string.schedule_unavailable_fallback),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 13.sp
                            )
                            Text(
                                text = stringResource(Res.string.schedule_select_profile),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            item {
                ScheduleWeekPicker(
                    selectedDate = selectedDate,
                    today = LocalDate.parse(today),
                    onDateSelected = { selectedDateValue = it.toString() }
                )
            }

            if (availableSections.isNotEmpty()) {
                item {
                    ScheduleSectionPicker(
                        sections = availableSections,
                        selectedSection = selectedSection,
                        onSectionSelected = { selectedSection = it }
                    )
                }
            }

            item {
                Text(
                    text = "${selectedDate.dayOfWeek.turkishLabel()} · ${selectedDate.formatForSchedule()}",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                val entries = selectedDay?.let { day ->
                    schedule?.entries.orEmpty().filter { entry ->
                        entry.day == day && (selectedSection == "Tümü" || entry.section == null || entry.section == selectedSection)
                    }
                }.orEmpty()
                if (entries.isEmpty()) {
                    item { EmptyScheduleMessage() }
                } else {
                    items(entries, key = { "${it.day}-${it.startTime}-${it.endTime}-${it.courseCode}-${it.courseName}-${it.classroom}-${it.section}" }) { entry ->
                        ScheduleEntryCard(entry)
                    }
                }
            }

            item {
                schedule?.let { currentSchedule ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableWithoutRipple { uriHandler.openUri(currentSchedule.sourceUrl) },
                        color = SurfaceDefault,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Resmî ders programını aç",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            if (!state.isLoading && state.isAcademicProfileMissing) {
                item(key = "academicSelectionButton") {
                    AcademicProfileSelectionButton(
                        onSelect = viewModel::showAcademicSelectionSheet,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleSectionPicker(
    sections: List<String>,
    selectedSection: String,
    onSectionSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDefault,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (listOf("Tümü") + sections).forEach { section ->
                Surface(
                    modifier = Modifier.clickableWithoutRipple { onSectionSelected(section) },
                    color = if (section == selectedSection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = .08f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = section,
                        color = if (section == selectedSection) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleWeekPicker(
    selectedDate: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val weekStart = selectedDate.startOfWeek()
    val weekEnd = weekStart.plus(DatePeriod(days = 4))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDefault,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    onDateSelected(selectedDate.plus(DatePeriod(days = -7)))
                }) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Önceki hafta")
                }
                Text(
                    text = "${weekStart.formatForSchedule()} – ${weekEnd.formatForSchedule()}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    onDateSelected(selectedDate.plus(DatePeriod(days = 7)))
                }) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Sonraki hafta")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(5) { offset ->
                    val date = weekStart.plus(DatePeriod(days = offset))
                    val selected = date == selectedDate
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickableWithoutRipple { onDateSelected(date) },
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = .07f),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = date.dayOfWeek.shortLabel(),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Bugün",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 2.dp)
                    .clickableWithoutRipple { onDateSelected(today) }
            )
        }
    }
}

@Composable
private fun ScheduleHeader(schedule: ClassSchedule) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PrimaryGreen,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.School, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "${schedule.department} · ${schedule.classYear}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${schedule.faculty} · ${schedule.academicYear} ${schedule.term}",
                color = Color.White.copy(alpha = .86f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Son güncelleme: ${schedule.updatedAt}",
                color = Color.White.copy(alpha = .75f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ScheduleEntryCard(entry: ScheduleEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDefault,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.width(92.dp)) {
                val periods = entry.periods()
                if (periods != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        periods.forEach { period ->
                            Text(
                                text = "${period.start}–${period.end}",
                                color = if (period == periods.first()) MaterialTheme.colorScheme.primary else TextSecondary,
                                fontWeight = if (period == periods.first()) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "${entry.startTime}–${entry.endTime}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                if (entry.courseCode.isNotBlank()) {
                    Text(entry.courseCode, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    entry.courseName,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = if (entry.courseCode.isBlank()) 0.dp else 3.dp)
                )
                if (entry.classroom.isNotBlank()) {
                    Text(entry.classroom, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                }
                if (entry.instructor.isNotBlank()) {
                    Text(entry.instructor, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScheduleTag(entry.courseType)
                    entry.note?.let { ScheduleTag(it) }
                    entry.section?.let { ScheduleTag(it) }
                }
            }
        }
    }
}

private data class StandardPeriod(
    val number: Int,
    val start: String,
    val end: String
)

private val engineeringPeriods = listOf(
    StandardPeriod(1, "08:30", "09:20"),
    StandardPeriod(2, "09:30", "10:20"),
    StandardPeriod(3, "10:30", "11:20"),
    StandardPeriod(4, "11:30", "12:20"),
    // Mimarlık ve İç Mimarlık çizelgelerinde 12:30–13:20 satırı da kullanılıyor.
    StandardPeriod(5, "12:30", "13:20"),
    StandardPeriod(6, "13:30", "14:20"),
    StandardPeriod(7, "14:30", "15:20"),
    StandardPeriod(8, "15:30", "16:20"),
    StandardPeriod(9, "16:30", "17:20"),
    StandardPeriod(10, "17:30", "18:20"),
    StandardPeriod(11, "18:30", "19:20")
)

private val architecturePeriods = listOf(
    StandardPeriod(1, "08:30", "09:20"),
    StandardPeriod(2, "09:30", "10:20"),
    StandardPeriod(3, "10:30", "11:20"),
    StandardPeriod(4, "11:30", "12:20"),
    StandardPeriod(5, "12:30", "13:20"),
    StandardPeriod(6, "13:30", "14:20"),
    StandardPeriod(7, "14:30", "15:20"),
    StandardPeriod(8, "15:30", "16:20"),
    StandardPeriod(9, "16:30", "17:20"),
    StandardPeriod(10, "17:30", "18:20"),
    // Mimarlık Fakültesi çizelgesindeki son blok 18:25–19:15 olarak veriliyor.
    StandardPeriod(11, "18:25", "19:15")
)

private val interiorArchitecturePeriods = listOf(
    StandardPeriod(1, "08:30", "09:20"),
    StandardPeriod(2, "09:30", "10:20"),
    StandardPeriod(3, "10:30", "11:20"),
    StandardPeriod(4, "11:30", "12:20"),
    StandardPeriod(5, "12:30", "13:20"),
    StandardPeriod(6, "13:30", "14:20"),
    StandardPeriod(7, "14:30", "15:20"),
    StandardPeriod(8, "15:30", "16:20"),
    StandardPeriod(9, "16:30", "17:20"),
    StandardPeriod(10, "17:20", "18:30")
)

private val foodPeriods = listOf(
    StandardPeriod(1, "08:30", "09:15"),
    StandardPeriod(2, "09:30", "10:15"),
    StandardPeriod(3, "10:30", "11:15"),
    StandardPeriod(4, "11:30", "12:15"),
    StandardPeriod(5, "13:30", "14:15"),
    StandardPeriod(6, "14:30", "15:15"),
    StandardPeriod(7, "15:30", "16:15"),
    StandardPeriod(8, "16:30", "17:15")
)

private fun ScheduleEntry.periods(): List<StandardPeriod>? {
    fun findPeriods(periods: List<StandardPeriod>): List<StandardPeriod>? {
        val first = periods.indexOfFirst { it.start == startTime }
        val last = periods.indexOfLast { it.end == endTime }
        if (first < 0 || last < first) return null
        return periods.subList(first, last + 1)
    }

    return findPeriods(interiorArchitecturePeriods)
        ?: findPeriods(architecturePeriods)
        ?: findPeriods(engineeringPeriods)
        ?: findPeriods(foodPeriods)
}

@Composable
private fun ScheduleTag(text: String) {
    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = .1f), shape = RoundedCornerShape(50)) {
        Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun EmptyScheduleMessage() {
    Surface(color = SurfaceDefault, shape = RoundedCornerShape(18.dp)) {
        Text(
            text = "Bu tarih için ders bulunmuyor.",
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        )
    }
}

private fun LocalDate.startOfWeek(): LocalDate = plus(DatePeriod(days = -dayOfWeek.ordinal))

private fun LocalDate.toScheduleDay(): ScheduleDay? = when (dayOfWeek) {
    DayOfWeek.MONDAY -> ScheduleDay.MONDAY
    DayOfWeek.TUESDAY -> ScheduleDay.TUESDAY
    DayOfWeek.WEDNESDAY -> ScheduleDay.WEDNESDAY
    DayOfWeek.THURSDAY -> ScheduleDay.THURSDAY
    DayOfWeek.FRIDAY -> ScheduleDay.FRIDAY
    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> null
}

private fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "Pzt"
    DayOfWeek.TUESDAY -> "Sal"
    DayOfWeek.WEDNESDAY -> "Çar"
    DayOfWeek.THURSDAY -> "Per"
    DayOfWeek.FRIDAY -> "Cum"
    DayOfWeek.SATURDAY -> "Cmt"
    DayOfWeek.SUNDAY -> "Paz"
}

private fun DayOfWeek.turkishLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "Pazartesi"
    DayOfWeek.TUESDAY -> "Salı"
    DayOfWeek.WEDNESDAY -> "Çarşamba"
    DayOfWeek.THURSDAY -> "Perşembe"
    DayOfWeek.FRIDAY -> "Cuma"
    DayOfWeek.SATURDAY -> "Cumartesi"
    DayOfWeek.SUNDAY -> "Pazar"
}

private fun LocalDate.formatForSchedule(): String =
    "$dayOfMonth ${scheduleMonthNames[monthNumber - 1]}"

private val scheduleMonthNames = listOf(
    "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
)

private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
