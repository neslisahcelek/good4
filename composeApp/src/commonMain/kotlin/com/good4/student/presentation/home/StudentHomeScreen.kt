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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.good4.core.presentation.DarkBlue
import com.good4.core.presentation.DesertWhite
import com.good4.core.presentation.LightGreen
import com.good4.product.presentation.product_list.views.ProductListScreenRoot
import com.good4.product.presentation.product_list.ProductListViewModel
import com.good4.student.presentation.profile.StudentProfileScreen
import com.good4.student.presentation.reservations.StudentReservationsScreen
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.products
import good4.composeapp.generated.resources.reservations
import good4.composeapp.generated.resources.profile
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
            title = stringResource(Res.string.products),
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            title = stringResource(Res.string.reservations),
            selectedIcon = Icons.Filled.ShoppingCart,
            unselectedIcon = Icons.Outlined.ShoppingCart
        ),
        BottomNavItem(
            title = stringResource(Res.string.profile),
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person
        )
    )

    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                containerColor = DesertWhite
            ) {
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
                        label = { Text(item.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkBlue,
                            selectedTextColor = DarkBlue,
                            indicatorColor = LightGreen.copy(alpha = 0.3f)
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
                    val viewModel: ProductListViewModel = koinViewModel()
                    ProductListScreenRoot(
                        viewModel = viewModel
                    )
                }
                1 -> {
                    StudentReservationsScreen()
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

