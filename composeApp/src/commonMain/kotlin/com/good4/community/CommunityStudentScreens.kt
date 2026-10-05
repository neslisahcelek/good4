package com.good4.community

import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource


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
import com.good4.core.presentation.CommunityAccent
import com.good4.core.presentation.components.StandardButtonHeight
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

internal enum class StudentEventFilter(val labelResource: StringResource, val emptyTitleResource: StringResource, val emptySubtitleResource: StringResource) {
    UPCOMING(Res.string.community_yaklasan, Res.string.community_yaklasan_etkinlik_yok, Res.string.community_yeni_etkinlikler_yayinlandiginda_burada_gorunecek),
    REGISTERED(Res.string.community_kayitlarim, Res.string.community_henuz_bir_etkinlige_kayit_olmadin, Res.string.community_yaklasan_etkinlikleri_kesfedip_katilmak_istediklerine_kayit_olabi),
    PAST(Res.string.community_gecmis, Res.string.community_gecmis_etkinlik_yok, Res.string.community_tamamlanan_etkinlikler_burada_gorunecek) ;
    val label: String @Composable get() = stringResource(labelResource)
    val emptyTitle: String @Composable get() = stringResource(emptyTitleResource)
    val emptySubtitle: String @Composable get() = stringResource(emptySubtitleResource)
}

internal enum class StudentEventStatus(val labelResource: StringResource) {
    REGISTERED(Res.string.community_kayitlisin), ENDED(Res.string.reservation_status_completed), FULL(Res.string.community_kontenjan_doldu);
    val label: String @Composable get() = stringResource(labelResource)
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
                title = stringResource(Res.string.community_etkinlik),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.campus_closet_back)) } },
                actions = {
                    Box {
                        RoundIconAction(Icons.Outlined.MoreVert, stringResource(Res.string.community_diger_islemler)) { menuOpen = true }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = SurfaceDefault) {
                            if (registered && !ended) DropdownMenuItem(
                                text = { Text(stringResource(Res.string.community_kaydimi_iptal_et), color = ErrorRed) },
                                leadingIcon = { Icon(Icons.Outlined.EventBusy, contentDescription = null, tint = ErrorRed) },
                                enabled = !registrationLoading,
                                onClick = { menuOpen = false; confirmCancel = true }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.community_sikayet_et), color = ErrorRed) },
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
                        ended -> stringResource(Res.string.community_bu_etkinlik_tamamlandi)
                        cancelling -> stringResource(Res.string.community_kaydin_iptal_ediliyor)
                        registrationLoading -> stringResource(Res.string.community_kaydin_isleniyor)
                        registered -> stringResource(Res.string.community_giriste_qr_biletini_goster)
                        full -> stringResource(Res.string.community_bu_etkinlik_icin_yeni_kayit_alinamiyor)
                        else -> stringResource(Res.string.community_katilmak_icin_etkinlige_kayit_ol)
                    },
                    color = TextSecondary, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = if (registered) onShowTicket else onToggleRegistration,
                    enabled = !registrationLoading && !ended && (registered || !full),
                    modifier = Modifier.fillMaxWidth().height(StandardButtonHeight), shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        when {
                            ended -> stringResource(Res.string.community_etkinlik_sona_erdi)
                            cancelling -> stringResource(Res.string.community_iptal_ediliyor)
                            registrationLoading -> "Kaydediliyor…"
                            registered -> stringResource(Res.string.community_qr_biletimi_goster)
                            full -> stringResource(Res.string.community_kontenjan_doldu)
                            else -> stringResource(Res.string.community_etkinlige_kayit_ol)
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
                        model = data.imageUrl, contentDescription = stringResource(Res.string.community_kapak_gorseli), contentScale = ContentScale.Crop,
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
                NoticeCard(stringResource(Res.string.community_kaydin_tamamlandi_giriste_qr_biletini_gosterebilirsin), MaterialTheme.colorScheme.primary,
                    actionLabel = stringResource(Res.string.notification_close), onAction = onDismissRegistrationFeedback)
            }
            if (data.description.isNotBlank()) item(key = "description") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.community_etkinlik_hakkinda), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
                            Text(stringResource(Res.string.community_duzenleyen), color = TextSecondary, fontSize = 12.sp)
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
            title = stringResource(Res.string.community_kaydimi_iptal_et),
            message = stringResource(Res.string.community_cancel_my_registration, data.title),
            confirmLabel = stringResource(Res.string.community_iptal_et), dismissLabel = stringResource(Res.string.campus_closet_cancel), icon = Icons.Outlined.EventBusy,
            enabled = !registrationLoading,
            onConfirm = { confirmCancel = false; onToggleRegistration() },
            onDismiss = { confirmCancel = false }
        )
    }
}
