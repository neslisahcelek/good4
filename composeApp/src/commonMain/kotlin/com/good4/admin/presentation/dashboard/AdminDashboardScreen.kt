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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.Background
import com.good4.core.presentation.InkBlack
import com.good4.core.presentation.LimeGreen
import com.good4.core.presentation.components.StatCard
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
            .background(Background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Admin Panel",
                    fontWeight = FontWeight.SemiBold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Background
            )
        )

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = InkBlack)
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
                        text = "Özet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack
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
                            title = "Toplam Ürün",
                            value = state.totalProducts.toString(),
                            icon = Icons.Filled.Star,
                            color = InkBlack
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "İşletmeler",
                            value = state.totalBusinesses.toString(),
                            icon = Icons.Filled.Home,
                            color = LimeGreen
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
                            title = "Kampanyalar",
                            value = state.totalCampaigns.toString(),
                            icon = Icons.Filled.Check,
                            color = Color(0xFFFF9800)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Kullanıcılar",
                            value = state.totalUsers.toString(),
                            icon = Icons.Filled.Person,
                            color = Color(0xFF9C27B0)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Hızlı İşlemler",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkBlack
                    )
                    Text(
                        text = "Alt menüden ürün ve kampanya ekleyebilirsiniz",
                        fontSize = 14.sp,
                        color = Color.Gray
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
