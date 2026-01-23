package com.good4.business.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import com.good4.code.domain.CodeStatus
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.InkBlack
import com.good4.core.presentation.Background
import com.good4.core.presentation.MintGreen
import com.good4.core.presentation.LimeGreen
import com.good4.core.presentation.SlateGray
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.dashboard_code_prefix
import good4.composeapp.generated.resources.dashboard_completed
import good4.composeapp.generated.resources.dashboard_no_actions
import good4.composeapp.generated.resources.dashboard_pending
import good4.composeapp.generated.resources.dashboard_recent_actions
import good4.composeapp.generated.resources.dashboard_status_pending
import good4.composeapp.generated.resources.dashboard_status_used
import good4.composeapp.generated.resources.dashboard_title
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

    Scaffold(
        modifier = modifier,
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.dashboard_welcome_prefix) + state.businessName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
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
                CircularProgressIndicator(color = InkBlack)
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
                        color = Color(0xFFFF9800)
                    )
                }

                item {
                    StatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(Res.string.dashboard_completed),
                        value = state.completedCount.toString(),
                        icon = Icons.Filled.Check,
                        color = LimeGreen
                    )
                }

                item {
                    StatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(Res.string.dashboard_total_products),
                        value = state.totalProducts.toString(),
                        icon = Icons.Filled.Star,
                        color = InkBlack
                    )
                }

                item {
                    Text(
                        text = stringResource(Res.string.dashboard_recent_actions),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkBlack
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
                                color = SlateGray
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
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.padding(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack
                )
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
            containerColor = Color.White
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
                    color = Color.Gray
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (code.statusEnum == CodeStatus.USED) LimeGreen.copy(alpha = 0.1f)
                        else MintGreen.copy(alpha = 0.3f)
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
                    color = if (code.statusEnum == CodeStatus.USED) LimeGreen else InkBlack
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
