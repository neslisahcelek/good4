package com.good4.community

import good4.composeapp.generated.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.good4.campuscloset.ClosetCard
import com.good4.campuscloset.ClosetSectionHeading
import com.good4.campuscloset.TiltedIcon
import com.good4.core.presentation.*
import com.good4.core.presentation.components.StandardButtonHeight
import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import com.good4.core.presentation.components.Good4ConfirmDialog
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

private enum class ManagerFilter(val labelResource: StringResource, val icon: ImageVector) {
    UPCOMING(Res.string.community_yaklasan, Icons.Outlined.Event),
    DRAFT(Res.string.community_taslak, Icons.Outlined.EditNote),
    PAST(Res.string.community_gecmis, Icons.Outlined.History);
    val label: String @Composable get() = stringResource(labelResource)
}


private fun CommunityEntry.isEvent() = data.kind == "event"

private fun CommunityEntry.matches(filter: ManagerFilter, now: kotlinx.datetime.LocalDateTime) = when (filter) {
    ManagerFilter.UPCOMING -> data.status == "published" && !data.hasEnded(now)
    ManagerFilter.DRAFT -> data.status == "draft"
    ManagerFilter.PAST -> data.status == "cancelled" || (data.status == "published" && data.hasEnded(now))
}

/**
 * The community manager area: an overview, one page per event and a full-screen event form.
 * The pages are local state so the student-facing community screens stay untouched.
 */
@Composable
internal fun CommunityManagerFlow(
    state: CommunityState,
    community: Community,
    viewModel: CommunityViewModel,
    onExit: () -> Unit,
    onPreviewStudent: () -> Unit
) {
    var openEventId by rememberSaveable(community.id) { mutableStateOf<String?>(null) }
    val editor = state.eventEditor
    var filter by rememberSaveable(community.id) { mutableStateOf(ManagerFilter.UPCOMING) }
    var now by remember { mutableStateOf(eventNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            now = eventNow()
        }
    }
    if (editor == null) CommunityBackHandler {
        if (openEventId != null) { openEventId = null; viewModel.clearAdmissionMessage() } else onExit()
    }
    val openEvent = state.entries.firstOrNull { it.id == openEventId }
    val target = editor
    when {
        target != null -> ManagerEventEditor(
            target = target,
            saving = state.saving,
            error = state.error,
            onClose = viewModel::closeEventEditor,
            onDraftChange = viewModel::updateEventDraft,
            onImageChange = viewModel::setEventImage,
            onSubmit = { status ->
                viewModel.submitEventEditor(status)
                if (viewModel.state.value.eventEditor?.fieldErrors.isNullOrEmpty() && target.entryId == null)
                    filter = if (status == "draft") ManagerFilter.DRAFT else ManagerFilter.UPCOMING
            }
        )
        openEvent != null -> ManagerEventPage(
            entry = openEvent,
            now = now,
            state = state,
            onBack = { openEventId = null; viewModel.clearAdmissionMessage() },
            onEdit = { viewModel.openEventEditor(openEvent) },
            onPublish = { viewModel.save(openEvent.id, openEvent.data.copy(status = "published"), null) {} },
            onCancelEvent = { viewModel.cancel(openEvent.id) {} },
            onScanned = { viewModel.admit(openEvent, scanned = it) },
            onError = viewModel::reportError,
            onDismissMessage = viewModel::clearAdmissionMessage
        )
        else -> ManagerHome(
            state = state,
            community = community,
            now = now,
            filter = filter,
            onFilter = { filter = it },
            onExit = onExit,
            onPreviewStudent = onPreviewStudent,
            onOpenEvent = { viewModel.clearAdmissionMessage(); openEventId = it.id },
            onCreateEvent = { viewModel.openEventEditor() },
            onRetry = { viewModel.select(community) },
            onLoadMore = viewModel::loadMoreEntries
        )
    }
}

// ---------------------------------------------------------------- Overview

