package com.good4.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.SurfaceCanvasWarm
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

private val monthNames = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
private val weekNames = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AcademicCalendarScreen(
    onBack: () -> Unit,
    viewModel: AcademicCalendarViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var month by rememberSaveable { mutableStateOf(LocalDate(today.year, today.monthNumber, 1).toString()) }
    var selectedDate by rememberSaveable { mutableStateOf(today.toString()) }
    var faculty by rememberSaveable { mutableStateOf("Tümü") }
    val first = LocalDate.parse(month)
    val faculties = remember(state.events) {
        val available = state.events.map { it.faculty }.distinct()
        listOf("Tümü") + listOf("Genel").filter { it in available } + available.filter { it != "Genel" }.sorted()
    }
    val visibleEvents = remember(state.events, faculty) {
        if (faculty == "Tümü") state.events else state.events.filter { it.faculty == "Genel" || it.faculty == faculty }
    }
    val selectedEvents = visibleEvents.filter { selectedDate in it.startDate..it.endDate }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SurfaceCanvasWarm),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        stickyHeader {
            Surface(color = PrimaryGreen, shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp, start = 8.dp, end = 18.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Geri", tint = Color.White) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Akademik Takvim", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("2026–2027 · Akdeniz Üniversitesi", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
                    }
                    Icon(Icons.Outlined.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
        item {
            Column(modifier = Modifier.padding(top = 18.dp)) {
                Text("Fakülte / Birim", modifier = Modifier.padding(horizontal = 16.dp), color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    faculties.forEach { item ->
                        Surface(
                            modifier = Modifier.clickable { faculty = item },
                            color = if (faculty == item) MaterialTheme.colorScheme.primary else SurfaceDefault,
                            shape = RoundedCornerShape(50)
                        ) { Text(item, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = if (faculty == item) MaterialTheme.colorScheme.onPrimary else TextPrimary, fontSize = 13.sp) }
                    }
                }
            }
        }
        item {
            Surface(modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(), color = SurfaceDefault, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, BorderMuted.copy(alpha = .45f)), shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { val previous = first.plus(DatePeriod(months = -1)); month = previous.toString(); selectedDate = previous.toString() }) { Icon(Icons.Outlined.ChevronLeft, "Önceki ay") }
                        Text("${monthNames[first.monthNumber - 1]} ${first.year}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { val next = first.plus(DatePeriod(months = 1)); month = next.toString(); selectedDate = next.toString() }) { Icon(Icons.Outlined.ChevronRight, "Sonraki ay") }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { weekNames.forEachIndexed { index, name -> Text(name, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = if (index == 6) MaterialTheme.colorScheme.error else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }
                    Spacer(Modifier.height(7.dp))
                    val leading = first.dayOfWeek.ordinal
                    val days = daysInMonth(first.year, first.monthNumber)
                    repeat(6) { row ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            repeat(7) { column ->
                                val day = row * 7 + column - leading + 1
                                if (day !in 1..days) Spacer(Modifier.weight(1f).height(54.dp)) else {
                                    val date = LocalDate(first.year, first.monthNumber, day).toString()
                                    val hasEvents = visibleEvents.any { date in it.startDate..it.endDate }
                                    val selected = date == selectedDate
                                    Column(
                                        modifier = Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(14.dp)).clickable { selectedDate = date }.background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .12f) else Color.Transparent),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(31.dp).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)) {
                                            Text(day.toString(), color = if (selected) MaterialTheme.colorScheme.onPrimary else TextPrimary, fontWeight = if (selected || date == today.toString()) FontWeight.Bold else FontWeight.Normal)
                                        }
                                        if (hasEvents) Box(Modifier.padding(top = 3.dp).size(5.dp).background(Color(0xFF8D76E8), CircleShape)) else Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(formatDate(selectedDate), color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                when {
                    state.loading -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                    state.failed -> EmptyCalendarMessage("Takvim şu anda yüklenemedi.")
                    selectedEvents.isEmpty() -> EmptyCalendarMessage("Bu tarihte akademik takvim kaydı yok.")
                    else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { selectedEvents.forEach { CalendarEventCard(it) } }
                }
            }
        }
    }
}

@Composable private fun CalendarEventCard(item: AcademicCalendarEvent) {
    Surface(color = SurfaceDefault, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, BorderMuted.copy(alpha = .45f)), shadowElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(item.faculty, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(item.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            if (item.startDate != item.endDate) Text("${formatDate(item.startDate)} – ${formatDate(item.endDate)}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
            if (item.description.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .07f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Açıklama", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(item.description, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable private fun EmptyCalendarMessage(text: String) { Surface(color = SurfaceDefault, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, BorderMuted.copy(alpha = .45f))) { Text(text, modifier = Modifier.fillMaxWidth().padding(22.dp), textAlign = TextAlign.Center, color = TextSecondary) } }
private fun daysInMonth(year: Int, month: Int): Int = when (month) { 2 -> if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) 29 else 28; 4, 6, 9, 11 -> 30; else -> 31 }
private fun formatDate(value: String): String = runCatching { val date = LocalDate.parse(value); "${date.dayOfMonth} ${monthNames[date.monthNumber - 1]} ${date.year}" }.getOrDefault(value)
