package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.good4.config.domain.HomeBanner
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorSnackbar
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceCanvasWarm
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.toDisplayAddressOrNull
import com.good4.core.util.ReservationTimeCalculator
import com.good4.core.util.openMaps
import com.good4.dining.domain.DailyMeal
import com.good4.dining.presentation.AkdenizDiningMenuState
import com.good4.dining.presentation.AkdenizDiningMenuViewModel
import com.good4.feedback.FeedbackViewModel
import com.good4.notification.NotificationInbox
import com.good4.student.home.HomeShortcut
import com.good4.student.presentation.home.appearance
import com.good4.product.Product
import com.good4.product.presentation.product_list.ProductListAction
import com.good4.product.presentation.product_list.ProductListState
import com.good4.product.presentation.product_list.ProductListViewModel
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.home_delivery_time
import good4.composeapp.generated.resources.home_welcome_generic
import good4.composeapp.generated.resources.home_welcome_title
import good4.composeapp.generated.resources.good4_logo_transparent
import good4.composeapp.generated.resources.product_list_active_reservation_title
import good4.composeapp.generated.resources.product_list_countdown_prefix
import good4.composeapp.generated.resources.product_list_credit_label
import good4.composeapp.generated.resources.product_list_greeting_prefix
import good4.composeapp.generated.resources.product_list_greeting_suffix
import good4.composeapp.generated.resources.product_list_order_code_label
import good4.composeapp.generated.resources.reservation_status_pending
import good4.composeapp.generated.resources.student_reservations
import good4.composeapp.generated.resources.time_minute_suffix
import good4.composeapp.generated.resources.time_second_suffix
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.seconds

@Composable
fun ProductListScreenRoot(
    communityManager: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = koinViewModel(),
    onProfileClick: (() -> Unit)? = null,
    onReservationCardClick: () -> Unit = {},
    onCommunitiesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onCampusMapClick: () -> Unit = {},
    onClassScheduleClick: () -> Unit = {},
    onDailyMenuClick: (DailyMeal) -> Unit = {},
    homeShortcuts: List<HomeShortcut> = HomeShortcut.entries.filter { it.defaultVisible && (it != HomeShortcut.SUSPENDED_MEALS || config.ReleaseFeatures.suspendedMeals) },
    onMenuShortcutClick: (HomeShortcut) -> Unit = {},
    onEditHomeClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val diningMenuViewModel: AkdenizDiningMenuViewModel = koinViewModel()
    val diningMenuState by diningMenuViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadActiveReservation()
        viewModel.loadStudentInfo()
        viewModel.loadHomeBanner()
        diningMenuViewModel.loadMenu()
    }
    // Returning to the app on a new day must not keep yesterday's menu on the widget.
    LifecycleResumeEffect(Unit) {
        diningMenuViewModel.refreshIfDayChanged()
        onPauseOrDispose { }
    }

    ProductListScreen(
        communityManager = communityManager,
        modifier = modifier,
        state = state,
        diningMenuState = diningMenuState,
        onCommunitiesClick = onCommunitiesClick,
        onProfileClick = onProfileClick,
        onNotificationsClick = onNotificationsClick,
        onReservationCardClick = onReservationCardClick,
        onCalendarClick = onCalendarClick,
        onCampusMapClick = onCampusMapClick,
        onClassScheduleClick = onClassScheduleClick,
        onDailyMenuClick = onDailyMenuClick,
        homeShortcuts = homeShortcuts,
        onMenuShortcutClick = onMenuShortcutClick,
        onEditHomeClick = onEditHomeClick,
        onAction = { action ->
            viewModel.onAction(action)
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductListScreen(
    communityManager: Boolean = false,
    modifier: Modifier = Modifier,
    state: ProductListState,
    diningMenuState: AkdenizDiningMenuState = AkdenizDiningMenuState(),
    onProfileClick: (() -> Unit)? = null,
    onNotificationsClick: () -> Unit = {},
    onReservationCardClick: () -> Unit = {},
    onCommunitiesClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onCampusMapClick: () -> Unit = {},
    onClassScheduleClick: () -> Unit = {},
    onDailyMenuClick: (DailyMeal) -> Unit = {},
    homeShortcuts: List<HomeShortcut> = HomeShortcut.entries.filter { it.defaultVisible && (it != HomeShortcut.SUSPENDED_MEALS || config.ReleaseFeatures.suspendedMeals) },
    onMenuShortcutClick: (HomeShortcut) -> Unit = {},
    onEditHomeClick: () -> Unit = {},
    onAction: (ProductListAction) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.activeReservation) {
        if (state.activeReservation != null) {
            listState.animateScrollToItem(0)
        }
    }

    Good4NestedScaffold(
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceCanvasWarm)
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryGreen
                    )
                }

                else -> Column(Modifier.fillMaxSize()) {
                    // The greeting stays outside the list so iOS bounce never drags it under the status bar.
                    ProductListGreetingHeader(
                        userName = state.userName.orEmpty(),
                        onProfileClick = onProfileClick,
                        onNotificationsClick = onNotificationsClick
                    )
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(
                            top = if (onProfileClick == null) {
                                WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
                            } else {
                                0.dp
                            },
                            bottom = 16.dp
                        )
                    ) {
                        item {
                            CampusSummaryCards(
                                diningMenuState = diningMenuState,
                                onDailyMenuClick = onDailyMenuClick
                            )
                        }

                        if (state.homeBanners.isNotEmpty()) {
                            item { HomeAdvertisementSlider(state.homeBanners) }
                        }

                        item {
                            HomeQuickActions(
                                communityManager = communityManager,
                                onReservationsClick = onReservationCardClick,
                                onCommunitiesClick = onCommunitiesClick,
                                onCalendarClick = onCalendarClick,
                                onCampusMapClick = onCampusMapClick,
                                onClassScheduleClick = onClassScheduleClick,
                                shortcuts = homeShortcuts,
                                onMenuShortcutClick = onMenuShortcutClick,
                                onEditHomeClick = onEditHomeClick
                            )
                        }

                        state.activeReservation?.let { reservation ->
                            item {
                                ProductListActiveReservationCard(
                                    reservationCode = reservation.code,
                                    product = reservation.product,
                                    expiryTime = reservation.expiryTime,
                                    codeId = reservation.codeId,
                                    onClick = onReservationCardClick,
                                    onExpired = { codeId ->
                                        onAction(ProductListAction.OnReservationExpired(codeId))
                                    }
                                )
                            }
                        }

                    }
                }
            }

            ErrorSnackbar(
                modifier = Modifier.align(Alignment.TopCenter),
                errorMessage = state.errorMessage,
                onDismiss = { onAction(ProductListAction.OnDismissError) }
            )

        }
    }
}

