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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NavigationBar
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.product.presentation.product_list.ProductListViewModel
import com.good4.product.presentation.product_list.views.ProductListScreenRoot
import com.good4.student.presentation.reservations.ReservationUiModel
import com.good4.student.presentation.reservations.StudentReservationsScreen
import com.good4.student.presentation.reservations.StudentReservationsViewModel
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
    onNavigateToProfile: () -> Unit
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
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var reservationsScrollRequestKey by rememberSaveable { mutableIntStateOf(0) }
    var pendingReservationFromHome by remember { mutableStateOf<ReservationUiModel?>(null) }
    val productListViewModel: ProductListViewModel = koinViewModel()
    val reservationsViewModel: StudentReservationsViewModel = koinViewModel()
    val reservationsState by reservationsViewModel.state.collectAsStateWithLifecycle()

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
            1 -> reservationsViewModel.refresh()
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
                            selectedIconColor = PrimaryGreen,
                            selectedTextColor = PrimaryGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = PistachioGreen
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
                2 -> com.good4.community.CommunitiesScreen(onBack = { selectedItemIndex = 0 })
                0 -> {
                    ProductListScreenRoot(
                        viewModel = productListViewModel,
                        onCommunitiesClick = { selectedItemIndex = 2 },
                        onProfileClick = onNavigateToProfile,
                        onReservationCardClick = {
                            showReservationsTab()
                        }
                    )
                }

                1 -> {
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
            }
        }
    }
    if (menuOpen) {
        StudentMenuSheet(onDismiss = { menuOpen = false })
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
