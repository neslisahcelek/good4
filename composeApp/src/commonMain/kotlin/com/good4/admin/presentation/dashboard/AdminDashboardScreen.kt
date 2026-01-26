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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.StatCard
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.admin_dashboard
import good4.composeapp.generated.resources.admin_dashboard_businesses
import good4.composeapp.generated.resources.admin_dashboard_campaigns
import good4.composeapp.generated.resources.admin_dashboard_quick_actions_subtitle
import good4.composeapp.generated.resources.admin_dashboard_quick_actions_title
import good4.composeapp.generated.resources.admin_dashboard_summary_title
import good4.composeapp.generated.resources.admin_dashboard_total_products
import good4.composeapp.generated.resources.admin_dashboard_users
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminDashboardViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Good4TopBar(title = stringResource(Res.string.admin_dashboard))
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(Res.string.admin_dashboard_summary_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(Res.string.admin_dashboard_total_products),
                            value = state.totalProducts.toString(),
                            icon = Icons.Filled.Star,
                            color = TextPrimary
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(Res.string.admin_dashboard_businesses),
                            value = state.totalBusinesses.toString(),
                            icon = Icons.Filled.Home,
                            color = DeepGreen
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(Res.string.admin_dashboard_campaigns),
                            value = state.totalCampaigns.toString(),
                            icon = Icons.Filled.Check,
                            color = AccentYellow
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(Res.string.admin_dashboard_users),
                            value = state.totalUsers.toString(),
                            icon = Icons.Filled.Person,
                            color = PistachioGreen
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.admin_dashboard_quick_actions_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(Res.string.admin_dashboard_quick_actions_subtitle),
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
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
