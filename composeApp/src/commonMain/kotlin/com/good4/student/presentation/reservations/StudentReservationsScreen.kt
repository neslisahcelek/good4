package com.good4.student.presentation.reservations

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.code.domain.CodeStatus
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.ProfileTopBarAction
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.ReservationCard
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.cancel
import good4.composeapp.generated.resources.emoji_ticket
import good4.composeapp.generated.resources.preview_address
import good4.composeapp.generated.resources.preview_business_name
import good4.composeapp.generated.resources.preview_product_name
import good4.composeapp.generated.resources.reservation_expired_short
import good4.composeapp.generated.resources.student_reservations_credit_label
import good4.composeapp.generated.resources.student_reservations_credit_reset_text
import good4.composeapp.generated.resources.student_reservations_empty_subtitle
import good4.composeapp.generated.resources.student_reservations_empty_title
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
    viewModel: StudentReservationsViewModel = koinViewModel(),
    onProfileClick: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StudentReservationsContent(
        modifier = modifier,
        state = state,
        onProfileClick = onProfileClick,
        onCancelReservation = viewModel::cancelReservation
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentReservationsContent(
    modifier: Modifier = Modifier,
    state: StudentReservationsState,
    onProfileClick: (() -> Unit)? = null,
    onCancelReservation: (String) -> Unit
) {
    Good4NestedScaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.student_reservations_title),
                actions = {
                    if (onProfileClick != null) {
                        ProfileTopBarAction(onClick = onProfileClick)
                    }
                }
            )
        }
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.student_reservations_credit_reset_text),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
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
                            ReservationItem(
                                reservation = reservation,
                                onCancelReservation = onCancelReservation
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationItem(
    modifier: Modifier = Modifier,
    reservation: ReservationUiModel,
    onCancelReservation: (String) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ReservationCard(
            title = null,
            productName = reservation.productName,
            businessName = reservation.businessName,
            businessAddress = reservation.businessAddress.ifBlank { null },
            businessAddressUrl = reservation.businessAddressUrl.ifBlank { null },
            status = reservation.statusEnum,
            code = reservation.code,
            remainingTime = reservation.remainingTime.ifBlank { null },
            showCancelButton = reservation.statusEnum == CodeStatus.PENDING,
            cancelButtonLabel = stringResource(Res.string.cancel),
            onCancelClick = { onCancelReservation(reservation.id) }
        )
    }
}

data class ReservationUiModel(
    val id: String,
    val code: String,
    val productName: String,
    val businessName: String,
    val businessAddress: String = "",
    val businessAddressUrl: String = "",
    val status: String,
    val remainingTime: String,
    val createdAt: Long? = null
)

@Preview
@Composable
fun StudentReservationsScreenPreview() {
    MaterialTheme {
        val productName = stringResource(Res.string.preview_product_name)
        val businessName = stringResource(Res.string.preview_business_name)
        val businessAddress = stringResource(Res.string.preview_address)
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
                businessAddress = businessAddress,
                businessAddressUrl = "",
                status = CodeStatus.PENDING.value,
                remainingTime = remainingTime
            ),
            ReservationUiModel(
                id = "preview_2",
                code = codeValue,
                productName = productName,
                businessName = businessName,
                businessAddress = businessAddress,
                businessAddressUrl = "",
                status = CodeStatus.USED.value,
                remainingTime = stringResource(Res.string.reservation_expired_short)
            )
        )
        StudentReservationsContent(
            state = StudentReservationsState(
                reservations = sampleReservations,
                remainingCredit = 2
            ),
            onCancelReservation = {}
        )
    }
}

