package com.good4.community

import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil3.compose.AsyncImage
import com.good4.campuscloset.TiltedIcon
import com.good4.core.presentation.*
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.ProductImagePicker
import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend
import good4.composeapp.generated.resources.notification_unavailable
import good4.composeapp.generated.resources.community_load_more
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CommunitiesScreen(
    onBack: () -> Unit,
    managerEntryMode: Boolean = false,
    onSwitchToStudent: () -> Unit = onBack,
    viewModel: CommunityViewModel = koinViewModel(),
    initialOrganizationId: String = "",
    initialEventId: String = "",
    initialShowTicket: Boolean = false
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var eventFilter by rememberSaveable { mutableIntStateOf(0) }
    var previewAsStudent by rememberSaveable { mutableStateOf(false) }
    var editor by remember { mutableStateOf<CommunityEntryDto?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var profileEditor by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<CommunityEntry?>(null) }
    var pendingRemoval by remember { mutableStateOf<CommunityEntry?>(null) }
    var admissionEntry by remember { mutableStateOf<CommunityEntry?>(null) }
    var registeredOnly by remember { mutableStateOf(false) }
    var pendingFeaturedEvent by remember { mutableStateOf<CommunityFeaturedEvent?>(null) }
    var reportTarget by remember { mutableStateOf<CommunityEntry?>(null) }
    var pendingBlock by remember { mutableStateOf<Community?>(null) }
    val reviewListState = androidx.compose.foundation.lazy.rememberLazyListState()
    com.good4.review.ReviewFeaturePrompt(
        feature = com.good4.review.ReviewFeature.COMMUNITIES,
        contentReady = !state.loading && state.error == null && (state.communities.isNotEmpty() || state.selected != null),
        isScrolling = reviewListState.isScrollInProgress,
        canPrompt = query.isBlank() && !state.saving && !state.codeGenerating && !state.admissionBusy &&
            !state.reportSending && state.registrationLoadingIds.isEmpty() && state.ticket == null &&
            editor == null && !profileEditor && detail == null && pendingRemoval == null &&
            admissionEntry == null && pendingFeaturedEvent == null && reportTarget == null && pendingBlock == null
    )
    var targetHandled by rememberSaveable(initialEventId) { mutableStateOf(false) }
    val unavailable = org.jetbrains.compose.resources.stringResource(good4.composeapp.generated.resources.Res.string.notification_unavailable)
    LaunchedEffect(initialEventId, state.loading, state.selected?.id) {
        if (initialEventId.isNotBlank() && !targetHandled && !state.loading) {
            previewAsStudent = true
            if (state.selected == null || (initialOrganizationId.isNotBlank() && state.selected?.id != initialOrganizationId)) {
                val target = state.communities.firstOrNull { it.id == initialOrganizationId }
                if (target != null) viewModel.select(target, initialEventId)
                else { targetHandled = true; viewModel.reportError(unavailable) }
            } else {
                targetHandled = true
                val event = state.entries.firstOrNull { it.id == initialEventId && it.data.status == "published" }
                if (event == null) viewModel.reportError(unavailable)
                else { detail = event; if (initialShowTicket && event.id in state.registeredEventIds) viewModel.showTicket(event) }
            }
        }
    }
    val community = state.selected
    val managerView = state.canManage && !previewAsStudent
    val isV2 = AppEnvironment.firebaseBackend == FirebaseBackend.V2
    val studentView = isV2 && !state.canManage && !previewAsStudent
    var studentFilter by rememberSaveable(community?.id) { mutableStateOf(StudentEventFilter.UPCOMING) }
    var now by remember { mutableStateOf(eventNow()) }
    val featuredEvents = if (studentView) {
        filterFeaturedCommunityEvents(state.featuredEvents, "", state.followedOnly, state.followedCommunityIds)
    } else state.filteredFeaturedEvents
    var today by remember { mutableStateOf(currentCampusDate()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            today = currentCampusDate()
            now = eventNow()
        }
    }
    LaunchedEffect(state.loading, state.communities, state.selected, today) {
        if (!state.loading && community == null) {
            viewModel.refreshFeaturedEvents(today)
        }
    }
    LaunchedEffect(pendingFeaturedEvent?.entry?.id, state.selected?.id, state.loading, state.entries) {
        val pending = pendingFeaturedEvent ?: return@LaunchedEffect
        if (!state.loading) {
            val selected = state.selected
            when {
                selected?.id == pending.community.id -> {
                    detail = state.entries.firstOrNull { it.id == pending.entry.id }
                    pendingFeaturedEvent = null
                }
                selected == null || selected.id != pending.community.id -> pendingFeaturedEvent = null
            }
        }
    }
    LifecycleResumeEffect(community?.id, state.canManage) {
        viewModel.resumeUpdates()
        if (community == null) {
            viewModel.refreshCommunities()
            viewModel.refreshFeaturedEvents(today, force = true)
        }
        onPauseOrDispose { viewModel.pauseUpdates() }
    }
    if (isV2 && managerView && community != null) {
        CommunityManagerFlow(
            state = state,
            community = community,
            viewModel = viewModel,
            onExit = { if (managerEntryMode) onBack() else viewModel.back() },
            onPreviewStudent = { previewAsStudent = true }
        )
        return
    }
    val studentDetail = detail?.takeIf { studentView && it.data.kind == "event" }
        ?.let { selectedEntry -> state.entries.firstOrNull { it.id == selectedEntry.id } ?: selectedEntry }
    if (studentView) DisposableEffect(studentDetail?.id) {
        viewModel.setStudentEventVisible(studentDetail?.id)
        onDispose { viewModel.setStudentEventVisible(null) }
    }
    if (studentDetail != null && community != null) {
        CommunityBackHandler { detail = null }
        CommunityStudentEventPage(
            community = community, entry = studentDetail, state = state, now = now,
            onBack = { detail = null },
            onToggleRegistration = { viewModel.toggleRegistration(studentDetail) },
            onShowTicket = { viewModel.showTicket(studentDetail) },
            onReport = { viewModel.clearReportStatus(); reportTarget = studentDetail },
            onDismissError = viewModel::clearError,
            onDismissRegistrationFeedback = viewModel::clearRegistrationFeedback
        )
    } else {
        Good4NestedScaffold(
            topBar = {
                Good4TopBar(
                    title = when {
                        community == null -> "Topluluklar"
                        managerView -> stringResource(Res.string.community_toplulugumu_yonet)
                        studentView -> "Topluluk"
                        else -> community.data.name
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (previewAsStudent) {
                                    previewAsStudent = false
                                } else if (community == null) {
                                    onBack()
                                } else if (managerEntryMode) {
                                    onBack()
                                } else {
                                    viewModel.back()
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.campus_closet_back))
                        }
                    },
                    actions = {
                        if (community != null && !state.canManage && !previewAsStudent) {
                            var menuOpen by remember(community.id) { mutableStateOf(false) }
                            Box {
                                RoundIconAction(Icons.Outlined.MoreVert, stringResource(Res.string.community_diger_islemler)) { menuOpen = true }
                                DropdownMenu(
                                    expanded = menuOpen,
                                    onDismissRequest = { menuOpen = false },
                                    containerColor = SurfaceDefault
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.community_toplulugu_engelle), color = ErrorRed) },
                                        leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null, tint = ErrorRed) },
                                        onClick = {
                                            menuOpen = false
                                            pendingBlock = community
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                state = reviewListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (community == null) {
                    if (studentView) {
                        item(key = "community-search") {
                            CommunitySearchField(query = query, onQueryChange = { query = it })
                        }
                        item(key = "community-follow-tabs") {
                            CommunityDiscoveryTabs(
                                followedOnly = state.followedOnly,
                                enabled = !state.loading,
                                onSelect = viewModel::setFollowedOnly
                            )
                        }
                        val followingPending = state.followedOnly &&
                            (state.followingLoading || !state.followingLoaded || state.followingError != null)
                        val filtered = state.communities.filter {
                            (!state.followedOnly || it.id in state.followedCommunityIds) &&
                                (it.data.name.contains(query, ignoreCase = true) ||
                                    it.data.description.contains(query, ignoreCase = true))
                        }
                        if (query.isBlank() && !followingPending) {
                            item(key = "student-featured-heading") {
                                Text(stringResource(Res.string.community_yaklasan_etkinlikler), fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary, modifier = Modifier.padding(top = 4.dp))
                            }
                            item(key = "student-featured-events") {
                                when {
                                    featuredEvents.isNotEmpty() -> StudentPosterRow(featuredEvents.take(10)) { featured ->
                                        pendingFeaturedEvent = featured
                                        viewModel.select(featured.community, featured.entry.id)
                                    }
                                    state.featuredEventsError != null -> NoticeCard(
                                        stringResource(Res.string.community_featured_events_load_error), ErrorRed, actionLabel = stringResource(Res.string.campus_closet_retry),
                                        onAction = { viewModel.refreshFeaturedEvents(today, force = true) })
                                    state.featuredEventsLoading || state.loading ->
                                        Text(stringResource(Res.string.community_etkinlikler_yukleniyor), fontSize = 13.sp, color = TextSecondary)
                                    else -> EmptyCommunityContent(
                                        title = stringResource(Res.string.community_yaklasan_etkinlik_yok),
                                        subtitle = stringResource(Res.string.community_yeni_etkinlikler_yayinlandiginda_burada_gorunecek),
                                    )
                                }
                            }
                        }
                        item(key = "community-list-heading") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (state.followedOnly) stringResource(Res.string.community_takip_ettigin_topluluklar) else stringResource(Res.string.community_topluluklari_kesfet),
                                    fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!state.loading && !followingPending) {
                                    Text(stringResource(Res.string.community_count, filtered.size), fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        }
                        if (followingPending) {
                            item(key = "community-follow-status") {
                                if (state.followingError != null) {
                                    NoticeCard(state.followingError.orEmpty(), ErrorRed, actionLabel = stringResource(Res.string.campus_closet_retry),
                                        onAction = { viewModel.refreshFollowing(force = true) })
                                } else {
                                    Text(stringResource(Res.string.community_takip_ettigin_topluluklar_yukleniyor), color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(filtered, key = { "community-${it.id}" }) { item ->
                                CommunityListCard(community = item, studentStyle = true, onClick = {
                                    tab = 0
                                    viewModel.select(item)
                                })
                            }
                            if (!state.loading && state.error == null && filtered.isEmpty()) {
                                item(key = "community-list-empty") {
                                    EmptyCommunityContent(
                                        title = when {
                                            query.isNotBlank() -> stringResource(Res.string.community_aramana_uygun_topluluk_bulunamadi)
                                            state.followedOnly -> stringResource(Res.string.community_henuz_bir_toplulugu_takip_etmiyorsun)
                                            else -> stringResource(Res.string.community_topluluklar_yakinda_burada)
                                        },
                                        subtitle = when {
                                            query.isNotBlank() -> stringResource(Res.string.community_farkli_bir_kelimeyle_tekrar_deneyebilirsin)
                                            state.followedOnly -> stringResource(Res.string.community_tumu_sekmesinden_topluluklari_kesfedip_takip_edebilirsin)
                                            else -> stringResource(Res.string.community_yeni_topluluklar_eklendikce_burada_kesfedebilirsin)
                                        }
                                    )
                                }
                            }
                        }
                        if (state.blockedCommunityIds.isNotEmpty()) item(key = "community-unblock") {
                            TextButton(onClick = viewModel::unblockAllCommunities, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(Res.string.community_unblock_count, state.blockedCommunityIds.size),
                                    fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                    } else {
                        if (isV2) item(key = "community-event-filters") {
                            CommunityEventFilterRow(
                                selectedId = state.selectedCategoryId,
                                enabled = !state.loading,
                                onSelect = viewModel::selectCategory,
                                followedOnly = state.followedOnly,
                                onFollowedOnlyChange = viewModel::setFollowedOnly,
                                studentStyle = studentView,
                            )
                        }
                        item(key = "featured-community-events") {
                            if (state.followedOnly && (state.followingLoading || state.followingError != null || !state.followingLoaded)) {
                                CommunityEventsBannerPlaceholder(
                                    loading = state.followingLoading,
                                    hasError = state.followingError != null,
                                    message = state.followingError ?: stringResource(Res.string.community_takip_ettiginiz_topluluklar_yukleniyor),
                                    onRetry = { viewModel.refreshFollowing(force = true) },
                                )
                            } else if (featuredEvents.isNotEmpty()) {
                                FeaturedCommunityEventsCarousel(
                                    events = featuredEvents,
                                    onEventClick = { featured ->
                                        pendingFeaturedEvent = featured
                                        viewModel.select(featured.community, featured.entry.id)
                                    }
                                )
                            } else if ((state.selectedCategoryId.isNotEmpty() || state.followedOnly)
                                && !state.featuredEventsLoading && state.featuredEventsError == null && !state.loading) {
                                EmptyCommunityContent(
                                    title = stringResource(Res.string.community_filtrelere_uygun_etkinlik_bulunamadi),
                                    subtitle = if (state.followedOnly && state.followedCommunityIds.isEmpty())
                                        stringResource(Res.string.community_takip_ettigin_bir_topluluk_yok_topluluklari_kesfedip_takip_edebil)
                                    else stringResource(Res.string.community_farkli_bir_kategori_secebilir_veya_filtreleri_kaldirabilirsin),
                                )
                            } else if (AppEnvironment.isDebug && state.selectedCategoryId.isEmpty() && !state.followedOnly) {
                                DemoCommunityEventsCarousel()
                            } else {
                                CommunityEventsBannerPlaceholder(
                                    loading = state.featuredEventsLoading,
                                    hasError = state.featuredEventsError != null,
                                    onRetry = { viewModel.refreshFeaturedEvents(today, force = true) }
                                )
                            }
                        }
                        item {
                            CommunitySearchField(
                                query = query,
                                onQueryChange = { query = it }
                            )
                        }
                        val filtered = state.communities.filter {
                            it.data.name.contains(query, ignoreCase = true) ||
                                it.data.description.contains(query, ignoreCase = true)
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(Res.string.community_kesfet),
                                    fontSize = 19.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                if (!state.loading) {
                                    Text(
                                        text = stringResource(Res.string.community_count, filtered.size),
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        items(filtered, key = { it.id }) { item ->
                            CommunityListCard(
                                community = item,
                                studentStyle = studentView,
                                onClick = {
                                    tab = 0
                                    viewModel.select(item)
                                }
                            )
                        }
                        if (state.blockedCommunityIds.isNotEmpty()) {
                            item {
                                TextButton(
                                    onClick = viewModel::unblockAllCommunities,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        stringResource(Res.string.community_unblock_count, state.blockedCommunityIds.size),
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        if (!state.loading && state.error == null && filtered.isEmpty()) {
                            item {
                                EmptyCommunityContent(
                                    title = if (query.isBlank()) stringResource(Res.string.community_topluluklar_yakinda_burada) else stringResource(Res.string.community_aramana_uygun_topluluk_bulunamadi),
                                    subtitle = if (query.isBlank()) stringResource(Res.string.community_yeni_topluluklar_eklendikce_burada_kesfedebilirsin) else stringResource(Res.string.community_farkli_bir_kelimeyle_tekrar_deneyebilirsin)
                                )
                            }
                        }
                    }
                } else {
                    if (managerView) {
                        item {
                            ManagerCommunityIdentity(
                                community = community,
                                onEditProfile = { profileEditor = true }
                            )
                        }
                        item {
                            val upcomingEvents = state.entries.count {
                                it.data.kind == "event" && it.data.status == "published" && it.data.lastDate() >= today
                            }
                            val currentMonth = today.take(7)
                            CommunityManagementActions(
                                upcomingEventCount = upcomingEvents,
                                registrationCount = state.registrationsByEntry.values.sumOf { it.size },
                                monthlyAttendanceCount = state.entries
                                    .filter { it.data.kind == "event" && it.data.date.startsWith(currentMonth) }
                                    .sumOf { state.attendanceByEntry[it.id]?.size ?: 0 },
                                followerCount = state.followerCount,
                                onAddEvent = {
                                    editingId = null
                                    editor = CommunityEntryDto()
                                },
                                onAddCoupon = {
                                    editingId = null
                                    editor = CommunityEntryDto(kind = "coupon", status = "pending")
                                },
                                onEditProfile = { profileEditor = true },
                                onSwitchToStudent = { previewAsStudent = true }
                            )
                        }
                    } else {
                        item {
                            CommunityDetailHeader(
                                community = community,
                                canManage = state.canManage,
                                previewAsStudent = previewAsStudent,
                                isFollowing = state.isFollowing,
                                followLoading = state.followLoading,
                                onFollowClick = viewModel::toggleFollow
                            )
                        }
                        if (previewAsStudent) {
                            item {
                                StudentPreviewBanner(onReturnToManagement = { previewAsStudent = false })
                            }
                        }
                    }
                    if (!studentView) item {
                        CommunityTabs(
                            selectedTab = tab,
                            onTabSelected = { tab = it },
                            manager = managerView
                        )
                    }
                    if (managerView && tab == 0) item {
                        ManagerEventFilters(
                            selected = eventFilter,
                            onSelected = { eventFilter = it }
                        )
                    }
                    if (studentView) item {
                        StudentEventFilters(state.entries, state.registeredEventIds, now, studentFilter) { studentFilter = it }
                    }
                    if (!studentView && !managerView && tab == 0) item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isV2) EventCategorySelector(state.selectedCategoryId, true, !state.loading, viewModel::selectCategory)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CommunityEventFilterChip(!registeredOnly, true, stringResource(Res.string.community_tum_etkinlikler), onClick = { registeredOnly = false })
                                CommunityEventFilterChip(registeredOnly, true, stringResource(Res.string.community_etkinlik_kayitlarim), onClick = { registeredOnly = true })
                            }
                        }
                    }
                    val entries = if (studentView) studentEventsFor(state.entries, studentFilter, state.registeredEventIds, now)
                    else state.entries.filter { entry ->
                        val kindMatches = entry.data.kind == (if (tab == 0) "event" else "coupon")
                        val visibilityMatches = if (managerView) true else entry.data.status == "published"
                        val registrationMatches = managerView || tab != 0 || !registeredOnly || entry.id in state.registeredEventIds
                        val categoryMatches = managerView || tab != 0 || matchesEventCategory(entry.data.categoryId, state.selectedCategoryId)
                        val managerFilterMatches = !managerView || tab != 0 || when (eventFilter) {
                            0 -> entry.data.status == "published" && entry.data.lastDate() >= today
                            1 -> entry.data.status == "draft"
                            else -> entry.data.status == "cancelled" || (entry.data.status == "published" && entry.data.lastDate() < today)
                        }
                        kindMatches && visibilityMatches && registrationMatches && categoryMatches && managerFilterMatches
                    }.let { filtered ->
                        if (managerView && tab == 0 && eventFilter == 2) filtered.sortedByDescending { it.data.date + it.data.time }
                        else filtered
                    }
                    if (!studentView && tab == 1 && !managerView && entries.isNotEmpty()) {
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(end = 4.dp)
                            ) {
                                items(entries, key = { it.id }) { entry ->
                                    StudentCouponCard(entry = entry, onClick = { detail = entry })
                                }
                            }
                        }
                    } else {
                        items(entries, key = { it.id }) { entry ->
                            if (studentView) StudentEventRow(entry, entry.id in state.registeredEventIds, now) { detail = entry }
                            else CommunityEntryCard(
                                entry = entry,
                                registrationCount = state.registrationsByEntry[entry.id]?.size,
                                attendanceCount = state.attendanceByEntry[entry.id]?.size,
                                canManage = managerView,
                                onManageAttendees = {
                                    viewModel.clearAdmissionMessage()
                                    admissionEntry = entry
                                },
                                onEdit = {
                                    editingId = entry.id
                                    editor = entry.data
                                },
                                onUnpublish = { pendingRemoval = entry },
                                onClick = { detail = entry }
                            )
                        }
                    }
                    if (!state.loading && state.error == null && entries.isEmpty()) {
                        item {
                            val managerEventTitle = when (eventFilter) {
                                0 -> stringResource(Res.string.community_yaklasan_etkinlik_yok)
                                1 -> stringResource(Res.string.community_kaydedilmis_taslak_yok)
                                else -> stringResource(Res.string.community_gecmis_etkinlik_yok)
                            }
                            val managerEventSubtitle = when (eventFilter) {
                                0 -> stringResource(Res.string.community_yeni_bir_etkinlik_olusturdugunda_burada_gorunecek)
                                1 -> stringResource(Res.string.community_hazirlamaya_ara_verdigin_etkinlikleri_taslak_olarak_kaydedebilirs)
                                else -> stringResource(Res.string.community_tamamlanan_ve_yayindan_kaldirilan_etkinlikler_burada_tutulur)
                            }
                            EmptyCommunityContent(
                                title = if (studentView) studentFilter.emptyTitle else if (managerView && tab == 0) managerEventTitle else if (tab == 0 && (state.selectedCategoryId.isNotEmpty() || registeredOnly)) stringResource(Res.string.community_filtrelere_uygun_etkinlik_bulunamadi) else if (tab == 0) stringResource(Res.string.community_henuz_etkinlik_yok) else stringResource(Res.string.community_henuz_kupon_yok),
                                subtitle = if (studentView) studentFilter.emptySubtitle else if (managerView && tab == 0) managerEventSubtitle else if (tab == 0 && (state.selectedCategoryId.isNotEmpty() || registeredOnly)) stringResource(Res.string.community_kategori_veya_kayit_filtresini_degistirerek_tekrar_deneyebilirsin) else if (tab == 0) stringResource(Res.string.community_yeni_etkinlikler_burada_gorunecek) else stringResource(Res.string.community_toplulugun_firsatlari_burada_yer_alacak),
                                coupon = !studentView && tab == 1
                            )
                        }
                    }
                }
                if (community != null && !state.loading && state.entriesCursor != null) item(key = "load-more") {
                    TextButton(onClick = viewModel::loadMoreEntries, modifier = Modifier.fillMaxWidth()) {
                        Text(org.jetbrains.compose.resources.stringResource(good4.composeapp.generated.resources.Res.string.community_load_more))
                    }
                }
                if (state.loading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                state.error?.let { error ->
                    item {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { if (community == null) viewModel.load() else viewModel.select(community) }) {
                            Text(stringResource(Res.string.campus_closet_retry))
                        }
                    }
                }
            }
        }
    }
    if (editor != null && community != null) {
        EntryEditor(initial = editor!!, editing = editingId != null, businesses = state.businesses, saving = state.saving, error = state.error, onDismiss = { if (!state.saving) { editor = null; viewModel.clearError() } }, onSave = { draft, image -> viewModel.save(editingId, draft, image) { editor = null } })
    }
    if (profileEditor && community != null) {
        CommunityProfileEditor(community.data, state.saving, state.error, { if (!state.saving) { profileEditor = false; viewModel.clearError() } }) { data, logo, cover -> viewModel.updateProfile(data, logo, cover) { profileEditor = false } }
    }
    state.ticket?.let { ticket ->
        val entry = state.entries.firstOrNull { it.id == state.ticketEventId }
        if (community != null && entry != null) {
            if (studentView) EventTicketDialog(
                community.id, entry, ticket, viewModel::closeTicket,
                onCancelRegistration = if (!entry.data.hasEnded(now)) ({ viewModel.toggleRegistration(entry) }) else null,
                registrationBusy = entry.id in state.registrationLoadingIds,
                error = state.error
            )
            else LegacyEventTicketDialog(community.id, entry, ticket, viewModel::closeTicket)
        }
    }
    admissionEntry?.let { entry ->
        EventAdmissionScreen(entry, state.registrationsByEntry[entry.id].orEmpty(), state.attendanceByEntry[entry.id].orEmpty(),
            state.admissionBusy, state.admissionMessage, state.error,
            onDismiss = { admissionEntry = null; viewModel.clearAdmissionMessage() },
            onScanned = { viewModel.admit(entry, scanned = it) }, onError = viewModel::reportError,
            onAdmit = { userId, undo -> viewModel.admit(entry, userId = userId, undo = undo) })
    }
    detail?.takeUnless { studentView && it.data.kind == "event" }?.let { entry ->
        CommunityEntryDetailDialog(
            entry = entry,
            canManage = managerView,
            saving = state.saving,
            generatedCode = state.generatedCouponCode.takeIf { state.generatedCouponEntryId == entry.id },
            codeGenerating = state.codeGenerating,
            registrations = state.registrationsByEntry[entry.id].orEmpty(),
            registered = entry.id in state.registeredEventIds,
            registrationLoading = entry.id in state.registrationLoadingIds,
            error = state.error,
            onDismiss = { detail = null },
            onEdit = {
                editingId = entry.id
                editor = entry.data
                detail = null
            },
            onUnpublish = {
                detail = null
                pendingRemoval = entry
            },
            onCreateCode = { viewModel.createCouponCode(entry) },
            onToggleRegistration = { viewModel.toggleRegistration(entry) },
            onShowTicket = { viewModel.showTicket(entry) },
            onManageAttendees = { detail = null; viewModel.clearAdmissionMessage(); admissionEntry = entry },
            onReport = { viewModel.clearReportStatus(); reportTarget = entry }
        )
    }
    reportTarget?.let { entry ->
        ReportContentDialog(
            title = entry.data.title,
            sending = state.reportSending,
            sent = state.reportSentEntryId == entry.id,
            error = state.reportError,
            onDismiss = { if (!state.reportSending) { reportTarget = null; viewModel.clearReportStatus() } },
            onSubmit = { reason, details -> viewModel.reportEntry(entry, reason, details) }
        )
    }
    pendingBlock?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingBlock = null },
            title = { Text(stringResource(Res.string.community_toplulugu_engelle)) },
            text = {
                Text(stringResource(Res.string.community_block_message, target.data.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingBlock = null
                    detail = null
                    viewModel.blockCommunity(target)
                }) { Text(stringResource(Res.string.campus_closet_engelle), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingBlock = null }) { Text(stringResource(Res.string.campus_closet_cancel)) } }
        )
    }
    pendingRemoval?.let { entry ->
        EntryRemovalConfirmation(
            entry = entry,
            saving = state.saving,
            onDismiss = { if (!state.saving) pendingRemoval = null },
            onConfirm = {
                viewModel.cancel(entry.id) { pendingRemoval = null }
            }
        )
    }
}

@Composable
private fun CommunityDiscoveryTabs(followedOnly: Boolean, enabled: Boolean, onSelect: (Boolean) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = SurfaceMuted) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(false to stringResource(Res.string.campus_closet_all), true to stringResource(Res.string.community_takip_ettiklerim)).forEach { (followed, label) ->
                val selected = followedOnly == followed
                Surface(
                    onClick = { onSelect(followed) }, enabled = enabled,
                    modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(12.dp),
                    color = if (selected) SurfaceDefault else Color.Transparent,
                    border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary)
                    }
                }
            }
        }
    }
}