@Composable
private fun ManagerHome(
    state: CommunityState,
    community: Community,
    now: kotlinx.datetime.LocalDateTime,
    filter: ManagerFilter,
    onFilter: (ManagerFilter) -> Unit,
    onExit: () -> Unit,
    onPreviewStudent: () -> Unit,
    onOpenEvent: (CommunityEntry) -> Unit,
    onCreateEvent: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    val events = state.entries.filter { it.isEvent() }
    val counts = ManagerFilter.entries.associateWith { f -> events.count { it.matches(f, now) } }
    val shown = events.filter { it.matches(filter, now) }.let { list ->
        if (filter == ManagerFilter.PAST) list.sortedByDescending { it.data.date + it.data.time } else list
    }
    val nextEvent = events.filter { it.matches(ManagerFilter.UPCOMING, now) }.minByOrNull { it.data.date + it.data.time }
    val currentMonth = now.date.toString().take(7)
    val monthlyArrivals = events.filter { it.data.date.startsWith(currentMonth) }
        .sumOf { state.attendanceByEntry[it.id]?.size ?: 0 }

    Good4NestedScaffold(
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.community_toplulugum),
                navigationIcon = { IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.campus_closet_back)) } },
                actions = {
                    RoundIconAction(Icons.Outlined.Visibility, stringResource(Res.string.community_ogrencilerin_gordugu_hali), onPreviewStudent)
                }
            )
        },
        bottomBar = {
            StickyActionBar {
                Button(
                    onClick = onCreateEvent,
                    modifier = Modifier.fillMaxWidth().height(StandardButtonHeight),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.community_etkinlik_olustur), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(AppBackground).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "identity") { CommunityIdentityCard(community) }
            item(key = "stats") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(stringResource(Res.string.community_takipci), state.followerCount?.toString() ?: "–", Icons.Outlined.Groups, CommunityAccent, Modifier.weight(1f))
                    StatTile(stringResource(Res.string.community_yaklasan), counts.getValue(ManagerFilter.UPCOMING).toString(), Icons.Outlined.Event, PrimaryGreen, Modifier.weight(1f))
                    StatTile(stringResource(Res.string.community_bu_ay_gelen), monthlyArrivals.toString(), Icons.Outlined.HowToReg, CommunityAttendanceAccent, Modifier.weight(1f))
                }
            }
            state.error?.let { error ->
                item(key = "error") { NoticeCard(error, ErrorRed, actionLabel = stringResource(Res.string.campus_closet_retry), onAction = onRetry) }
            }
            if (nextEvent != null) {
                item(key = "next") {
                    NextEventCard(nextEvent, state.registrationsByEntry[nextEvent.id]?.size ?: 0, onOpen = { onOpenEvent(nextEvent) })
                }
            }
            item(key = "events-heading") {
                Text("Etkinlikler", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            }
            item(key = "filters") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ManagerFilter.entries.forEach { option ->
                        FilterTile(
                            option = option,
                            count = counts.getValue(option),
                            selected = option == filter,
                            modifier = Modifier.weight(1f),
                            onClick = { onFilter(option) }
                        )
                    }
                }
            }
            if (state.loading) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (shown.isEmpty()) {
                item(key = "empty") { EmptyEvents(filter, onCreateEvent) }
            } else {
                items(shown, key = { it.id }) { entry ->
                    ManagerEventRow(entry, state.registrationsByEntry[entry.id]?.size, now, onClick = { onOpenEvent(entry) })
                }
            }
            // Events arrive in pages; counts and filters cover only what has been loaded so far.
            if (!state.loading && state.entriesCursor != null) item(key = "load-more") {
                TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.community_load_more)) }
            }
        }
    }
}

@Composable
private fun CommunityIdentityCard(community: Community) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(community.data.name, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (community.data.university.isNotBlank()) {
                    Text(community.data.university, fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(12.dp))
            if (community.data.logoUrl.isNotBlank()) {
                AsyncImage(
                    model = community.data.logoUrl, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                )
            } else {
                TiltedIcon(Icons.Outlined.Groups, CommunityAccent, size = 40, iconSize = 22)
            }
        }
    }
}