@Composable
private fun ProductListGreetingHeader(
    modifier: Modifier = Modifier,
    userName: String,
    onProfileClick: (() -> Unit)? = null,
    onNotificationsClick: () -> Unit = {}
) {
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val hasUnseenNotifications by NotificationInbox.hasUnseen.collectAsStateWithLifecycle()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(SurfaceCanvasWarm)
            .padding(start = 18.dp, end = 14.dp, top = topInset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.good4_logo_transparent),
                contentDescription = "Good4",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 42.dp, height = 44.dp)
            )
            Text(
                modifier = Modifier.weight(1f),
                text = if (userName.isBlank()) {
                    stringResource(Res.string.home_welcome_generic)
                } else {
                    stringResource(Res.string.home_welcome_title, userName)
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SurfaceDefault,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.24f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Bildirimler",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (hasUnseenNotifications) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-7).dp, y = 7.dp)
                                .size(7.dp)
                                .background(Color(0xFFFFD54F), CircleShape)
                        )
                    }
                }
                if (onProfileClick != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceCanvasWarm),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onProfileClick) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = "Profil",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSummaryCards(
    modifier: Modifier = Modifier,
    remainingCredits: Int,
    deliveryTimeMinutes: Int
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(116.dp),
            shape = RoundedCornerShape(20.dp),
            color = PrimaryGreen,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.product_list_credit_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.78f)
                )
                Text(
                    text = remainingCredits.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(116.dp),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceDefault,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.home_delivery_time),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "$deliveryTimeMinutes dk",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

/**
 * Up to four banners in a horizontal strip. Each card takes about three quarters of the width so
 * the next one peeks in, which shows the strip can be swiped without extra indicators.
 */
@Composable
private fun HomeAdvertisementSlider(banners: List<HomeBanner>) {
    if (banners.size == 1) {
        // A lone banner keeps the strip's card size so the page looks the same with one ad or four.
        BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            HomeAdvertisementBanner(
                banners.single(),
                Modifier.padding(start = 12.dp).width(maxWidth * 0.75f - 12.dp)
            )
        }
        return
    }
    val pagerState = rememberPagerState { banners.size }
    val dragged by pagerState.interactionSource.collectIsDraggedAsState()
    // settledPage only changes once a slide finishes; keying on currentPage cancelled the slide halfway.
    LaunchedEffect(pagerState.settledPage, dragged) {
        if (dragged) return@LaunchedEffect
        delay(5_000)
        pagerState.animateScrollToPage((pagerState.settledPage + 1) % banners.size)
    }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(start = 12.dp, end = maxWidth * 0.25f),
            pageSpacing = 10.dp
        ) { page ->
            HomeAdvertisementBanner(banners[page])
        }
    }
}

