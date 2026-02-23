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
import androidx.compose.material3.LocalTextStyle
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
import com.good4.business.presentation.dashboard.BusinessDashboardScreen
import com.good4.business.presentation.dashboard.BusinessDashboardViewModel
import com.good4.business.presentation.products.BusinessProductsScreenRoot
import com.good4.business.presentation.products.BusinessProductsViewModel
import com.good4.business.presentation.profile.BusinessProfileScreen
import com.good4.business.presentation.verify.VerifyCodeScreen
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NavigationBar
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