@Composable
private fun NextEventCard(entry: CommunityEntry, registrations: Int, onOpen: () -> Unit) {
    ClosetCard(modifier = Modifier.clickable(onClick = onOpen)) {
        ClosetSectionHeading(stringResource(Res.string.community_siradaki_etkinlik), Icons.Outlined.EventAvailable)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(entry.data.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(formatEventSchedule(entry.data), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            if (entry.data.location.isNotBlank()) Text(entry.data.location, fontSize = 13.sp, color = TextSecondary)
        }
        RegistrationProgress(registrations, entry.data.capacity)
        Surface(onClick = onOpen, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.community_girisleri_yonet), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun FilterTile(option: ManagerFilter, count: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val accent = when (option) {
        ManagerFilter.UPCOMING -> PrimaryGreen
        ManagerFilter.DRAFT -> DraftAccent
        ManagerFilter.PAST -> TextSecondary
    }
    FilterTile(option.label, count, option.icon, accent, selected, modifier, onClick)
}

@Composable
private fun ManagerEventRow(entry: CommunityEntry, registrations: Int?, now: kotlinx.datetime.LocalDateTime, onClick: () -> Unit) {
    ClosetCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            EventDateTile(entry.data, Modifier.width(60.dp).aspectRatio(CoverImageSpec.ASPECT_RATIO))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.data.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(formatEventSchedule(entry.data), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventStatusChip(entry.data, now)
                    if (registrations != null && entry.data.status != "draft") {
                        Text(
                            if (entry.data.capacity > 0) stringResource(Res.string.community_registration_capacity, registrations, entry.data.capacity) else stringResource(Res.string.community_registration_count, registrations),
                            fontSize = 12.sp, color = TextSecondary
                        )
                    }
                }
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
private fun EmptyEvents(filter: ManagerFilter, onCreateEvent: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TiltedIcon(filter.icon, CommunityAccent, size = 56, iconSize = 28)
            Spacer(Modifier.height(16.dp))
            Text(
                when (filter) {
                    ManagerFilter.UPCOMING -> stringResource(Res.string.community_yaklasan_etkinlik_yok)
                    ManagerFilter.DRAFT -> stringResource(Res.string.community_taslak_yok)
                    ManagerFilter.PAST -> stringResource(Res.string.community_gecmis_etkinlik_yok)
                },
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when (filter) {
                    ManagerFilter.UPCOMING -> stringResource(Res.string.community_bir_etkinlik_olustur_takipcilerin_ve_kampusteki_ogrenciler_toplul)
                    ManagerFilter.DRAFT -> stringResource(Res.string.community_hazirlamaya_ara_verdigin_etkinlikleri_taslak_olarak_kaydedersen_b)
                    ManagerFilter.PAST -> stringResource(Res.string.community_tamamlanan_ve_iptal_edilen_etkinlikler_burada_listelenir)
                },
                fontSize = 13.sp, lineHeight = 19.sp, color = TextSecondary, textAlign = TextAlign.Center
            )
            if (filter == ManagerFilter.UPCOMING) {
                Spacer(Modifier.height(16.dp))
                Surface(onClick = onCreateEvent, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                    Text(
                        stringResource(Res.string.community_etkinlik_olustur),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Event page

@Composable
private fun ManagerEventPage(
    entry: CommunityEntry,
    now: kotlinx.datetime.LocalDateTime,
    state: CommunityState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPublish: () -> Unit,
    onCancelEvent: () -> Unit,
    onScanned: (String) -> Unit,
    onError: (String) -> Unit,
    onDismissMessage: () -> Unit
) {
    val data = entry.data
    val registrations = state.registrationsByEntry[entry.id].orEmpty()
    val arrivals = state.attendanceByEntry[entry.id].orEmpty().associateBy { it.userId }
    val ended = data.hasEnded(now)
    val admissionOpen = data.status == "published" && !ended
    var menuOpen by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var viewingPoster by remember { mutableStateOf(false) }
    var search by rememberSaveable(entry.id) { mutableStateOf("") }
    var arrivedOnly by rememberSaveable(entry.id) { mutableStateOf(false) }
    val shownPeople = registrations.filter {
        (!arrivedOnly || it.userId in arrivals) && it.displayName.contains(search.trim(), ignoreCase = true)
    }

    Good4NestedScaffold(
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.community_etkinlik),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.campus_closet_back)) } },
                actions = {
                    if (data.status != "cancelled") {
                        Box {
                            RoundIconAction(Icons.Outlined.MoreVert, stringResource(Res.string.community_diger_islemler)) { menuOpen = true }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = SurfaceDefault) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.edit)) },
                                    leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                    onClick = { menuOpen = false; onEdit() }
                                )
                                if (!ended) DropdownMenuItem(
                                    text = { Text(if (data.status == "draft") stringResource(Res.string.community_taslagi_kaldir) else stringResource(Res.string.community_etkinligi_iptal_et), color = ErrorRed) },
                                    leadingIcon = { Icon(Icons.Outlined.EventBusy, null, tint = ErrorRed) },
                                    onClick = { menuOpen = false; confirmCancel = true }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            when {
                data.status == "draft" -> StickyActionBar {
                    Text(stringResource(Res.string.community_taslaklar_ogrencilere_gorunmez), color = TextSecondary, fontSize = 12.sp)
                    Button(
                        onClick = onPublish, enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth().height(StandardButtonHeight), shape = RoundedCornerShape(16.dp)
                    ) { Text(if (state.saving) stringResource(Res.string.community_yayinlaniyor) else stringResource(Res.string.community_yayinla), fontWeight = FontWeight.SemiBold) }
                }
                admissionOpen -> StickyActionBar {
                    EventScannerButton(
                        enabled = !state.admissionBusy,
                        onScanned = onScanned,
                        onError = onError,
                        modifier = Modifier.fillMaxWidth().height(StandardButtonHeight)
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(AppBackground).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "info") {
                ClosetCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                        if (data.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = data.imageUrl, contentDescription = stringResource(Res.string.community_kapak_gorseli), contentScale = ContentScale.Crop,
                                modifier = Modifier.width(96.dp).aspectRatio(CoverImageSpec.ASPECT_RATIO)
                                    .clip(RoundedCornerShape(12.dp)).background(SurfaceMuted).clickable { viewingPoster = true }
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EventStatusChip(data, now)
                            Text(data.title, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                    }
                    InfoLine(Icons.Outlined.CalendarMonth, formatEventSchedule(data))
                    if (data.location.isNotBlank()) InfoLine(Icons.Outlined.LocationOn, data.location)
                    InfoLine(Icons.Outlined.Sell, EventCategory.labelFor(data.categoryId))
                    if (data.description.isNotBlank()) {
                        HorizontalDivider(color = BorderMuted.copy(alpha = 0.35f))
                        CollapsibleDescription(data.description, key = entry.id)
                    }
                }
            }
            if (data.status != "draft") {
                item(key = "counts") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(
                            stringResource(Res.string.community_kayit), if (data.capacity > 0) "${registrations.size}/${data.capacity}" else registrations.size.toString(),
                            Icons.Outlined.ConfirmationNumber, PrimaryGreen, Modifier.weight(1f)
                        )
                        StatTile(stringResource(Res.string.community_giris_yapti), arrivals.size.toString(), Icons.Outlined.HowToReg, CommunityAttendanceAccent, Modifier.weight(1f))
                    }
                }
            }
            state.admissionMessage?.let { message ->
                item(key = "message") { NoticeCard(message, MaterialTheme.colorScheme.primary, actionLabel = "Tamam", onAction = onDismissMessage) }
            }
            state.error?.let { error ->
                item(key = "error") { NoticeCard(error, ErrorRed, actionLabel = "Tamam", onAction = onDismissMessage) }
            }
            if (data.status != "draft") {
                item(key = "people-heading") {
                    ClosetSectionHeading(
                        stringResource(Res.string.community_katilimcilar), Icons.Outlined.Groups,
                        if (admissionOpen) stringResource(Res.string.community_girisler_yalnizca_ogrencinin_qr_bileti_okutularak_onaylanir)
                        else stringResource(Res.string.community_registered_students, registrations.size)
                    )
                }
                if (registrations.size > 5) item(key = "search") {
                    OutlinedTextField(
                        value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.community_ogrenci_ara), color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextSecondary) },
                        singleLine = true, shape = RoundedCornerShape(18.dp), colors = managerFieldColors()
                    )
                }
                if (registrations.isNotEmpty()) item(key = "people-filter") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallToggle(stringResource(Res.string.community_people_all, registrations.size), !arrivedOnly) { arrivedOnly = false }
                        SmallToggle(stringResource(Res.string.community_people_arrived, arrivals.size), arrivedOnly) { arrivedOnly = true }
                    }
                }
                item(key = "people") {
                    ClosetCard {
                        if (shownPeople.isEmpty()) {
                            Text(
                                when {
                                    search.isNotBlank() -> stringResource(Res.string.community_aramana_uygun_ogrenci_yok)
                                    arrivedOnly -> stringResource(Res.string.community_henuz_giris_yapan_yok)
                                    else -> stringResource(Res.string.community_henuz_kayitli_ogrenci_yok)
                                },
                                fontSize = 13.sp, color = TextSecondary
                            )
                        }
                        shownPeople.forEachIndexed { index, person ->
                            if (index > 0) HorizontalDivider(color = BorderMuted.copy(alpha = 0.3f))
                            ParticipantRow(person, arrivals[person.userId])
                        }
                    }
                }
            }
        }
    }

    if (viewingPoster && data.imageUrl.isNotBlank()) PosterViewer(data.imageUrl) { viewingPoster = false }
    if (confirmCancel) {
        Good4ConfirmDialog(
            title = if (data.status == "draft") stringResource(Res.string.community_taslagi_kaldir) else stringResource(Res.string.community_etkinligi_iptal_et),
            message = if (data.status == "draft") stringResource(Res.string.community_taslak_yayinlanmadan_gecmis_e_tasinir)
            else stringResource(Res.string.community_cancel_registered_message, registrations.size),
            confirmLabel = if (data.status == "draft") stringResource(Res.string.community_kaldir) else stringResource(Res.string.community_iptal_et),
            dismissLabel = stringResource(Res.string.campus_closet_cancel),
            icon = Icons.Outlined.EventBusy,
            enabled = !state.saving,
            onConfirm = { confirmCancel = false; onCancelEvent() },
            onDismiss = { confirmCancel = false }
        )
    }
}

