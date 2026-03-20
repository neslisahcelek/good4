package com.good4.business.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.code.domain.CodeStatus
import com.good4.core.presentation.AccentYellow
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.StatCard
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.dashboard_code_prefix
import good4.composeapp.generated.resources.dashboard_completed
import good4.composeapp.generated.resources.dashboard_no_actions
import good4.composeapp.generated.resources.dashboard_pending
import good4.composeapp.generated.resources.dashboard_recent_actions
import good4.composeapp.generated.resources.dashboard_status_pending
import good4.composeapp.generated.resources.dashboard_status_used
import good4.composeapp.generated.resources.dashboard_total_products
import good4.composeapp.generated.resources.dashboard_welcome_prefix
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessDashboardViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Good4NestedScaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                titleContent = {
                    Text(
                        text = stringResource(Res.string.dashboard_welcome_prefix) + state.businessName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TextPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(Res.string.dashboard_pending),
                        value = state.pendingCount.toString(),
                        icon = Icons.Filled.DateRange,
                        color = AccentYellow
                    )
                }

                item {
                    StatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(Res.string.dashboard_completed),
                        value = state.completedCount.toString(),
                        icon = Icons.Filled.Check,
                        color = DeepGreen
                    )
                }

                item {
                    StatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(Res.string.dashboard_total_products),
                        value = state.totalProducts.toString(),
                        icon = Icons.Filled.Star,
                        color = TextPrimary
                    )
                }

                item {
                    Text(
                        text = stringResource(Res.string.dashboard_recent_actions),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                if (state.recentCodes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.dashboard_no_actions),
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(state.recentCodes) { code ->
                        RecentCodeCard(code = code)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCodeCard(
    modifier: Modifier = Modifier,
    code: RecentCodeUiModel
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDefault
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = code.productName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(Res.string.dashboard_code_prefix) + code.codeValue,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (code.statusEnum == CodeStatus.USED) DeepGreen.copy(alpha = 0.1f)
                        else PistachioGreen.copy(alpha = 0.3f)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (code.statusEnum == CodeStatus.USED) {
                        stringResource(Res.string.dashboard_status_used)
                    } else {
                        stringResource(Res.string.dashboard_status_pending)
                    },
                    fontSize = 12.sp,
                    color = if (code.statusEnum == CodeStatus.USED) DeepGreen else TextPrimary
                )
            }
        }
    }
}

data class RecentCodeUiModel(
    val id: String,
    val codeValue: String,
    val productName: String,
    val status: String
)

@Preview
@Composable
fun BusinessDashboardScreenPreview() {
    MaterialTheme {
        BusinessDashboardScreen()
    }
}