/** Small 4:5 posters, about two and a half on screen, scrolled by hand. */
@Composable
private fun StudentPosterRow(events: List<CommunityFeaturedEvent>, onEventClick: (CommunityFeaturedEvent) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth * 0.40f
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(events, key = { it.entry.id }) { event ->
                val data = event.entry.data
                Column(
                    modifier = Modifier.width(cardWidth).clip(RoundedCornerShape(14.dp)).clickable { onEventClick(event) },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (data.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = data.imageUrl, contentDescription = "${data.title} etkinlik afişi", contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(CoverImageSpec.ASPECT_RATIO)
                                .clip(RoundedCornerShape(14.dp)).background(PistachioGreen)
                        )
                    } else {
                        EventDateTile(data, Modifier.fillMaxWidth().aspectRatio(CoverImageSpec.ASPECT_RATIO))
                    }
                    Column(Modifier.padding(horizontal = 2.dp)) {
                        Text(data.title, color = TextPrimary, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatEventShort(data), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** Instagram-style 4:5 posters in a row, with the next one peeking in from the edge. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedCommunityEventsCarousel(
    events: List<CommunityFeaturedEvent>,
    onEventClick: (CommunityFeaturedEvent) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { events.size })
    val dragged by pagerState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(events.size, dragged) {
        if (events.size > 1 && !dragged) {
            while (true) {
                delay(5_000)
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % events.size)
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth * 0.54f
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(cardWidth),
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.Top
        ) { page ->
            val event = events[page]
            FeaturedCommunityEventCard(event = event, width = cardWidth, onClick = { onEventClick(event) })
        }
    }
}

private data class DemoCommunitySlide(
    val title: String,
    val subtitle: String,
    val startColor: Color,
    val endColor: Color
)

private val demoCommunitySlides = listOf(
    DemoCommunitySlide("Kampüs Buluşması", "Tanışma ve sohbet", Color(0xFF006C4C), Color(0xFF17A579)),
    DemoCommunitySlide("Tasarım Atölyesi", "Birlikte üretelim", Color(0xFF184B83), Color(0xFF4A8DCF)),
    DemoCommunitySlide("Sahne Gecesi", "Kampüste sanat", Color(0xFF4B6400), Color(0xFF8DAA37))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DemoCommunityEventsCarousel() {
    val pagerState = rememberPagerState(pageCount = { demoCommunitySlides.size })
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4_500)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % demoCommunitySlides.size)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().aspectRatio(12f / 5f)
        ) { page ->
            DemoCommunityEventCard(demoCommunitySlides[page], page + 1)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(demoCommunitySlides.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(if (index == currentPage) 18.dp else 6.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(if (index == currentPage) MaterialTheme.colorScheme.primary else BorderMuted)
                )
            }
        }
    }
}

