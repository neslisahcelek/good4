package com.good4.student.presentation.reservations

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.DarkBlue
import com.good4.core.presentation.DesertWhite
import com.good4.core.presentation.LightGreen
import com.good4.core.presentation.PrimaryGreen
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentReservationsScreen(
    modifier: Modifier = Modifier,
    viewModel: StudentReservationsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DesertWhite)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Rezervasyonlarım",
                    fontWeight = FontWeight.SemiBold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DesertWhite
            )
        )

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DarkBlue)
                }
            }
            state.reservations.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎫",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Henüz rezervasyonun yok",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bir ürün rezerve ettiğinde\nburada görünecek",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.reservations,
                        key = { it.id }
                    ) { reservation ->
                        ReservationCard(reservation = reservation)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationCard(
    modifier: Modifier = Modifier,
    reservation: ReservationUiModel
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reservation.productName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = DarkBlue
                )
                StatusBadge(status = reservation.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reservation.businessName,
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Kod gösterimi
            if (reservation.statusEnum == CodeStatus.PENDING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightGreen.copy(alpha = 0.2f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Kodun",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = reservation.code,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlue,
                            letterSpacing = 4.sp
                        )
                        if (reservation.remainingTime.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Kalan süre: ${reservation.remainingTime}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    modifier: Modifier = Modifier,
    status: String
) {
    val (backgroundColor, textColor, text) = when (status) {
        "pending" -> Triple(LightGreen.copy(alpha = 0.2f), PrimaryGreen, "Bekliyor")
        "completed" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Tamamlandı")
        "expired" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Süresi Doldu")
        "cancelled" -> Triple(Color(0xFFF5F5F5), Color.Gray, "İptal Edildi")
        else -> Triple(Color.LightGray, Color.Gray, status)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

data class ReservationUiModel(
    val id: String,
    val code: String,
    val productName: String,
    val businessName: String,
    val status: String,
    val remainingTime: String,
    val createdAt: String? = null
)

@Preview
@Composable
fun StudentReservationsScreenPreview() {
    MaterialTheme {
        // Preview
    }
}