@Composable
private fun HomeAdvertisementBanner(
    banner: HomeBanner,
    modifier: Modifier = Modifier,
    feedbackViewModel: FeedbackViewModel = koinViewModel()
) {
    val uriHandler = LocalUriHandler.current
    // An uploaded image is always shown, Good4's own promotions included; the "Reklam alanı" card only fills in without one.
    val isPlaceholder = banner.imageUrl.isBlank()
    val feedbackState by feedbackViewModel.state.collectAsStateWithLifecycle()
    var showAdInfo by remember { mutableStateOf(false) }
    var showAdReport by remember { mutableStateOf(false) }
    com.good4.review.ReviewModalBlocker(showAdInfo || showAdReport)
    var reportReason by remember { mutableStateOf("") }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2.5f)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceMuted)
            .then(if (!isPlaceholder && banner.targetUrl.isNotBlank()) Modifier.clickable {
                uriHandler.openUri(banner.targetUrl)
            } else Modifier)
    ) {
        if (isPlaceholder) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceMuted,
                border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Store,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(30.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Reklam alanı",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Sponsorlu içerikler burada gösterilir.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            AsyncImage(
                model = banner.imageUrl,
                contentDescription = banner.advertiserName.ifBlank { "Sponsorlu içerik" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // One small "Reklam ⓘ" tag keeps the disclosure and the info/report entry without covering the ad.
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(onClickLabel = "Reklam hakkında ve reklamı bildir") { showAdInfo = true }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(text = "Reklam", color = Color.White, fontSize = 9.sp, lineHeight = 11.sp)
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }

    if (showAdInfo) {
        AlertDialog(
            onDismissRequest = { showAdInfo = false },
            title = { Text("Reklam hakkında") },
            text = {
                Text(
                    "Reklamveren: ${banner.advertiserName}\n\n" +
                        "Bu reklam Good4 yöneticisi tarafından incelenip ana sayfadaki genel reklam alanında yayımlanır."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    feedbackViewModel.startNew()
                    reportReason = ""
                    showAdInfo = false
                    showAdReport = true
                }) { Text("Reklamı bildir") }
            },
            dismissButton = {
                TextButton(onClick = { showAdInfo = false }) { Text("Kapat") }
            }
        )
    }

    if (showAdReport) {
        AlertDialog(
            onDismissRequest = {
                if (!feedbackState.isSubmitting) showAdReport = false
            },
            title = { Text(if (feedbackState.isSubmitted) "Bildirim gönderildi" else "Reklamı bildir") },
            text = {
                if (feedbackState.isSubmitted) {
                    Text("Bildirimin Good4 yönetim paneline ulaştı. Teşekkürler.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${banner.advertiserName} reklamında uygunsuz veya yaşa uygun olmayan bir içerik gördüysen bize bildir.")
                        OutlinedTextField(
                            value = reportReason,
                            onValueChange = { if (it.length <= 1000) reportReason = it },
                            label = { Text("Sorunu açıkla") },
                            supportingText = { Text("En az 10 karakter yazmalısın.") },
                            minLines = 3,
                            maxLines = 5,
                            enabled = !feedbackState.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )
                        feedbackState.errorMessage?.let { error ->
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                if (feedbackState.isSubmitted) {
                    TextButton(onClick = { showAdReport = false }) { Text("Tamam") }
                } else {
                    TextButton(
                        enabled = reportReason.trim().length >= 10 && !feedbackState.isSubmitting,
                        onClick = {
                            val reason = reportReason.trim()
                            if (reason.length < 10) return@TextButton
                            feedbackViewModel.onSubjectChange(
                                "Reklam bildirimi: ${banner.advertiserName.take(90)}"
                            )
                            feedbackViewModel.onMessageChange(
                                "Reklamveren: ${banner.advertiserName}\n" +
                                    "Yayın: ${banner.startsOn}–${banner.endsOn}\n" +
                                    "Bildirim: $reason"
                            )
                            feedbackViewModel.submit()
                        }
                    ) {
                        Text(if (feedbackState.isSubmitting) "Gönderiliyor…" else "Gönder")
                    }
                }
            },
            dismissButton = {
                if (!feedbackState.isSubmitted) {
                    TextButton(
                        enabled = !feedbackState.isSubmitting,
                        onClick = { showAdReport = false }
                    ) { Text("Vazgeç") }
                }
            }
        )
    }
}

@Composable
private fun HomeQuickActions(
    communityManager: Boolean,
    onReservationsClick: () -> Unit,
    onCommunitiesClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onCampusMapClick: () -> Unit,
    onClassScheduleClick: () -> Unit,
    shortcuts: List<HomeShortcut>,
    onMenuShortcutClick: (HomeShortcut) -> Unit,
    onEditHomeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shortcuts.forEach { shortcut ->
            val appearance = shortcut.appearance(communityManager)
            val onClick = when (shortcut) {
                HomeShortcut.COMMUNITIES -> onCommunitiesClick
                HomeShortcut.CLASS_SCHEDULE -> onClassScheduleClick
                HomeShortcut.CAMPUS_MAP -> onCampusMapClick
                HomeShortcut.ACADEMIC_CALENDAR -> onCalendarClick
                HomeShortcut.SUSPENDED_MEALS -> onReservationsClick
                else -> ({ onMenuShortcutClick(shortcut) })
            }
            HomeQuickActionCard(
                modifier = Modifier.fillMaxWidth(),
                title = appearance.title,
                icon = appearance.icon,
                accent = appearance.accent,
                onClick = onClick
            )
        }
        TextButton(onClick = onEditHomeClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Outlined.Edit, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Sayfanı Düzenle", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun HomeQuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: (() -> Unit)?
) {
    val iconAccent = if (accent == PrimaryGreen) MaterialTheme.colorScheme.primary else accent
    Surface(
        modifier = modifier
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(18.dp))) {
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp, end = 58.dp, top = 12.dp, bottom = 12.dp),
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(36.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(36.dp)
                        .graphicsLayer(rotationZ = -5f)
                        .background(
                            color = TextPrimary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(11.dp)
                        )
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .graphicsLayer(rotationZ = -5f),
                    shape = RoundedCornerShape(11.dp),
                    color = iconAccent.copy(alpha = 0.24f),
                    border = BorderStroke(1.dp, iconAccent.copy(alpha = 0.88f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductListActiveReservationCard(
    modifier: Modifier = Modifier,
    reservationCode: String,
    product: Product,
    expiryTime: Instant?,
    codeId: String,
    onClick: () -> Unit = {},
    onExpired: (String) -> Unit
) {
    var remainingTime by remember { mutableStateOf("") }
    var isExpired by remember { mutableStateOf(false) }
    val minuteSuffix = stringResource(Res.string.time_minute_suffix)
    val secondSuffix = stringResource(Res.string.time_second_suffix)

    LaunchedEffect(expiryTime) {
        while (expiryTime != null && !isExpired) {
            val remainingSeconds = ReservationTimeCalculator.remainingSecondsUntilExpiry(
                expiresAtEpochSeconds = expiryTime.epochSeconds
            ) ?: break
            if (remainingSeconds <= 0) {
                isExpired = true
                onExpired(codeId)
                break
            }

            remainingTime = ReservationTimeCalculator.formatRemainingTimeFromExpiry(
                expiresAtEpochSeconds = expiryTime.epochSeconds,
                minuteSuffix = minuteSuffix,
                secondSuffix = secondSuffix,
                expiredLabel = ""
            ).orEmpty()

            delay(1.seconds)
        }
    }

    val displayAddress = toDisplayAddressOrNull(product.address)
    val mapsAddress = product.addressUrl.ifBlank { null }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = PistachioGreen,
        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f)),
        shadowElevation = 3.dp
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDefault,
            border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.product_list_active_reservation_title),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PistachioGreen
                    ) {
                        Text(
                            text = stringResource(Res.string.reservation_status_pending),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = BorderMuted.copy(alpha = 0.4f))

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Store,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = product.storeName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (displayAddress != null) {
                            Text(
                                text = displayAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (mapsAddress != null) {
                                    Modifier.clickable { openMaps(mapsAddress) }
                                } else {
                                    Modifier
                                },
                                textDecoration = if (mapsAddress != null) TextDecoration.Underline else null
                            )
                        }
                    }
                }

                if (reservationCode.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(Res.string.product_list_order_code_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary.copy(alpha = 0.65f)
                            )
                            Text(
                                text = reservationCode,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = PrimaryGreen,
                                letterSpacing = 0.5.sp
                            )
                        }
                        if (remainingTime.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(Res.string.product_list_countdown_prefix) +
                                            remainingTime,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ProductListScreenPreview() {
    MaterialTheme {
        ProductListScreen(
            state = ProductListState(),
            onAction = {}
        )
    }
}