@Composable
private fun ParticipantRow(person: CommunityEventRegistrationDto, arrival: EventAttendanceDto?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val name = person.displayName.ifBlank { stringResource(Res.string.community_good4_ogrencisi) }
        Surface(Modifier.size(38.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    name.split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() },
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
            }
        }
        Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (arrival != null) {
            val time = Instant.fromEpochSeconds(arrival.checkedInAt).toLocalDateTime(EventTimeZone)
            val color = MaterialTheme.colorScheme.primary
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.14f), border = BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
                Text(
                    "Geldi · ${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(stringResource(Res.string.community_bekleniyor), fontSize = 12.sp, color = TextSecondary)
        }
    }
}

// ---------------------------------------------------------------- Event form

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagerEventEditor(
    target: EventEditorState,
    saving: Boolean,
    error: String?,
    onClose: () -> Unit,
    onDraftChange: (CommunityEntryDto) -> Unit,
    onImageChange: (ByteArray?) -> Unit,
    onSubmit: (String) -> Unit
) {
    val isNew = target.entryId == null
    val canDraft = isNew || target.initial.status == "draft"
    val startingDraft = remember(target.entryId) { target.initial.withSuggestedEnd() }
    val draft = target.draft
    val image = target.image
    var pickerError by remember(target.entryId) { mutableStateOf<String?>(null) }
    var customEnd by remember(target.entryId) { mutableStateOf(startingDraft.matchingDuration() == EventDuration.CUSTOM) }
    var previewing by remember { mutableStateOf(false) }
    var viewingCover by remember { mutableStateOf(false) }
    val coverPicker = rememberCoverImagePicker(
        onPicked = { onImageChange(it); pickerError = null },
        onError = { pickerError = it }
    )
    var confirmDiscard by remember { mutableStateOf(false) }
    var dateTarget by remember { mutableStateOf<ScheduleField?>(null) }
    var timeTarget by remember { mutableStateOf<ScheduleField?>(null) }
    val dirty = target.dirty
    val fieldErrors = target.fieldErrors.mapValues { (_, message) -> message.asString() }
    val fieldOffsets = remember { mutableStateMapOf<EventField, Int>() }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val keyboardVisible = imeBottom > 0
    // The form sits above the home tab bar, so only the part of the keyboard that rises past it is padded.
    var spaceBelow by remember { mutableIntStateOf(0) }
    val keyboardPadding = with(density) { (imeBottom - spaceBelow).coerceAtLeast(0).toDp() }
    val dismissKeyboard = { focusManager.clearFocus(); keyboard?.hide(); Unit }
    val close = { if (dirty && !saving) confirmDiscard = true else onClose() }
    fun Modifier.field(field: EventField) = onGloballyPositioned { fieldOffsets[field] = it.positionInParent().y.toInt() }
    fun submit(status: String) {
        dismissKeyboard()
        onSubmit(status)
    }
    LaunchedEffect(fieldErrors) {
        fieldErrors.keys.firstOrNull()?.let { first ->
            if (first == EventField.END) customEnd = true
            scrollState.animateScrollTo(((fieldOffsets[first] ?: 0) - with(density) { 24.dp.roundToPx() }).coerceAtLeast(0))
        }
    }
    CommunityBackHandler { if (!saving) close() }

    Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
        val rootHeight = coordinates.findRootCoordinates().size.height
        spaceBelow = (rootHeight - (coordinates.positionInRoot().y + coordinates.size.height)).toInt().coerceAtLeast(0)
    }) {
    Good4NestedScaffold(
        modifier = Modifier.padding(bottom = keyboardPadding),
        topBar = {
            Good4TopBar(
                title = if (isNew) stringResource(Res.string.community_yeni_etkinlik) else stringResource(Res.string.community_etkinligi_duzenle),
                navigationIcon = { IconButton(onClick = close, enabled = !saving) { Icon(Icons.Outlined.Close, stringResource(Res.string.notification_close)) } },
                actions = {
                    if (keyboardVisible) TextButton(onClick = dismissKeyboard) { Text(stringResource(Res.string.campus_closet_bitti)) }
                    else TextButton(onClick = { previewing = true }, enabled = !saving) {
                        Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(Res.string.community_onizle))
                    }
                }
            )
        },
        bottomBar = {
            StickyActionBar {
                val message = when {
                    error != null -> error
                    pickerError != null -> pickerError
                    fieldErrors.size == 1 -> fieldErrors.entries.first().let { (field, text) -> stringResource(Res.string.community_field_error, field.label, text) }
                    fieldErrors.isNotEmpty() -> stringResource(Res.string.community_check_fields, fieldErrors.size)
                    else -> null
                }
                if (message != null) Text(message, color = ErrorRed, fontSize = 12.sp)
                else Text(
                    when {
                        canDraft -> stringResource(Res.string.community_taslaklar_ogrencilere_gorunmez_hazir_olunca_yayinlarsin)
                        !dirty -> stringResource(Res.string.community_henuz_bir_degisiklik_yapmadin)
                        else -> stringResource(Res.string.community_degisiklikler_kayitli_ogrencilerin_biletlerinde_de_guncellenir)
                    },
                    color = TextSecondary, fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (canDraft) {
                        OutlinedButton(
                            onClick = { submit("draft") }, enabled = !saving && !coverPicker.preparing,
                            modifier = Modifier.weight(1f).height(StandardButtonHeight), shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.8f))
                        ) { Text(stringResource(Res.string.community_taslak_kaydet), fontWeight = FontWeight.Medium) }
                    }
                    Button(
                        onClick = { submit("published") }, enabled = !saving && !coverPicker.preparing && (canDraft || dirty),
                        modifier = Modifier.weight(1.4f).height(StandardButtonHeight), shape = RoundedCornerShape(16.dp)
                    ) {
                        if (saving) {
                            CircularProgressIndicator(Modifier.size(StandardButtonLoadingIndicatorSize), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            when {
                                saving -> "Kaydediliyor…"
                                canDraft -> stringResource(Res.string.community_yayinla)
                                else -> stringResource(Res.string.campus_closet_save)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(AppBackground).padding(padding)
                .verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CoverField(
                bytes = image, remoteUrl = draft.imageUrl, enabled = !saving, preparing = coverPicker.preparing,
                onPick = coverPicker.open, onView = { viewingCover = true },
                onRemove = { onImageChange(null); onDraftChange(draft.copy(imageUrl = "")) }
            )
            ClosetCard {
                ClosetSectionHeading(stringResource(Res.string.community_etkinlik), Icons.Outlined.Event)
                FormTextField(
                    value = draft.title, label = stringResource(Res.string.campus_closet_baslik), placeholder = stringResource(Res.string.community_orn_tanisma_bulusmasi),
                    error = fieldErrors[EventField.TITLE], enabled = !saving, modifier = Modifier.field(EventField.TITLE)
                ) { onDraftChange(draft.copy(title = it.take(120))) }
                Column(Modifier.field(EventField.CATEGORY), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(Res.string.campus_closet_kategori), color = TextSecondary, fontSize = 12.sp)
                    CategoryChipRow(draft.categoryId, enabled = !saving) { onDraftChange(draft.copy(categoryId = it)) }
                    fieldErrors[EventField.CATEGORY]?.let { FieldError(it) }
                }
                FormTextField(
                    value = draft.description, label = stringResource(Res.string.campus_closet_aciklama), placeholder = stringResource(Res.string.community_etkinlikte_neler_olacak_kimler_katilabilir),
                    error = fieldErrors[EventField.DESCRIPTION], enabled = !saving, singleLine = false,
                    modifier = Modifier.field(EventField.DESCRIPTION)
                ) { onDraftChange(draft.copy(description = it.take(4000))) }
            }
            ClosetCard {
                ClosetSectionHeading(stringResource(Res.string.community_zaman_ve_yer), Icons.Outlined.Schedule)
                Column(Modifier.field(EventField.START), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PickerField(stringResource(Res.string.community_baslangic), Icons.Outlined.CalendarMonth, draft.date.takeIf { it.isNotBlank() }?.let(::formatEventDate),
                            stringResource(Res.string.community_tarih_sec), fieldErrors[EventField.START] != null, !saving, { dateTarget = ScheduleField.START }, Modifier.weight(1.6f))
                        PickerField(stringResource(Res.string.community_saat), Icons.Outlined.Schedule, draft.time.ifBlank { null },
                            stringResource(Res.string.community_sec), fieldErrors[EventField.START] != null, !saving, { timeTarget = ScheduleField.START }, Modifier.weight(1f))
                    }
                    fieldErrors[EventField.START]?.let { FieldError(it) }
                }
                Column(Modifier.field(EventField.END), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(Res.string.community_sure), color = TextSecondary, fontSize = 12.sp)
                    val matched = draft.matchingDuration()
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EventDuration.entries.forEach { option ->
                            val selected = if (option == EventDuration.CUSTOM) customEnd else !customEnd && matched == option
                            DurationChip(option.shortLabel, selected, enabled = !saving && draft.date.isNotBlank() && draft.time.isNotBlank(),
                                modifier = Modifier.weight(if (option == EventDuration.ALL_DAY) 1.4f else 1f)) {
                                if (option == EventDuration.CUSTOM) customEnd = true
                                else { customEnd = false; onDraftChange(draft.withDuration(option)) }
                            }
                        }
                    }
                    if (customEnd) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PickerField(stringResource(Res.string.community_bitis), Icons.Outlined.CalendarMonth, draft.endDate.takeIf { it.isNotBlank() }?.let(::formatEventDate),
                                stringResource(Res.string.community_tarih_sec), fieldErrors[EventField.END] != null, !saving, { dateTarget = ScheduleField.END }, Modifier.weight(1.6f))
                            PickerField(stringResource(Res.string.community_saat), Icons.Outlined.Schedule, draft.endTime.ifBlank { null },
                                stringResource(Res.string.community_sec), fieldErrors[EventField.END] != null, !saving, { timeTarget = ScheduleField.END }, Modifier.weight(1f))
                        }
                    } else if (draft.endDate.isNotBlank() && draft.endTime.isNotBlank()) {
                        Text(stringResource(Res.string.community_end_summary, formatEventDate(draft.endDate), draft.endTime), color = TextSecondary, fontSize = 12.sp)
                    } else if (draft.date.isBlank() || draft.time.isBlank()) {
                        Text(stringResource(Res.string.community_once_baslangici_sec_sure_ona_gore_hesaplanir), color = TextSecondary, fontSize = 12.sp)
                    }
                    fieldErrors[EventField.END]?.let { FieldError(it) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    FormTextField(
                        value = draft.location, label = stringResource(Res.string.community_konum), placeholder = stringResource(Res.string.community_orn_merkezi_kafeterya),
                        error = fieldErrors[EventField.LOCATION], enabled = !saving,
                        modifier = Modifier.weight(1.8f).field(EventField.LOCATION)
                    ) { onDraftChange(draft.copy(location = it.take(200))) }
                    FormTextField(
                        value = draft.capacity.takeIf { it > 0 }?.toString().orEmpty(), label = stringResource(Res.string.community_kontenjan), placeholder = stringResource(Res.string.community_sinirsiz),
                        error = fieldErrors[EventField.CAPACITY], enabled = !saving, number = true,
                        modifier = Modifier.weight(1f).field(EventField.CAPACITY)
                    ) { value -> onDraftChange(draft.copy(capacity = value.filter(Char::isDigit).take(6).toIntOrNull() ?: 0)) }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    }

    SchedulePickerDialogs(draft, dateTarget, timeTarget, onChange = onDraftChange, onCloseDate = { dateTarget = null }, onCloseTime = { timeTarget = null })
    if (viewingCover) {
        (image ?: draft.imageUrl.takeIf { it.isNotBlank() })?.let { PosterViewer(it) { viewingCover = false } }
    }
    if (previewing) {
        ModalBottomSheet(
            onDismissRequest = { previewing = false }, containerColor = AppBackground,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ClosetSectionHeading(stringResource(Res.string.community_ogrencilerin_gorecegi), Icons.Outlined.Visibility, stringResource(Res.string.community_topluluk_sayfasinda_etkinligin_boyle_gorunecek))
                EventPreviewCard(draft, image)
            }
        }
    }
    if (confirmDiscard) {
        Good4ConfirmDialog(
            title = stringResource(Res.string.community_degisiklikler_kaydedilmedi),
            message = stringResource(Res.string.community_cikarsan_bu_formda_yaptigin_degisiklikler_kaybolacak),
            confirmLabel = stringResource(Res.string.community_cik),
            dismissLabel = stringResource(Res.string.campus_closet_cancel),
            onConfirm = { confirmDiscard = false; onClose() },
            onDismiss = { confirmDiscard = false }
        )
    }
}

