package com.good4.student.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
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
            title = stringResource(Res.string.student_reservations),
            selectedIcon = Icons.Filled.ShoppingCart,
            unselectedIcon = Icons.Outlined.ShoppingCart
        )
    )

    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    var reservationsScrollRequestKey by rememberSaveable { mutableIntStateOf(0) }
    var pendingReservationFromHome by remember { mutableStateOf<ReservationUiModel?>(null) }
    val productListViewModel: ProductListViewModel = koinViewModel()
    val reservationsViewModel: StudentReservationsViewModel = koinViewModel()
    val reservationsState by reservationsViewModel.state.collectAsStateWithLifecycle()

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
                        selected = selectedItemIndex == index,
                        onClick = { selectedItemIndex = index },
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
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TextPrimary,
                            selectedTextColor = TextPrimary,
                            unselectedIconColor = TextPrimary,
                            unselectedTextColor = TextPrimary,
                            indicatorColor = SurfaceDefault.copy(alpha = 0.95f)
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
                0 -> {
                    ProductListScreenRoot(
                        viewModel = productListViewModel,
                        onProfileClick = onNavigateToProfile,
                        onReservationCardClick = {
                            val productState = productListViewModel.state.value
                            val activeReservation = productState.activeReservation
                            reservationsScrollRequestKey += 1
                            if (activeReservation != null) {
                                pendingReservationFromHome = productState.toPendingReservationUiModel()
                            }
                            selectedItemIndex = 1
                        }
                    )
                }

                1 -> {
                    StudentReservationsScreen(
                        viewModel = reservationsViewModel,
                        scrollToTopRequestKey = reservationsScrollRequestKey,
                        prioritizedReservation = pendingReservationFromHome,
                        onProfileClick = onNavigateToProfile
                    )
                }
            }
        }
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
