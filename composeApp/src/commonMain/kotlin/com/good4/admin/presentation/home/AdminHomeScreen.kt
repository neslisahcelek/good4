package com.good4.admin.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.good4.admin.presentation.campaigns.AdminCampaignsScreen
import com.good4.admin.presentation.dashboard.AdminDashboardScreen
import com.good4.admin.presentation.products.AdminProductsScreen
import com.good4.admin.presentation.profile.AdminProfileScreen
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NavigationBar
import com.good4.core.presentation.components.Good4Scaffold
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.admin_nav_campaigns
import good4.composeapp.generated.resources.admin_nav_dashboard
import good4.composeapp.generated.resources.admin_nav_products
import good4.composeapp.generated.resources.admin_nav_profile
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

data class AdminNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AdminHomeScreenRoot(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    val navItems = listOf(
        AdminNavItem(
            title = stringResource(Res.string.admin_nav_dashboard),
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        AdminNavItem(
            title = stringResource(Res.string.admin_nav_products),
            selectedIcon = Icons.Filled.Menu,
            unselectedIcon = Icons.Outlined.Menu
        ),
        AdminNavItem(
            title = stringResource(Res.string.admin_nav_campaigns),
            selectedIcon = Icons.Filled.Favorite,
            unselectedIcon = Icons.Outlined.Favorite
        ),
        AdminNavItem(
            title = stringResource(Res.string.admin_nav_profile),
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person
        )
    )

    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

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
                0 -> AdminDashboardScreen()
                1 -> AdminProductsScreen()
                2 -> AdminCampaignsScreen()
                3 -> AdminProfileScreen(onLogout = onLogout)
            }
        }
    }
}

@Preview
@Composable
fun AdminHomeScreenPreview() {
    MaterialTheme {
        AdminHomeScreenRoot(onLogout = {})
    }
}