@Composable
private fun EventPreviewCard(draft: CommunityEntryDto, image: ByteArray?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) {
        Column {
            val cover: Any? = image ?: draft.imageUrl.takeIf { it.isNotBlank() }
            if (cover != null) {
                AsyncImage(
                    model = cover, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(CoverImageSpec.ASPECT_RATIO).background(SurfaceMuted)
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (EventCategory.fromId(draft.categoryId) != null) {
                    Text(EventCategory.labelFor(draft.categoryId), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(draft.title.ifBlank { stringResource(Res.string.community_etkinlik_basligi) }, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    color = if (draft.title.isBlank()) TextSecondary else TextPrimary)
                if (draft.date.isNotBlank()) InfoLine(Icons.Outlined.CalendarMonth, formatEventSchedule(draft))
                if (draft.location.isNotBlank()) InfoLine(Icons.Outlined.LocationOn, draft.location)
                InfoLine(Icons.Outlined.Groups, if (draft.capacity > 0) stringResource(Res.string.community_capacity_people, draft.capacity) else stringResource(Res.string.community_kontenjan_sinirsiz))
                if (draft.description.isNotBlank()) Text(draft.description, fontSize = 14.sp, lineHeight = 20.sp, color = TextSecondary)
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp)) {
                    Text(stringResource(Res.string.community_etkinlige_kayit_ol), modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

/** One scrolling row instead of a wrapped block; the chosen category is brought into view. */
@Composable
private fun CategoryChipRow(selectedId: String, enabled: Boolean, onSelect: (String) -> Unit) {
    val categories = EventCategory.entries
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        val index = categories.indexOfFirst { it.id == selectedId }
        if (index > 0) listState.scrollToItem(index)
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            SmallToggle(category.label, selectedId == category.id, enabled = enabled) { onSelect(category.id) }
        }
    }
}

/** One compact row: an invitation while empty, a 4:5 poster thumbnail with actions once chosen. */
@Composable
private fun CoverField(
    bytes: ByteArray?,
    remoteUrl: String,
    enabled: Boolean,
    preparing: Boolean,
    onPick: () -> Unit,
    onView: () -> Unit,
    onRemove: () -> Unit
) {
    val model: Any? = bytes ?: remoteUrl.takeIf { it.isNotBlank() }
    Surface(
        onClick = if (model == null) onPick else onView, enabled = enabled && !preparing,
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault, border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)), shadowElevation = 1.dp
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (model == null) {
                TiltedIcon(Icons.Outlined.AddPhotoAlternate, PrimaryGreen, size = 32, iconSize = 18)
            } else {
                AsyncImage(
                    model = model, contentDescription = stringResource(Res.string.community_kapak_gorseli), contentScale = ContentScale.Crop,
                    modifier = Modifier.width(64.dp).aspectRatio(CoverImageSpec.ASPECT_RATIO)
                        .clip(RoundedCornerShape(10.dp)).background(SurfaceMuted)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    when {
                        preparing -> stringResource(Res.string.community_gorsel_hazirlaniyor)
                        model == null -> stringResource(Res.string.community_kapak_gorseli_ekle)
                        else -> stringResource(Res.string.community_kapak_gorseli)
                    },
                    fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary
                )
                if (model == null) {
                    Text(stringResource(Res.string.community_instagram_gonderisi_gibi_4_5_dikey_gorsel_onerilir), fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(Res.string.community_degistir), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(enabled = enabled && !preparing, onClick = onPick).padding(vertical = 6.dp))
                        Text(stringResource(Res.string.community_kaldir), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ErrorRed,
                            modifier = Modifier.clickable(enabled = enabled && !preparing, onClick = onRemove).padding(vertical = 6.dp))
                    }
                }
            }
            if (preparing) {
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
            } else if (model == null) {
                Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    label: String,
    placeholder: String,
    error: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    number: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = TextSecondary, maxLines = 1) },
        singleLine = singleLine, minLines = if (singleLine) 1 else 2, maxLines = if (singleLine) 1 else 8,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        shape = RoundedCornerShape(14.dp), colors = managerFieldColors()
    )
}

