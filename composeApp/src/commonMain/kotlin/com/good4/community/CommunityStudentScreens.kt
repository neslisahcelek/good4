package com.good4.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.good4.campuscloset.ClosetCard
import com.good4.campuscloset.TiltedIcon
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.components.Good4ConfirmDialog
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import kotlinx.datetime.LocalDateTime

internal enum class StudentEventFilter(val label: String, val emptyTitle: String, val emptySubtitle: String) {
    UPCOMING("Yaklaşan", "Yaklaşan etkinlik yok", "Yeni etkinlikler yayınlandığında burada görünecek."),
    REGISTERED("Kayıtlarım", "Henüz bir etkinliğe kayıt olmadın", "Yaklaşan etkinlikleri keşfedip katılmak istediklerine kayıt olabilirsin."),
    PAST("Geçmiş", "Geçmiş etkinlik yok", "Tamamlanan etkinlikler burada görünecek.")
}

internal enum class StudentEventStatus(val label: String) {
    REGISTERED("Kayıtlısın"), ENDED("Tamamlandı"), FULL("Kontenjan doldu")
}

internal fun studentEventStatus(data: CommunityEntryDto, registered: Boolean, now: LocalDateTime): StudentEventStatus? = when {
    registered -> StudentEventStatus.REGISTERED
    data.hasEnded(now) -> StudentEventStatus.ENDED
    data.capacity > 0 && data.registrationCount >= data.capacity -> StudentEventStatus.FULL
    else -> null
}

@Composable
private fun StudentEventStatusChip(data: CommunityEntryDto, registered: Boolean, now: LocalDateTime) {
    studentEventStatus(data, registered, now)?.let { status ->
        val color = when (status) {
            StudentEventStatus.REGISTERED -> MaterialTheme.colorScheme.primary
            StudentEventStatus.ENDED -> TextSecondary
            StudentEventStatus.FULL -> ErrorRed
        }
        StatusChip(status.label, color)
    }
}

