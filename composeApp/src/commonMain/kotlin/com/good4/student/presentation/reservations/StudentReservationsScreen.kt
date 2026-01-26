package com.good4.student.presentation.reservations

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.code.domain.CodeStatus
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.emoji_ticket
import good4.composeapp.generated.resources.preview_business_name
import good4.composeapp.generated.resources.preview_product_name
import good4.composeapp.generated.resources.reservation_expired_short
import good4.composeapp.generated.resources.reservation_status_completed
import good4.composeapp.generated.resources.reservation_status_expired
import good4.composeapp.generated.resources.reservation_status_pending
import good4.composeapp.generated.resources.student_reservations_code_title
import good4.composeapp.generated.resources.student_reservations_credit_label
import good4.composeapp.generated.resources.student_reservations_credit_reset_prefix
import good4.composeapp.generated.resources.student_reservations_credit_reset_suffix
import good4.composeapp.generated.resources.student_reservations_empty_subtitle
import good4.composeapp.generated.resources.student_reservations_empty_title
import good4.composeapp.generated.resources.student_reservations_remaining_prefix
import good4.composeapp.generated.resources.student_reservations_title
import good4.composeapp.generated.resources.time_minute_suffix
import good4.composeapp.generated.resources.time_second_suffix
import good4.composeapp.generated.resources.verify_code_placeholder
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentReservationsScreen(
    modifier: Modifier = Modifier,
    viewModel: StudentReservationsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StudentReservationsContent(
        modifier = modifier,
        state = state
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentReservationsContent(
    modifier: Modifier = Modifier,
    state: StudentReservationsState
) {
    Good4Scaffold(
        modifier = modifier,
        topBar = { Good4TopBar(title = stringResource(Res.string.student_reservations_title)) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
        ) {
            state.remainingCredit?.let { credit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PistachioGreen
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.student_reservations_credit_label),
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = credit.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
                if (credit == 0 && state.creditResetIntervalDays != null) {
                    val resetText =
                        stringResource(Res.string.student_reservations_credit_reset_prefix) +
                                state.creditResetIntervalDays +
                                stringResource(Res.string.student_reservations_credit_reset_suffix)
                    Text(
                        text = resetText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TextPrimary)
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
                                text = stringResource(Res.string.emoji_ticket),
                                fontSize = 64.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(Res.string.student_reservations_empty_title),
                                fontSize = 18.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.student_reservations_empty_subtitle),
                                fontSize = 14.sp,
                                color = TextSecondary,
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
}

@Composable
private fun ReservationCard(
    modifier: Modifier = Modifier,
    reservation: ReservationUiModel
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDefault
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.12f))
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
                    color = TextPrimary
                )
                StatusBadge(status = reservation.statusEnum)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Store,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = reservation.businessName,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (reservation.statusEnum == CodeStatus.PENDING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceMuted)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.student_reservations_code_title),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = reservation.code,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 4.sp
                        )
                        if (reservation.remainingTime.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(Res.string.student_reservations_remaining_prefix) +
                                            reservation.remainingTime,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
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
    status: CodeStatus
) {
    val (backgroundColor, textColor, text) = when (status) {
        CodeStatus.PENDING -> Triple(
            PistachioGreen,
            TextPrimary,
            stringResource(Res.string.reservation_status_pending)
        )

        CodeStatus.USED -> Triple(
            SurfaceMuted,
            TextPrimary,
            stringResource(Res.string.reservation_status_completed)
        )

        CodeStatus.EXPIRED -> Triple(
            ErrorRed.copy(alpha = 0.12f),
            ErrorRed,
            stringResource(Res.string.reservation_status_expired)
        )
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
        val productName = stringResource(Res.string.preview_product_name)
        val businessName = stringResource(Res.string.preview_business_name)
        val codeValue = stringResource(Res.string.verify_code_placeholder)
        val minuteSuffix = stringResource(Res.string.time_minute_suffix)
        val secondSuffix = stringResource(Res.string.time_second_suffix)
        val remainingTime = "12${minuteSuffix} 30${secondSuffix}"
        val sampleReservations = listOf(
            ReservationUiModel(
                id = "preview_1",
                code = codeValue,
                productName = productName,
                businessName = businessName,
                status = CodeStatus.PENDING.value,
                remainingTime = remainingTime
            ),
            ReservationUiModel(
                id = "preview_2",
                code = codeValue,
                productName = productName,
                businessName = businessName,
                status = CodeStatus.USED.value,
                remainingTime = stringResource(Res.string.reservation_expired_short)
            )
        )
        StudentReservationsContent(
            state = StudentReservationsState(
                reservations = sampleReservations,
                remainingCredit = 2,
                creditResetIntervalDays = 7
            )
        )
    }
}




