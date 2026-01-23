package com.good4.admin.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
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
import com.good4.admin.presentation.campaigns.AdminCampaignsScreen
import com.good4.admin.presentation.dashboard.AdminDashboardScreen
import com.good4.admin.presentation.products.AdminProductsScreen
import com.good4.admin.presentation.profile.AdminProfileScreen
import com.good4.core.presentation.InkBlack
import com.good4.core.presentation.Background
import com.good4.core.presentation.MintGreen
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
            title = "Dashboard",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        AdminNavItem(
            title = "Ürünler",
            selectedIcon = Icons.Filled.List,
            unselectedIcon = Icons.Outlined.List
        ),
        AdminNavItem(
            title = "Kampanyalar",
            selectedIcon = Icons.Filled.Favorite,
            unselectedIcon = Icons.Outlined.Favorite
        ),
        AdminNavItem(
            title = "Profil",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person
        )
    )

    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                containerColor = Background
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
                            selectedIconColor = InkBlack,
                            selectedTextColor = InkBlack,
                            indicatorColor = MintGreen.copy(alpha = 0.3f)
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