@Composable
private fun DemoCommunityEventCard(slide: DemoCommunitySlide, number: Int) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(18.dp),
        color = slide.startColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(slide.startColor, slide.endColor))
            )
        ) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .offset(x = 46.dp)
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            )
            Column(
                modifier = Modifier.align(Alignment.CenterStart).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "ÖRNEK ETKİNLİK 0$number",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = slide.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = slide.subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun CommunityEventsBannerPlaceholder(
    loading: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    message: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(12f / 5f),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceMuted,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = if (!loading && !hasError && message == null)
                        stringResource(Res.string.community_yaklasan_etkinlik_yok)
                    else stringResource(Res.string.community_yaklasan_etkinlikler),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = message ?: when {
                        hasError -> stringResource(Res.string.community_featured_events_load_error)
                        loading -> stringResource(Res.string.community_etkinlikler_yukleniyor)
                        else -> stringResource(Res.string.community_yeni_etkinlikler_yayinlandiginda_burada_gorunecek)
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                if (hasError) {
                    TextButton(onClick = onRetry) { Text(stringResource(Res.string.campus_closet_retry), color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
private fun FeaturedCommunityEventCard(event: CommunityFeaturedEvent, width: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val entry = event.entry.data
    Column(
        modifier = Modifier.width(width).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().aspectRatio(CoverImageSpec.ASPECT_RATIO),
            shape = RoundedCornerShape(18.dp),
            color = PistachioGreen,
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.18f))
        ) {
            AsyncImage(
                model = entry.imageUrl,
                contentDescription = "${entry.title} etkinlik afişi",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.title, color = TextPrimary, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatEventShort(entry), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(event.community.data.name, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun currentCampusDate(): String =
    Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Istanbul")).date.toString()

@Composable
private fun CommunitySearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Topluluk ara", color = TextSecondary) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary) },
        trailingIcon = if (query.isNotBlank()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Aramayı temizle", tint = TextSecondary)
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceDefault,
            unfocusedContainerColor = SurfaceDefault,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = BorderMuted.copy(alpha = 0.35f)
        )
    )
}

@Composable
private fun CommunityListCard(community: Community, studentStyle: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = if (studentStyle) 0.55f else 0.18f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (studentStyle && community.data.logoUrl.isBlank()) {
                TiltedIcon(Icons.Outlined.Groups, CommunityAccent)
            } else {
                CommunityLogo(community.data.logoUrl, size = 54.dp)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = community.data.name,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                val supportingText = community.data.description.ifBlank { community.data.university }
                if (supportingText.isNotBlank()) {
                    Text(
                        text = supportingText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CommunityDetailHeader(
    community: Community,
    canManage: Boolean,
    previewAsStudent: Boolean,
    isFollowing: Boolean,
    followLoading: Boolean,
    onFollowClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = community.data.name,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (community.data.university.isNotBlank()) {
                        Text(
                            text = community.data.university,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                if (community.data.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = community.data.logoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    TiltedIcon(Icons.Outlined.Groups, CommunityAccent, size = 40, iconSize = 22)
                }
            }
            if (community.data.description.isNotBlank()) {
                CollapsibleDescription(community.data.description, key = community.id)
            }
            if (!canManage) {
                if (isFollowing) {
                    OutlinedButton(
                        onClick = onFollowClick,
                        enabled = !followLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (followLoading) "Kaydediliyor…" else "Takip ediliyor", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onFollowClick,
                        enabled = !followLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(if (followLoading) "Kaydediliyor…" else "Takip et", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (canManage && previewAsStudent) {
                Text(
                    "Bu alan yalnızca önizlemedir; kendi topluluğunu takip edemezsin.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ManagerCommunityIdentity(
    community: Community,
    onEditProfile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.22f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommunityLogo(community.data.logoUrl, size = 56.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("YÖNETİCİ PANELİ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(community.data.name, fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (community.data.university.isNotBlank()) {
                    Text(community.data.university, fontSize = 13.sp, color = TextSecondary, maxLines = 1)
                }
                Text("Yönetim yetkin aktif", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEditProfile) {
                Icon(Icons.Outlined.ManageAccounts, contentDescription = "Topluluk bilgilerini düzenle", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CommunityManagementActions(
    upcomingEventCount: Int,
    registrationCount: Int,
    monthlyAttendanceCount: Int,
    followerCount: Int?,
    onAddEvent: () -> Unit,
    onAddCoupon: () -> Unit,
    onEditProfile: () -> Unit,
    onSwitchToStudent: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.22f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Genel bakış", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("Topluluğunun güncel durumunu ve etkinlik hareketlerini buradan takip et.", fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ManagementStat("Takipçi", followerCount?.toString() ?: "…", Modifier.weight(1f))
                ManagementStat("Yaklaşan etkinlik", upcomingEventCount.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ManagementStat("Toplam kayıt", registrationCount.toString(), Modifier.weight(1f))
                ManagementStat("Bu ay gelen", monthlyAttendanceCount.toString(), Modifier.weight(1f))
            }
            HorizontalDivider(color = BorderMuted.copy(alpha = 0.25f))
            Text("Hızlı işlemler", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddEvent,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Etkinlik ekle")
                }
                OutlinedButton(
                    onClick = onAddCoupon,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Text("Kupon ekle")
                }
            }
            TextButton(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Topluluk bilgilerini düzenle")
            }
            TextButton(
                onClick = onSwitchToStudent,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("Öğrenci görünümünü aç")
            }
        }
    }
}

@Composable
private fun StudentPreviewBanner(onReturnToManagement: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PistachioGreen.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Öğrenci görünümü", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("Yayınlanan içeriklerin öğrencilerdeki görünümü", fontSize = 12.sp, color = TextSecondary)
            }
            TextButton(onClick = onReturnToManagement) { Text("Yönetime dön") }
        }
    }
}

@Composable
private fun ManagerEventFilters(selected: Int, onSelected: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(listOf("Yaklaşanlar", "Taslaklar", "Geçmiş")) { label ->
            val index = when (label) {
                "Yaklaşanlar" -> 0
                "Taslaklar" -> 1
                else -> 2
            }
            FilterChip(
                selected = selected == index,
                onClick = { onSelected(index) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun ManagementStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = PistachioGreen.copy(alpha = 0.42f)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 13.dp), horizontalAlignment = Alignment.Start) {
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 11.sp, lineHeight = 14.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun CommunityTabs(selectedTab: Int, onTabSelected: (Int) -> Unit, manager: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceMuted
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val labels = if (manager) listOf("Etkinlik yönetimi", "Kupon yönetimi") else listOf("Etkinlikler", "Kuponlar")
            labels.forEachIndexed { index, label ->
                Surface(
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = if (selectedTab == index) SurfaceDefault else androidx.compose.ui.graphics.Color.Transparent,
                    shadowElevation = if (selectedTab == index) 1.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityEntryCard(
    entry: CommunityEntry,
    registrationCount: Int? = null,
    attendanceCount: Int? = null,
    canManage: Boolean = false,
    onManageAttendees: () -> Unit = {},
    onEdit: () -> Unit = {},
    onUnpublish: () -> Unit = {},
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.18f))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (entry.data.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = entry.data.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(13.dp),
                        color = PistachioGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (entry.data.kind == "coupon") Icons.Outlined.LocalOffer else Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.data.title, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(if (entry.data.kind == "event") formatEventSchedule(entry.data) else "${entry.data.date} ${entry.data.time}".trim(), fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    if (canManage) {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = when (entry.data.status) {
                                "published" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                "draft" -> PistachioGreen
                                else -> MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                            }
                        ) {
                            Text(
                                text = when (entry.data.status) {
                                    "published" -> "Yayında"
                                    "draft" -> "Taslak"
                                    "pending" -> "Onayda"
                                    else -> "Kaldırıldı"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (entry.data.status == "cancelled") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (entry.data.kind == "event" && registrationCount != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("$registrationCount kayıtlı · ${attendanceCount?.toString() ?: "…"} giriş yaptı", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    }
                    if (entry.data.capacity > 0) {
                        Text(
                            "${entry.data.capacity} kişilik kontenjan · %${(registrationCount * 100 / entry.data.capacity).coerceAtMost(100)} dolu",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(entry.data.location, fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary)
                }
                if (entry.data.status != "published") {
                    Text(
                        text = if (entry.data.status == "pending") "Onay bekliyor" else "İptal edildi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
                if (canManage) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (entry.data.kind == "event") {
                            Button(
                                onClick = onManageAttendees,
                                enabled = entry.data.status == "published",
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Katılımcılar · QR", fontSize = 12.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Düzenle", fontSize = 12.sp)
                        }
                    }
                    if (entry.data.status != "cancelled") {
                        OutlinedButton(
                            onClick = onUnpublish,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                if (entry.data.status == "draft") "Taslağı kaldır" else "Yayından kaldır",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryRemovalConfirmation(
    entry: CommunityEntry,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isDraft = entry.data.status == "draft"
    val isCoupon = entry.data.kind == "coupon"
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
            shape = RoundedCornerShape(22.dp),
            color = SurfaceDefault,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    when {
                        isDraft -> "Taslak kaldırılsın mı?"
                        isCoupon -> "Kupon kaldırılsın mı?"
                        else -> "Etkinlik yayından kaldırılsın mı?"
                    },
                    fontSize = 20.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(entry.data.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    if (isDraft) {
                        "Bu taslak aktif listeden çıkarılacak ve Geçmiş bölümünde saklanacak."
                    } else if (isCoupon) {
                        "Öğrenciler artık bu kuponu göremeyecek ve yeni kullanım kodu oluşturulamayacak."
                    } else {
                        "Öğrenciler artık bu etkinliği göremeyecek ve yeni kayıt alınmayacak. Mevcut kayıt ve katılım geçmişi korunacak."
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !saving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text("Vazgeç")
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = !saving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                    ) {
                        Text(if (saving) "Kaldırılıyor…" else "Kaldır")
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentCouponCard(entry: CommunityEntry, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(260.dp),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.18f)),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(13.dp), color = PistachioGreen) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            val advantage = when (entry.data.discountType) {
                "percentage" -> if (entry.data.discountValue > 0) "%${entry.data.discountValue} indirim" else "Topluluğa özel indirim"
                "fixed" -> if (entry.data.discountValue > 0) "${entry.data.discountValue} TL indirim" else "Topluluğa özel indirim"
                "freeItem" -> "Ücretsiz ürün"
                else -> entry.data.title
            }
            Text(advantage, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(entry.data.title, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2)
            Text(entry.data.location, fontSize = 13.sp, color = TextSecondary, maxLines = 1)
            Text("Son gün: ${entry.data.date}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Text("Kuponu görüntüle", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityEntryDetailDialog(
    entry: CommunityEntry,
    canManage: Boolean,
    saving: Boolean,
    generatedCode: String?,
    codeGenerating: Boolean,
    registrations: List<CommunityEventRegistrationDto>,
    registered: Boolean,
    registrationLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onUnpublish: () -> Unit,
    onCreateCode: () -> Unit,
    onToggleRegistration: () -> Unit,
    onShowTicket: () -> Unit,
    onManageAttendees: () -> Unit,
    onReport: (() -> Unit)? = null
) {
    val isCoupon = entry.data.kind == "coupon"
    val dateTime = if (isCoupon) entry.data.date else formatEventSchedule(entry.data)

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDefault,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = PistachioGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isCoupon) Icons.Outlined.LocalOffer else Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                    Text(
                        text = entry.data.title,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 7.dp),
                        fontSize = 22.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Kapat",
                            tint = TextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                if (!isCoupon && AppEnvironment.firebaseBackend == FirebaseBackend.V2) {
                    CommunityDetailInfoRow(icon = Icons.Outlined.LocalOffer, text = EventCategory.labelFor(entry.data.categoryId))
                }
                CommunityDetailInfoRow(
                    icon = Icons.Outlined.CalendarMonth,
                    text = dateTime
                )
                CommunityDetailInfoRow(
                    icon = if (isCoupon) Icons.Outlined.Storefront else Icons.Outlined.LocationOn,
                    text = entry.data.location
                )

                Text(
                    text = entry.data.description,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = TextSecondary
                )

                if (isCoupon && !canManage) {
                    if (generatedCode == null) {
                        Button(
                            onClick = onCreateCode,
                            enabled = !codeGenerating && entry.data.businessId.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (codeGenerating) "Kod oluşturuluyor…" else "6 haneli kullanım kodu oluştur")
                        }
                        if (entry.data.businessId.isBlank()) {
                            Text("Bu kupon henüz işletme doğrulamasına bağlanmamış.", color = TextSecondary, fontSize = 12.sp)
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Kasada göstereceğin kod", color = TextSecondary, fontSize = 12.sp)
                                Text(generatedCode, color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
                                Text("Kod tek kullanımlıktır.", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (!isCoupon && !canManage) {
                    if (registered) Button(onClick = onShowTicket, modifier = Modifier.fillMaxWidth()) { Text("QR biletimi göster") }
                    Button(
                        onClick = onToggleRegistration,
                        enabled = !registrationLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = if (registered) {
                            ButtonDefaults.buttonColors(containerColor = SurfaceMuted, contentColor = TextPrimary)
                        } else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (registrationLoading) "Kaydediliyor…" else if (registered) "Kaydımı iptal et" else "Etkinliğe kayıt ol")
                    }
                }

                if (!isCoupon && canManage) {
                    Button(onClick = onManageAttendees, modifier = Modifier.fillMaxWidth()) { Text("Katılımcıları Yönet · QR giriş") }
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = PistachioGreen.copy(alpha = 0.55f)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("${registrations.size} kişi kayıtlı", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            if (registrations.isEmpty()) {
                                Text("Henüz katılımcı yok.", fontSize = 13.sp, color = TextSecondary)
                            } else {
                                registrations.take(8).forEach { registration ->
                                    Text("• ${registration.displayName}", fontSize = 13.sp, color = TextPrimary)
                                }
                                if (registrations.size > 8) Text("+${registrations.size - 8} kişi daha", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                if (canManage) {
                    HorizontalDivider(color = BorderMuted.copy(alpha = 0.32f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onEdit,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Düzenle", fontWeight = FontWeight.Medium)
                        }
                        OutlinedButton(
                            onClick = onUnpublish,
                            enabled = !saving && entry.data.status != "cancelled",
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(13.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                                disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Yayından kaldır",
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                if (!canManage && onReport != null) {
                    TextButton(
                        onClick = onReport,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Outlined.Flag, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bu içeriği bildir", fontSize = 13.sp, color = TextSecondary)
                    }
                }
                error?.let { Text(it, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private val reportReasons = listOf(
    "Uygunsuz veya rahatsız edici içerik",
    "Yanıltıcı ya da sahte bilgi",
    "Spam veya reklam",
    "Taciz, nefret söylemi veya şiddet",
    "Diğer"
)

@Composable
private fun ReportContentDialog(
    title: String,
    sending: Boolean,
    sent: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    var reason by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (sent) "Bildiriminiz alındı" else "İçeriği bildir") },
        text = {
            if (sent) {
                Text("Teşekkürler. Good4 ekibi \"$title\" içeriğini inceleyecek ve gerekirse yayından kaldıracak.")
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("\"$title\" için bir neden seçin.", fontSize = 14.sp, color = TextSecondary)
                    reportReasons.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !sending) { reason = option }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = reason == option, onClick = { reason = option }, enabled = !sending)
                            Text(option, fontSize = 14.sp, color = TextPrimary)
                        }
                    }
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it.take(500) },
                        enabled = !sending,
                        label = { Text("Açıklama (isteğe bağlı)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    error?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            if (sent) {
                TextButton(onClick = onDismiss) { Text("Tamam") }
            } else {
                TextButton(
                    onClick = { reason?.let { onSubmit(it, details) } },
                    enabled = reason != null && !sending
                ) { Text(if (sending) "Gönderiliyor…" else "Gönder") }
            }
        },
        dismissButton = {
            if (!sent) TextButton(onClick = onDismiss, enabled = !sending) { Text("Vazgeç") }
        }
    )
}

@Composable
private fun CommunityDetailInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun CommunityLogo(url: String, size: androidx.compose.ui.unit.Dp = 60.dp) {
    Surface(shape = RoundedCornerShape(16.dp), color = PistachioGreen, modifier = Modifier.size(size)) {
        if (url.isNotBlank()) AsyncImage(url, null, contentScale = ContentScale.Crop)
        else Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size * 0.5f)) }
    }
}

@Composable
private fun EmptyCommunityContent(title: String, subtitle: String, coupon: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp, horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (coupon) Icons.Outlined.LocalOffer else Icons.Outlined.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(40.dp)
        )
        Text(title, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(subtitle, fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

internal fun validateCommunityEntry(
    entry: CommunityEntryDto,
    initial: CommunityEntryDto? = null,
    now: kotlinx.datetime.LocalDateTime = eventNow()
): String? {
    if (entry.kind == "event" && AppEnvironment.firebaseBackend == FirebaseBackend.V2 && EventCategory.fromId(entry.categoryId) == null) return "Etkinlik kategorisini seçin."
    if (entry.title.isBlank() || entry.description.isBlank() || entry.location.isBlank()) return "Başlık, açıklama ve ${if (entry.kind == "coupon") "işletme" else "konum"} alanlarını doldurun."
    if (entry.kind == "event") validateEventSchedule(entry, initial, now)?.let { return it }
    else if (runCatching { LocalDate.parse(entry.date) }.isFailure) return "Son kullanım tarihini seçin."
    if (entry.kind == "event" && entry.capacity > 100_000) return "Kontenjan 100.000 kişiden fazla olamaz."
    if (entry.kind == "coupon" && entry.businessId.isBlank()) return "Kuponu doğrulayacak işletmeyi seçin."
    if (entry.kind == "coupon" && entry.discountType !in setOf("percentage", "fixed", "freeItem")) return "Geçerli bir indirim türü seçin."
    if (entry.kind == "coupon" && entry.discountType == "percentage" && entry.discountValue !in 1..100) return "İndirim oranını 1 ile 100 arasında girin."
    if (entry.kind == "coupon" && entry.discountType == "fixed" && entry.discountValue <= 0) return "İndirim tutarını girin."
    if (entry.title.length > 120 || entry.description.length > 4000 || entry.location.length > 200 || entry.code.length > 80) return "Bazı alanlar çok uzun. Lütfen kısaltın."
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryEditor(initial: CommunityEntryDto, editing: Boolean, businesses: List<CommunityBusiness>, saving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (CommunityEntryDto, ByteArray?) -> Unit) {
    var draft by remember { mutableStateOf(initial.withSuggestedEnd()) }
    var image by remember { mutableStateOf<ByteArray?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf(false) }
    var dateTarget by remember { mutableStateOf<ScheduleField?>(null) }
    var timeTarget by remember { mutableStateOf<ScheduleField?>(null) }
    val coupon = draft.kind == "coupon"
    val originalForValidation = if (editing) initial else null
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = SurfaceCanvasWarm) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(when {
                preview -> "Önizleme"
                coupon -> if (editing) "Kuponu düzenle" else "Kupon ekle"
                else -> if (editing) "Etkinliği düzenle" else "Etkinlik ekle"
            }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (!preview) {
                ProductImagePicker(currentRemoteImageUrl = draft.imageUrl, pendingImageBytes = image, isUploading = saving, onPendingImageChange = { image = it }, onError = { localError = it })
                EditorField("Başlık", draft.title, saving) { draft = draft.copy(title = it) }
                if (!coupon && AppEnvironment.firebaseBackend == FirebaseBackend.V2) {
                    EventCategorySelector(draft.categoryId, false, !saving) { draft = draft.copy(categoryId = it) }
                }
                if (coupon) {
                    OutlinedButton(onClick = { dateTarget = ScheduleField.START }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                        Text("Son kullanım tarihi: " + draft.date.ifBlank { "Seç" }.let(::formatEventDate))
                    }
                } else {
                    ScheduleRow("Başlangıç", draft.date, draft.time, !saving,
                        onDate = { dateTarget = ScheduleField.START }, onTime = { timeTarget = ScheduleField.START })
                    ScheduleRow("Bitiş", draft.endDate, draft.endTime, !saving,
                        onDate = { dateTarget = ScheduleField.END }, onTime = { timeTarget = ScheduleField.END })
                }
                if (coupon) {
                    BusinessSelector(
                        businesses = businesses,
                        selectedId = draft.businessId,
                        enabled = !saving,
                        onSelect = { business -> draft = draft.copy(businessId = business.id, location = business.name) }
                    )
                    DiscountTypeSelector(
                        selected = draft.discountType,
                        enabled = !saving,
                        onSelect = { draft = draft.copy(discountType = it) }
                    )
                    if (draft.discountType != "freeItem") {
                        EditorField(
                            if (draft.discountType == "percentage") "İndirim oranı (%)" else "İndirim tutarı (TL)",
                            draft.discountValue.takeIf { it > 0 }?.toString().orEmpty(),
                            saving
                        ) { value -> draft = draft.copy(discountValue = value.filter(Char::isDigit).toIntOrNull() ?: 0) }
                    }
                    Text("Her öğrenci bu kuponu 1 kez kullanabilir.", fontSize = 12.sp, color = TextSecondary)
                } else {
                    EditorField("Konum", draft.location, saving) { draft = draft.copy(location = it) }
                    EditorField(
                        "Kontenjan (sınırsız için boş bırak)",
                        draft.capacity.takeIf { it > 0 }?.toString().orEmpty(),
                        saving
                    ) { value ->
                        draft = draft.copy(capacity = value.filter(Char::isDigit).toIntOrNull() ?: 0)
                    }
                }
                EditorField(if (coupon) "Avantaj ve kullanım şartları" else "Açıklama", draft.description, saving, singleLine = false) { draft = draft.copy(description = it) }
            } else {
                Text(draft.title, style = MaterialTheme.typography.titleLarge)
                if (!coupon && AppEnvironment.firebaseBackend == FirebaseBackend.V2) Text(EventCategory.labelFor(draft.categoryId), color = MaterialTheme.colorScheme.primary)
                Text(if (coupon) formatEventDate(draft.date) else formatEventSchedule(draft)); Text(draft.location); Text(draft.description)
                if (!coupon) Text(if (draft.capacity > 0) "Kontenjan: ${draft.capacity} kişi" else "Kontenjan: Sınırsız")
                if (coupon) {
                    Text(
                        when (draft.discountType) {
                            "percentage" -> "%${draft.discountValue} indirim"
                            "fixed" -> "${draft.discountValue} TL indirim"
                            else -> "Ücretsiz ürün"
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = { preview = false }, enabled = !saving) { Text("Düzenlemeye dön") }
            }
            if (coupon) Text("Kuponunuz Good4 onayından sonra görünür olacak.", color = TextSecondary)
            (localError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!coupon && !preview) {
                OutlinedButton(
                    onClick = {
                        localError = when {
                            draft.title.isBlank() -> "Taslak için en az etkinlik başlığını girin."
                            AppEnvironment.firebaseBackend == FirebaseBackend.V2 && EventCategory.fromId(draft.categoryId) == null -> "Etkinlik kategorisini seçin."
                            else -> null
                        }
                        if (localError == null) {
                            onSave(
                                draft.copy(
                                    title = draft.title.trim(),
                                    description = draft.description.trim(),
                                    location = draft.location.trim(),
                                    status = "draft"
                                ),
                                image
                            )
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Taslak olarak kaydet")
                }
            }
            Button(onClick = {
                localError = validateCommunityEntry(draft, originalForValidation)
                if (localError == null) {
                    if (preview) {
                        onSave(
                            draft.copy(
                                title = draft.title.trim(),
                                description = draft.description.trim(),
                                location = draft.location.trim(),
                                totalLimit = 0,
                                status = if (coupon) "pending" else "published"
                            ),
                            image
                        )
                    } else {
                        preview = true
                    }
                }
            }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Kaydediliyor…" else if (!preview) "Önizle" else if (coupon) "Onaya gönder" else "Yayınla") }
        }
    }
    SchedulePickerDialogs(draft, dateTarget, timeTarget, onChange = { draft = it }, onCloseDate = { dateTarget = null }, onCloseTime = { timeTarget = null })
}

@Composable
private fun ScheduleRow(label: String, date: String, time: String, enabled: Boolean, onDate: () -> Unit, onTime: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDate, enabled = enabled, modifier = Modifier.weight(1.6f)) {
                Text(if (date.isBlank()) "Tarih seç" else formatEventDate(date), maxLines = 1)
            }
            OutlinedButton(onClick = onTime, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text(time.ifBlank { "Saat seç" }, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun EventCategorySelector(selectedId: String, filter: Boolean, enabled: Boolean, onSelect: (String) -> Unit) {
    if (filter) {
        CommunityEventFilterRow(selectedId, enabled, onSelect)
        return
    }
    var expanded by remember { mutableStateOf(false) }
    val label = when {
        selectedId.isEmpty() -> "Seç"
        else -> EventCategory.labelFor(selectedId)
    }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true }, enabled = enabled,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Outlined.LocalOffer, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Kategori: $label")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EventCategory.entries.forEach { category ->
                DropdownMenuItem(text = { Text(category.label) }, onClick = { onSelect(category.id); expanded = false })
            }
        }
    }
}

@Composable
private fun CommunityEventFilterRow(
    selectedId: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    followedOnly: Boolean = false,
    onFollowedOnlyChange: ((Boolean) -> Unit)? = null,
    studentStyle: Boolean = false,
) {
    val categories = remember {
        listOf("" to "Tümü") + EventCategory.entries.map { it.id to it.label } +
            (EventCategory.UNCATEGORIZED to "Kategori belirtilmemiş")
    }
    val listState = rememberLazyListState()
    var previousCategory by remember { mutableStateOf<String?>(null) }
    var previousFollowedOnly by remember { mutableStateOf(followedOnly) }
    val hasFollowFilter = onFollowedOnlyChange != null

    LaunchedEffect(selectedId, followedOnly, hasFollowFilter) {
        val categoryChanged = previousCategory != selectedId
        val followChanged = previousFollowedOnly != followedOnly
        val initialDefault = previousCategory == null && selectedId.isEmpty()
        previousCategory = selectedId
        previousFollowedOnly = followedOnly
        val index = if (hasFollowFilter && ((followChanged && !categoryChanged) || initialDefault)) {
            0
        } else {
            categories.indexOfFirst { it.first == selectedId }.coerceAtLeast(0) + if (hasFollowFilter) 1 else 0
        }
        listState.animateScrollToItem(index)
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        if (onFollowedOnlyChange != null) item(key = "followed") {
            if (studentStyle) {
                SmallToggle("Takip ettiklerim", followedOnly, onClick = { onFollowedOnlyChange(!followedOnly) })
            } else {
                CommunityEventFilterChip(
                    selected = followedOnly,
                    enabled = true,
                    label = "Takip ettiklerim",
                    onClick = { onFollowedOnlyChange(!followedOnly) },
                    leadingIcon = { Icon(Icons.Outlined.Groups, null, Modifier.size(18.dp)) },
                )
            }
        }
        items(categories, key = { "category-${it.first}" }) { (id, label) ->
            if (studentStyle) {
                SmallToggle(label, selectedId == id, enabled, onClick = { onSelect(id) })
            } else {
                CommunityEventFilterChip(selectedId == id, enabled, label, onClick = { onSelect(id) })
            }
        }
    }
}

@Composable
private fun CommunityEventFilterChip(
    selected: Boolean,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, maxLines = 1) },
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SurfaceDefault,
            labelColor = TextSecondary,
            iconColor = TextSecondary,
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
            selectedLeadingIconColor = colors.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = BorderMuted,
            selectedBorderColor = colors.primary,
            selectedBorderWidth = 1.dp,
        ),
    )
}

@Composable
private fun DiscountTypeSelector(selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("İndirim türü", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "percentage" to "Yüzde",
                "fixed" to "Sabit TL",
                "freeItem" to "Ücretsiz"
            ).forEach { (value, label) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    enabled = enabled,
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
private fun BusinessSelector(
    businesses: List<CommunityBusiness>,
    selectedId: String,
    enabled: Boolean,
    onSelect: (CommunityBusiness) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = businesses.firstOrNull { it.id == selectedId }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && businesses.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Outlined.Storefront, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(selected?.name ?: if (businesses.isEmpty()) "Kayıtlı işletme bulunamadı" else "Doğrulayacak işletmeyi seç")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            businesses.forEach { business ->
                DropdownMenuItem(
                    text = { Text(business.name) },
                    onClick = {
                        onSelect(business)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityProfileEditor(initial: CommunityDto, saving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (CommunityDto, ByteArray?, ByteArray?) -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    var logo by remember { mutableStateOf<ByteArray?>(null) }
    var cover by remember { mutableStateOf<ByteArray?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Topluluk bilgileri", style = MaterialTheme.typography.headlineSmall)
            Text("Topluluk logosu", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            ProductImagePicker(currentRemoteImageUrl = draft.logoUrl, pendingImageBytes = logo, isUploading = saving, onPendingImageChange = { logo = it }, onError = { localError = it })
            Text("Kapak fotoğrafı", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            ProductImagePicker(currentRemoteImageUrl = draft.coverUrl, pendingImageBytes = cover, isUploading = saving, onPendingImageChange = { cover = it }, onError = { localError = it })
            EditorField("Topluluk adı", draft.name, saving) { draft = draft.copy(name = it) }
            EditorField("Kısa açıklama", draft.description, saving, false) { draft = draft.copy(description = it) }
            (localError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { onSave(draft.copy(name = draft.name.trim(), description = draft.description.trim()), logo, cover) }, enabled = !saving && draft.name.isNotBlank() && draft.name.length <= 120 && draft.description.length <= 1000, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Kaydediliyor…" else "Kaydet") }
        }
    }
}

@Composable
private fun EditorField(label: String, value: String, saving: Boolean, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, enabled = !saving, singleLine = singleLine, shape = RoundedCornerShape(14.dp))
}
