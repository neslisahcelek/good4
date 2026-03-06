package com.good4.student.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NavigationBar
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.product.presentation.product_list.ProductListViewModel
import com.good4.product.presentation.product_list.views.ProductListScreenRoot
import com.good4.student.presentation.profile.StudentProfileScreen
import com.good4.student.presentation.reservations.StudentReservationsScreen
import com.good4.student.presentation.reservations.StudentReservationsViewModel
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.student_profile
import good4.composeapp.generated.resources.student_reservations
import good4.composeapp.generated.resources.student_home
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
    onLogout: () -> Unit
) {
    val navItems = listOf(
        BottomNavItem(
            title = stringResource(Res.string.student_reservations),
            selectedIcon = Icons.Filled.ShoppingCart,
            unselectedIcon = Icons.Outlined.ShoppingCart
        ),
        BottomNavItem(
            title = stringResource(Res.string.student_home),
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            title = stringResource(Res.string.student_profile),
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person
        )
    )

    var selectedItemIndex by rememberSaveable { mutableIntStateOf(1) }
    val productListViewModel: ProductListViewModel = koinViewModel()
    val reservationsViewModel: StudentReservationsViewModel = koinViewModel()

    LaunchedEffect(selectedItemIndex) {
        when (selectedItemIndex) {
            0 -> reservationsViewModel.refresh()
            1 -> productListViewModel.refresh()
        }
    }

    Good4Scaffold(
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
                        label = { 
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedItemIndex == index) FontWeight.Medium else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepGreen,
                            selectedTextColor = DeepGreen,
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
                0 -> {
                    StudentReservationsScreen(
                        viewModel = reservationsViewModel
                    )
                }
                1 -> {
                    ProductListScreenRoot(
                        viewModel = productListViewModel
                    )
                }
                2 -> {
                    StudentProfileScreen(
                        onLogout = onLogout
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
            onLogout = {}
        )
    }
}

