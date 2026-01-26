package com.good4.business.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.good4.business.presentation.dashboard.BusinessDashboardScreen
import com.good4.business.presentation.dashboard.BusinessDashboardViewModel
import com.good4.business.presentation.products.BusinessProductsScreenRoot
import com.good4.business.presentation.products.BusinessProductsViewModel
import com.good4.business.presentation.profile.BusinessProfileScreen
import com.good4.business.presentation.verify.VerifyCodeScreen
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.components.Good4Scaffold
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.nav_dashboard
import good4.composeapp.generated.resources.nav_products
import good4.composeapp.generated.resources.nav_profile
import good4.composeapp.generated.resources.nav_verify_code
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

data class BusinessNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun BusinessHomeScreenRoot(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    val navItems = listOf(
        BusinessNavItem(
            title = stringResource(Res.string.nav_dashboard),
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BusinessNavItem(
            title = stringResource(Res.string.nav_verify_code),
            selectedIcon = Icons.Filled.Search,
            unselectedIcon = Icons.Outlined.Search
        ),
        BusinessNavItem(
            title = stringResource(Res.string.nav_products),
            selectedIcon = Icons.Filled.Store,
            unselectedIcon = Icons.Outlined.Store
        ),
        BusinessNavItem(
            title = stringResource(Res.string.nav_profile),
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person
        )
    )

    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    val dashboardViewModel: BusinessDashboardViewModel = koinViewModel()
    val productsViewModel: BusinessProductsViewModel = koinViewModel()

    LaunchedEffect(selectedItemIndex) {
        when (selectedItemIndex) {
            0 -> dashboardViewModel.refreshDashboard()
            2 -> productsViewModel.refreshProducts()
        }
    }

    Good4Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                containerColor = AppBackground
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
                            selectedIconColor = TextPrimary,
                            selectedTextColor = TextPrimary,
                            indicatorColor = PistachioGreen.copy(alpha = 0.3f)
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
                0 -> BusinessDashboardScreen(viewModel = dashboardViewModel)
                1 -> VerifyCodeScreen()
                2 -> BusinessProductsScreenRoot(viewModel = productsViewModel)
                3 -> BusinessProfileScreen(onLogout = onLogout)
            }
        }
    }
}

@Preview
@Composable
fun BusinessHomeScreenPreview() {
    MaterialTheme {
        BusinessHomeScreenRoot(onLogout = {})
    }
}