@Composable
private fun DurationChip(label: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(12.dp), color = SurfaceDefault,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else BorderMuted.copy(alpha = 0.55f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label, fontSize = 13.sp, maxLines = 1,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = when {
                    !enabled -> TextSecondary
                    selected -> MaterialTheme.colorScheme.primary
                    else -> TextPrimary
                }
            )
        }
    }
}

@Composable
private fun FieldError(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
}

@Composable
private fun PickerField(
    label: String,
    icon: ImageVector,
    value: String?,
    placeholder: String,
    isError: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp), shape = RoundedCornerShape(14.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, if (isError) MaterialTheme.colorScheme.error else BorderMuted.copy(alpha = 0.55f))
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, fontSize = 11.sp, color = if (isError) MaterialTheme.colorScheme.error else TextSecondary)
                Text(value ?: placeholder, fontSize = 14.sp, color = if (value == null) TextSecondary else TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---------------------------------------------------------------- Shared pieces

internal enum class ScheduleField { START, END }

private fun utcMillisOf(date: String): Long? = runCatching {
    LocalDate.parse(date).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
private object TodayOrLater : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis >= (utcMillisOf(eventNow().date.toString()) ?: 0L)
    override fun isSelectableYear(year: Int): Boolean = year >= eventNow().year
}

/** Date and time pickers for an event's start or end; moving the start keeps the event's length. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SchedulePickerDialogs(
    draft: CommunityEntryDto,
    dateTarget: ScheduleField?,
    timeTarget: ScheduleField?,
    onChange: (CommunityEntryDto) -> Unit,
    onCloseDate: () -> Unit,
    onCloseTime: () -> Unit
) {
    dateTarget?.let { target ->
        val current = if (target == ScheduleField.END) draft.endDate else draft.date
        val pickerState = key(target) {
            rememberDatePickerState(initialSelectedDateMillis = utcMillisOf(current), selectableDates = TodayOrLater)
        }
        DatePickerDialog(onDismissRequest = onCloseDate, confirmButton = {
            TextButton(enabled = pickerState.selectedDateMillis != null, onClick = {
                pickerState.selectedDateMillis?.let {
                    val picked = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString()
                    onChange(if (target == ScheduleField.END) draft.copy(endDate = picked) else draft.withStart(picked, draft.time))
                }
                onCloseDate()
            }) { Text(stringResource(Res.string.community_sec)) }
        }, dismissButton = { TextButton(onClick = onCloseDate) { Text(stringResource(Res.string.campus_closet_cancel)) } }) { DatePicker(state = pickerState) }
    }
    timeTarget?.let { target ->
        val current = if (target == ScheduleField.END) draft.endTime else draft.time
        val timeState = key(target) {
            rememberTimePickerState(
                initialHour = current.substringBefore(':').toIntOrNull() ?: if (target == ScheduleField.END) 16 else 14,
                initialMinute = current.substringAfter(':', "").toIntOrNull() ?: 0,
                is24Hour = true
            )
        }
        AlertDialog(
            onDismissRequest = onCloseTime,
            title = { Text(if (target == ScheduleField.END) stringResource(Res.string.community_bitis_saati) else stringResource(Res.string.community_baslangic_saati)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val picked = "${timeState.hour.toString().padStart(2, '0')}:${timeState.minute.toString().padStart(2, '0')}"
                    onChange(if (target == ScheduleField.END) draft.copy(endTime = picked) else draft.withStart(draft.date, picked))
                    onCloseTime()
                }) { Text(stringResource(Res.string.community_sec)) }
            },
            dismissButton = { TextButton(onClick = onCloseTime) { Text(stringResource(Res.string.campus_closet_cancel)) } }
        )
    }
}

@Composable
private fun managerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = BorderMuted.copy(alpha = 0.55f),
    focusedContainerColor = SurfaceDefault,
    unfocusedContainerColor = SurfaceDefault,
    disabledContainerColor = SurfaceMuted,
    cursorColor = MaterialTheme.colorScheme.primary
)


@Preview
@Composable
private fun ManagerEventEditorPreview() {
    val initial = CommunityEntryDto()
    ManagerEventEditor(
        target = EventEditorState(null, initial, initial),
        saving = false,
        error = null,
        onClose = {},
        onDraftChange = {},
        onImageChange = {},
        onSubmit = {}
    )
}
