package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorSnackbar
import com.good4.core.presentation.LocalThemeController
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceCanvasWarm
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.ProfileTopBarAction
import com.good4.core.presentation.components.toDisplayAddressOrNull
import com.good4.core.util.ReservationTimeCalculator
import com.good4.core.util.openMaps
import com.good4.dining.presentation.AkdenizDiningMenuCard
import com.good4.dining.presentation.AkdenizDiningMenuState
import com.good4.dining.presentation.AkdenizDiningMenuViewModel
import com.good4.dining.presentation.AKDENIZ_BALANCE_URL
import com.good4.product.Product
import com.good4.product.presentation.product_list.ProductListAction
import com.good4.product.presentation.product_list.ProductListState
import com.good4.product.presentation.product_list.ProductListViewModel
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.good4_home_header_background
import good4.composeapp.generated.resources.home_delivery_time
import good4.composeapp.generated.resources.home_top_up
import good4.composeapp.generated.resources.home_welcome_generic
import good4.composeapp.generated.resources.home_welcome_title
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
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = koinViewModel(),
    onProfileClick: (() -> Unit)? = null,
    onReservationCardClick: () -> Unit = {},
    onCommunitiesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val diningMenuViewModel: AkdenizDiningMenuViewModel = koinViewModel()
    val diningMenuState by diningMenuViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadActiveReservation()
        viewModel.loadStudentInfo()
        diningMenuViewModel.loadMenu()
    }

    ProductListScreen(
        modifier = modifier,
        state = state,
        diningMenuState = diningMenuState,
        onCommunitiesClick = onCommunitiesClick,
        onProfileClick = onProfileClick,
        onNotificationsClick = onNotificationsClick,
        onReservationCardClick = onReservationCardClick,
        onAction = { action ->
            viewModel.onAction(action)
        }
    )
}

@Composable
fun ProductListScreen(
    modifier: Modifier = Modifier,
    state: ProductListState,
    diningMenuState: AkdenizDiningMenuState = AkdenizDiningMenuState(),
    onProfileClick: (() -> Unit)? = null,
    onNotificationsClick: () -> Unit = {},
    onReservationCardClick: () -> Unit = {},
    onCommunitiesClick: () -> Unit = {},
    onAction: (ProductListAction) -> Unit
) {
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

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

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
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
                            ProductListGreetingHeader(
                                userName = state.userName.orEmpty(),
                                onProfileClick = onProfileClick,
                                onNotificationsClick = onNotificationsClick
                            )
                        }

                        item {
                            CampusSummaryCards(
                                diningMenuState = diningMenuState,
                                onMenuClick = { uriHandler.openUri(AKDENIZ_BALANCE_URL) }
                            )
                        }

                        item {
                            HomeUpcomingEvents(
                                onViewAllClick = onCommunitiesClick
                            )
                        }

                        item {
                            HomeQuickActions(
                                onTopUpClick = {
                                    uriHandler.openUri(AKDENIZ_BALANCE_URL)
                                },
                                onReservationsClick = onReservationCardClick,
                                onCommunitiesClick = onCommunitiesClick
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
    val theme = LocalThemeController.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
    ) {
        Image(
            painter = painterResource(Res.drawable.good4_home_header_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = if (userName.isBlank()) {
                stringResource(Res.string.home_welcome_generic)
            } else {
                stringResource(Res.string.home_welcome_title, userName)
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(
                    start = 64.dp,
                    top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
                    end = if (onProfileClick == null) 64.dp else 112.dp
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
                    end = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { theme.setDark(!theme.isDark) }) {
                Icon(
                    imageVector = if (theme.isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = if (theme.isDark) "Gündüz moduna geç" else "Gece moduna geç",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            if (onProfileClick != null) {
                ProfileTopBarAction(
                    onClick = onProfileClick,
                    tint = Color.White
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
                    start = 12.dp
                )
        ) {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Bildirimler",
                    tint = Color.White,
                    modifier = Modifier.size(27.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 7.dp)
                    .size(8.dp)
                    .background(Color(0xFFFFD54F), RoundedCornerShape(50))
            )
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

@Composable
private fun HomeUpcomingEvents(
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Yaklaşan Etkinlikler",
                fontSize = 23.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Tümünü Gör",
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryGreen,
                modifier = Modifier.clickable(onClick = onViewAllClick)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Yaklaşan topluluk etkinliklerini burada görebilirsin.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary.copy(alpha = 0.72f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HomeQuickActions(
    onTopUpClick: () -> Unit,
    onReservationsClick: () -> Unit,
    onCommunitiesClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeQuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Topluluklar",
                icon = Icons.Outlined.Groups,
                accent = Color(0xFF75D9BE),
                onClick = onCommunitiesClick
            )
            HomeQuickActionCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.student_reservations),
                icon = Icons.Outlined.ShoppingCart,
                accent = Color(0xFF8CB7ED),
                onClick = onReservationsClick
            )
        }
        repeat(2) { row ->
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { column ->
                    val position = row * 2 + column
                    HomeQuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = if (position == 0) stringResource(Res.string.home_top_up) else "Yeni Alan ${position + 1}",
                        icon = when (position) {
                            0 -> Icons.Outlined.AccountBalanceWallet
                            1 -> Icons.Outlined.Place
                            2 -> Icons.Outlined.CalendarMonth
                            else -> Icons.Outlined.Email
                        },
                        accent = when (position) {
                            0 -> Color(0xFFF2A66F)
                            1 -> Color(0xFFB997EB)
                            2 -> Color(0xFFAAA4F2)
                            else -> Color(0xFF68CCDC)
                        },
                        onClick = if (position == 0) onTopUpClick else null
                    )
                }
            }
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
    Surface(
        modifier = modifier
            .height(76.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp
    ) {
        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))) {
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp, end = 58.dp),
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
                    .size(64.dp)
            )
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