@Composable
internal fun StudentEventRow(entry: CommunityEntry, registered: Boolean, now: LocalDateTime, onClick: () -> Unit) {
    ClosetCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            EventDateTile(entry.data, Modifier.width(60.dp).aspectRatio(CoverImageSpec.ASPECT_RATIO))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.data.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(formatEventSchedule(entry.data), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (entry.data.location.isNotBlank()) {
                    Text(entry.data.location, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                StudentEventStatusChip(entry.data, registered, now)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

internal fun studentEventsFor(
    entries: List<CommunityEntry>, filter: StudentEventFilter, registeredEventIds: Set<String>, now: LocalDateTime
): List<CommunityEntry> = entries.filter { entry ->
    entry.data.kind == "event" && entry.data.status == "published" && when (filter) {
        StudentEventFilter.UPCOMING -> !entry.data.hasEnded(now)
        StudentEventFilter.REGISTERED -> entry.id in registeredEventIds
        StudentEventFilter.PAST -> entry.data.hasEnded(now)
    }
}.let { filtered ->
    if (filter == StudentEventFilter.PAST) filtered.sortedByDescending { it.data.date + it.data.time }
    else filtered.sortedBy { it.data.date + it.data.time }
}

@Composable
internal fun StudentEventFilters(
    entries: List<CommunityEntry>, registeredEventIds: Set<String>, now: LocalDateTime,
    selected: StudentEventFilter, onSelect: (StudentEventFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StudentEventFilter.entries.forEach { filter ->
            val icon = when (filter) {
                StudentEventFilter.UPCOMING -> Icons.Outlined.Event
                StudentEventFilter.REGISTERED -> Icons.Outlined.ConfirmationNumber
                StudentEventFilter.PAST -> Icons.Outlined.History
            }
            val accent = when (filter) {
                StudentEventFilter.UPCOMING -> PrimaryGreen
                StudentEventFilter.REGISTERED -> CommunityAccent
                StudentEventFilter.PAST -> TextSecondary
            }
            FilterTile(
                label = filter.label, count = studentEventsFor(entries, filter, registeredEventIds, now).size,
                icon = icon, accent = accent, selected = selected == filter,
                modifier = Modifier.weight(1f), onClick = { onSelect(filter) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CommunityStudentEventPage(
    community: Community, entry: CommunityEntry, state: CommunityState, now: LocalDateTime,
    onBack: () -> Unit, onToggleRegistration: () -> Unit, onShowTicket: () -> Unit,
    onReport: () -> Unit, onDismissError: () -> Unit, onDismissRegistrationFeedback: () -> Unit
) {
    val data = entry.data
    val registered = entry.id in state.registeredEventIds
    val registrationLoading = entry.id in state.registrationLoadingIds
    val cancelling = entry.id in state.cancellingRegistrationIds
    val ended = data.hasEnded(now)
    val full = data.capacity > 0 && data.registrationCount >= data.capacity
    var menuOpen by remember(entry.id) { mutableStateOf(false) }
    var confirmCancel by remember(entry.id) { mutableStateOf(false) }
    var viewingPoster by remember(entry.id) { mutableStateOf(false) }

    Good4NestedScaffold(
        topBar = {
            Good4TopBar(
                title = "Etkinlik",
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
                actions = {
                    Box {
                        RoundIconAction(Icons.Outlined.MoreVert, "Diğer işlemler") { menuOpen = true }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = SurfaceDefault) {
                            if (registered && !ended) DropdownMenuItem(
                                text = { Text("Kaydımı iptal et", color = ErrorRed) },
                                leadingIcon = { Icon(Icons.Outlined.EventBusy, contentDescription = null, tint = ErrorRed) },
                                enabled = !registrationLoading,
                                onClick = { menuOpen = false; confirmCancel = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Şikâyet et", color = ErrorRed) },
                                leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null, tint = ErrorRed) },
                                onClick = { menuOpen = false; onReport() }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            StickyActionBar {
                Text(
                    when {
                        ended -> "Bu etkinlik tamamlandı."
                        cancelling -> "Kaydın iptal ediliyor."
                        registrationLoading -> "Kaydın işleniyor."
                        registered -> "Girişte QR biletini göster"
                        full -> "Bu etkinlik için yeni kayıt alınamıyor."
                        else -> "Katılmak için etkinliğe kayıt ol."
                    },
                    color = TextSecondary, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = if (registered) onShowTicket else onToggleRegistration,
                    enabled = !registrationLoading && !ended && (registered || !full),
                    modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        when {
                            ended -> "Etkinlik sona erdi"
                            cancelling -> "İptal ediliyor…"
                            registrationLoading -> "Kaydediliyor…"
                            registered -> "QR biletimi göster"
                            full -> "Kontenjan doldu"
                            else -> "Etkinliğe kayıt ol"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(AppBackground).padding(padding),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Centred at about two thirds of the width so the title and date stay on the first screen.
            if (data.imageUrl.isNotBlank()) item(key = "poster") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = data.imageUrl, contentDescription = "Kapak görseli", contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth(0.68f).aspectRatio(CoverImageSpec.ASPECT_RATIO)
                            .clip(RoundedCornerShape(18.dp)).background(PistachioGreen).clickable { viewingPoster = true }
                    )
                }
            }
            item(key = "heading") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(8.dp), color = SurfaceDefault,
                            border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f))) {
                            Text(EventCategory.labelFor(data.categoryId), color = TextSecondary, fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        data.relativeDayLabel(now)?.let { StatusChip(it, MaterialTheme.colorScheme.primary) }
                        StudentEventStatusChip(data, registered, now)
                    }
                    Text(data.title, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
            item(key = "schedule") {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = PistachioGreen) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Text(formatEventSchedule(data), color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                        if (data.location.isNotBlank()) InfoLine(Icons.Outlined.LocationOn, data.location)
                    }
                }
            }
            if (state.justRegisteredEntryId == entry.id) item(key = "registered-feedback") {
                NoticeCard("Kaydın tamamlandı. Girişte QR biletini gösterebilirsin.", MaterialTheme.colorScheme.primary,
                    actionLabel = "Kapat", onAction = onDismissRegistrationFeedback)
            }
            if (data.description.isNotBlank()) item(key = "description") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Etkinlik hakkında", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    CollapsibleDescription(data.description, key = entry.id, collapsedLines = 4)
                }
            }
            item(key = "organizer") {
                Surface(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    color = SurfaceDefault, border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f))) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (community.data.logoUrl.isNotBlank()) {
                            AsyncImage(community.data.logoUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)))
                        } else TiltedIcon(Icons.Outlined.Groups, CommunityAccent, size = 32)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Düzenleyen", color = TextSecondary, fontSize = 12.sp)
                            Text(community.data.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }
                }
            }
            state.error?.let { error ->
                item(key = "error") { NoticeCard(error, ErrorRed, actionLabel = "Tamam", onAction = onDismissError) }
            }
        }
    }

    if (viewingPoster && data.imageUrl.isNotBlank()) PosterViewer(data.imageUrl) { viewingPoster = false }
    if (confirmCancel) {
        Good4ConfirmDialog(
            title = "Kaydımı iptal et",
            message = "${data.title} etkinliğine kaydın ve QR biletin iptal edilecek.",
            confirmLabel = "İptal et", dismissLabel = "Vazgeç", icon = Icons.Outlined.EventBusy,
            enabled = !registrationLoading,
            onConfirm = { confirmCancel = false; onToggleRegistration() },
            onDismiss = { confirmCancel = false }
        )
    }
}
