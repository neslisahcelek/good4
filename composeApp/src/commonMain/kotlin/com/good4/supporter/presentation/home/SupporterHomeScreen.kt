package com.good4.supporter.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NavigationBar
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.supporter.presentation.cart.SupporterCartAction
import com.good4.supporter.presentation.cart.SupporterCartScreen
import com.good4.supporter.presentation.cart.SupporterCartViewModel
import com.good4.supporter.presentation.cart.totalItemCount
import com.good4.supporter.presentation.products.SupporterProductListScreenRoot
import com.good4.supporter.presentation.products.SupporterProductListViewModel
import com.good4.supporter.presentation.profile.SupporterProfileScreen
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.supporter_home_tab_cart
import good4.composeapp.generated.resources.supporter_home_tab_products
import good4.composeapp.generated.resources.supporter_home_tab_profile
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

data class SupporterNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun SupporterHomeScreenRoot(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onNavigateToOrderCode: (orderId: String) -> Unit
) {
    val cartViewModel: SupporterCartViewModel = koinViewModel()
    val productListViewModel: SupporterProductListViewModel = koinViewModel()

    val cartState by cartViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(cartState.createdOrderId) {
        cartState.createdOrderId?.let { orderId ->
            onNavigateToOrderCode(orderId)
            cartViewModel.onAction(SupporterCartAction.OnOrderNavigated)
        }
    }

    SupporterHomeScreen(
        modifier = modifier,
        cartViewModel = cartViewModel,
        productListViewModel = productListViewModel,
        onLogout = onLogout
    )
}

@Composable
fun SupporterHomeScreen(
    modifier: Modifier = Modifier,
    cartViewModel: SupporterCartViewModel,
    productListViewModel: SupporterProductListViewModel,
    onLogout: () -> Unit
) {
    val cartState by cartViewModel.state.collectAsStateWithLifecycle()
    val cartItemCounts = cartState.items.associate { it.product.documentId to it.quantity }
    val totalItemCount = cartState.totalItemCount

    val navItems = listOf(
        SupporterNavItem(
            title = stringResource(Res.string.supporter_home_tab_products),
            selectedIcon = Icons.Filled.Star,
            unselectedIcon = Icons.Outlined.Star
        ),
        SupporterNavItem(
            title = stringResource(Res.string.supporter_home_tab_cart),
            selectedIcon = Icons.Filled.ShoppingCart,
            unselectedIcon = Icons.Outlined.ShoppingCart
        ),
        SupporterNavItem(
            title = stringResource(Res.string.supporter_home_tab_profile),
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
                            SupporterNavIcon(
                                item = item,
                                isSelected = selectedItemIndex == index,
                                badge = if (index == 1 && totalItemCount > 0) totalItemCount else null
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
                0 -> SupporterProductListScreenRoot(
                    viewModel = productListViewModel,
                    cartItemCounts = cartItemCounts,
                    onAddToCart = { cartViewModel.onAction(SupporterCartAction.OnAddItem(it)) }
                )
                1 -> SupporterCartScreen(
                    state = cartState,
                    onAction = cartViewModel::onAction
                )
                2 -> SupporterProfileScreen(onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun SupporterNavIcon(
    modifier: Modifier = Modifier,
    item: SupporterNavItem,
    isSelected: Boolean,
    badge: Int?
) {
    val icon = if (isSelected) item.selectedIcon else item.unselectedIcon
    if (badge != null) {
        BadgedBox(
            modifier = modifier,
            badge = {
                Badge(containerColor = DeepGreen) {
                    Text(text = badge.toString(), fontSize = 10.sp, color = TextPrimary)
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = item.title)
        }
    } else {
        Icon(modifier = modifier, imageVector = icon, contentDescription = item.title)
    }
}

@Preview
@Composable
fun SupporterHomeScreenPreview() {
    MaterialTheme {}
}
