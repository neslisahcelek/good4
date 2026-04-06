package com.good4.admin.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AccentYellow
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.ProfileTopBarAction
import com.good4.core.presentation.components.StatCard
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.admin_dashboard
import good4.composeapp.generated.resources.admin_dashboard_active_products
import good4.composeapp.generated.resources.admin_dashboard_businesses
import good4.composeapp.generated.resources.admin_dashboard_campaigns
import good4.composeapp.generated.resources.admin_dashboard_stock_suffix
import good4.composeapp.generated.resources.admin_dashboard_total_products
import good4.composeapp.generated.resources.admin_dashboard_users
import good4.composeapp.generated.resources.admin_products_empty
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminDashboardViewModel = koinViewModel(),
    onMenuClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.refreshDashboard()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Good4TopBar(
            title = stringResource(Res.string.admin_dashboard),
            navigationIcon = {
                if (onMenuClick != null) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = null)
                    }
                }
            },
            actions = {
                if (onProfileClick != null) {
                    ProfileTopBarAction(onClick = onProfileClick)
                }
            }
        )
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TextPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    state.errorMessage?.let { error ->
                        Text(
                            text = error,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                item {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(Res.string.admin_dashboard_total_products),
                        value = state.totalProducts.toString(),
                        icon = Icons.Filled.Star,
                        color = TextPrimary
                    )
                }

                item {
                    StatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(Res.string.admin_dashboard_businesses),
                        value = state.totalBusinesses.toString(),
                        icon = Icons.Filled.Home,
                        color = DeepGreen
                    )
                }


                item {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(Res.string.admin_dashboard_campaigns),
                        value = state.totalCampaigns.toString(),
                        icon = Icons.Filled.Check,
                        color = AccentYellow
                    )
                }


                item {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(Res.string.admin_dashboard_users),
                        value = state.totalUsers.toString(),
                        icon = Icons.Filled.Person,
                        color = PistachioGreen
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.admin_dashboard_active_products),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                if (state.activeProducts.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.admin_products_empty),
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    items(
                        items = state.activeProducts,
                        key = { it.id }
                    ) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDefault),
                            content = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = product.name,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${product.stock} ${stringResource(Res.string.admin_dashboard_stock_suffix)}",
                                        color = TextSecondary
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AdminDashboardScreenPreview() {
    MaterialTheme {
        AdminDashboardScreen()
    }
}
