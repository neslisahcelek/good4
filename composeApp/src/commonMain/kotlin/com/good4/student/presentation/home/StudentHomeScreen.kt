package com.good4.student.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.good4.review.StoreReviewViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalUriHandler
import com.good4.student.home.HomeLayoutViewModel
import com.good4.student.home.HomeShortcut
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NavigationBar
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.campus.presentation.CampusMapScreen
import com.good4.community.CommunityViewModel
import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend
import com.good4.dining.domain.DailyMeal
import com.good4.dining.presentation.AkdenizDiningMenuViewModel
import com.good4.dining.presentation.DailyMenuScreen
import com.good4.product.presentation.product_list.ProductListViewModel
import com.good4.product.presentation.product_list.views.ProductListScreenRoot
import com.good4.student.presentation.reservations.ReservationUiModel
import com.good4.student.presentation.reservations.StudentReservationsScreen
import com.good4.student.presentation.reservations.StudentReservationsViewModel
import com.good4.suspendedmeal.SuspendedMealsScreen
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.student_home
import good4.composeapp.generated.resources.student_reservations
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun StudentHomeScreenRoot(
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToClassSchedule: () -> Unit = {},
    onNavigateToCampusCloset: () -> Unit = {},
    onNavigateToEditHome: (Boolean) -> Unit = {}
) {
    val navItems = listOf(
        BottomNavItem(
            title = stringResource(Res.string.student_home),
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            title = "Menü",
            selectedIcon = Icons.Filled.Menu,
            unselectedIcon = Icons.Filled.Menu
        )
    )

    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    var dailyMenuMeal by rememberSaveable { mutableStateOf(DailyMeal.CAFETERIA) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var menuInitialShortcutId by rememberSaveable { mutableStateOf<String?>(null) }
    var editHomeRequested by remember { mutableStateOf(false) }
    val layoutViewModel: HomeLayoutViewModel = koinViewModel()
    val layoutState by layoutViewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var reservationsScrollRequestKey by rememberSaveable { mutableIntStateOf(0) }
    var pendingReservationFromHome by remember { mutableStateOf<ReservationUiModel?>(null) }
    var managerEntryHandled by rememberSaveable { mutableStateOf(false) }
    val productListViewModel: ProductListViewModel = koinViewModel()
    val reservationsViewModel: StudentReservationsViewModel? = if (AppEnvironment.firebaseBackend != FirebaseBackend.V2) koinViewModel() else null
    val communityViewModel: CommunityViewModel = koinViewModel()
    val reviewViewModel: StoreReviewViewModel = koinViewModel()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    SideEffect { reviewViewModel.updateHome(lifecycleState == Lifecycle.State.RESUMED) }
    DisposableEffect(reviewViewModel) {
        onDispose { reviewViewModel.leaveScreen() }
    }
    com.good4.review.ReviewModalBlocker(menuOpen)
    val reservationsState by (reservationsViewModel?.state ?: remember {
        kotlinx.coroutines.flow.MutableStateFlow(com.good4.student.presentation.reservations.StudentReservationsState())
    }).collectAsStateWithLifecycle()
    val communityState by communityViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(editHomeRequested) {
        if (editHomeRequested) {
            // Let rememberSaveable capture the closed sheet before this route leaves composition.
            // Otherwise popping the editor can restore the menu over the home screen.
            withFrameNanos { }
            editHomeRequested = false
            onNavigateToEditHome(communityState.access.active && communityState.access.communityIds.isNotEmpty())
        }
    }

    fun editHome() {
        menuOpen = false
        editHomeRequested = true
    }

    LaunchedEffect(communityState.loading, communityState.access, communityState.communities) {
        if (!managerEntryHandled && !communityState.loading && communityState.access.active) {
            val managedCommunity = communityState.communities.firstOrNull {
                it.id in communityState.access.communityIds
            }
            managerEntryHandled = true
            if (managedCommunity != null) {
                communityViewModel.select(managedCommunity)
                selectedItemIndex = 2
            }
        }
    }

    fun showReservationsTab() {
        val productState = productListViewModel.state.value
        if (productState.activeReservation != null) {
            pendingReservationFromHome = productState.toPendingReservationUiModel()
            reservationsScrollRequestKey += 1
        }
        selectedItemIndex = 1
    }

    LaunchedEffect(reservationsState.reservations, pendingReservationFromHome?.id) {
        val pendingId = pendingReservationFromHome?.id ?: return@LaunchedEffect
        val existsInReservations = reservationsState.reservations.any { it.id == pendingId }
        if (existsInReservations) {
            pendingReservationFromHome = null
        }
    }

    LaunchedEffect(selectedItemIndex) {
        when (selectedItemIndex) {
            0 -> productListViewModel.refresh()
            1 -> reservationsViewModel?.refresh()
        }
    }

    Good4NestedScaffold(
        modifier = modifier,
        bottomBar = {
            Good4NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = if (index == 1) menuOpen else selectedItemIndex == 0 && !menuOpen,
                        onClick = {
                            if (index == 1) {
                                menuInitialShortcutId = null
                                menuOpen = true
                            } else {
                                selectedItemIndex = index
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedItemIndex == index) {
                                    item.selectedIcon
                                } else {
                                    item.unselectedIcon
                                },
                                contentDescription = item.title
                            )
                        },
                        label = {
                            Text(item.title)
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedItemIndex) {
                2 -> com.good4.community.CommunitiesScreen(
                    onBack = { selectedItemIndex = 0 },
                    managerEntryMode = communityState.access.active && managerEntryHandled,
                    onSwitchToStudent = { selectedItemIndex = 0 },
                    viewModel = communityViewModel
                )
                0 -> {
                    ProductListScreenRoot(
                        communityManager = communityState.access.active && communityState.access.communityIds.isNotEmpty(),
                        viewModel = productListViewModel,
                        homeShortcuts = layoutState.layout.visible.mapNotNull { id -> layoutState.shortcuts.firstOrNull { it.id == id } },
                        onEditHomeClick = ::editHome,
                        onMenuShortcutClick = { shortcut ->
                            val url = shortcut.externalUrl
                            if (url != null) uriHandler.openUri(url)
                            else {
                                menuInitialShortcutId = shortcut.id
                                menuOpen = true
                            }
                        },
                        onCommunitiesClick = {
                            val managed = communityState.communities.filter { communityState.access.active && it.id in communityState.access.communityIds }
                            if (managed.size == 1) communityViewModel.select(managed.first()) else communityViewModel.back()
                            selectedItemIndex = 2
                        },
                        onProfileClick = onNavigateToProfile,
                        onNotificationsClick = onNavigateToNotifications,
                        onCalendarClick = onNavigateToCalendar,
                        onClassScheduleClick = onNavigateToClassSchedule,
                        onCampusClosetClick = onNavigateToCampusCloset,
                        onCampusMapClick = { selectedItemIndex = 3 },
                        onDailyMenuClick = { meal ->
                            dailyMenuMeal = meal
                            selectedItemIndex = 4
                        },
                        onReservationCardClick = {
                            // V2 has its own campaign-based flow; the V1 reservation tab reads collections V2 denies.
                            if (AppEnvironment.firebaseBackend == FirebaseBackend.V2) selectedItemIndex = 5
                            else showReservationsTab()
                        }
                    )
                }

                1 -> if (reservationsViewModel != null) {
                    StudentReservationsScreen(
                        viewModel = reservationsViewModel,
                        scrollToTopRequestKey = reservationsScrollRequestKey,
                        prioritizedReservation = pendingReservationFromHome,
                        onProfileClick = onNavigateToProfile,
                        onReservationCancelStarted = { reservationId ->
                            if (pendingReservationFromHome?.id == reservationId) {
                                pendingReservationFromHome = null
                            }
                            productListViewModel.clearActiveReservationIfMatches(reservationId)
                        }
                    )
                }

                3 -> CampusMapScreen(onBackClick = { selectedItemIndex = 0 })
                5 -> SuspendedMealsScreen(onBackClick = { selectedItemIndex = 0 })
                4 -> {
                    // Same view model instance as the home widget, so the page opens without reloading.
                    val diningMenuViewModel: AkdenizDiningMenuViewModel = koinViewModel()
                    val diningMenuState by diningMenuViewModel.state.collectAsStateWithLifecycle()
                    DailyMenuScreen(
                        state = diningMenuState,
                        initialMeal = dailyMenuMeal,
                        onRefreshIfDayChanged = diningMenuViewModel::refreshIfDayChanged,
                        onBackClick = { selectedItemIndex = 0 }
                    )
                }
            }
        }
    }
    if (menuOpen) {
        StudentMenuSheet(
            onDismiss = { menuOpen = false },
            initialShortcut = HomeShortcut.entries.firstOrNull { it.id == menuInitialShortcutId },
            reviewViewModel = reviewViewModel,
            onEditHome = ::editHome
        )
    }
}

@Preview
@Composable
fun StudentHomeScreenPreview() {
    MaterialTheme {
        StudentHomeScreenRoot(
            onNavigateToProfile = {}
        )
    }
}
