package com.good4.core.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.code.domain.CodeStatus
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.util.openMaps
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.reservation_status_cancelled
import good4.composeapp.generated.resources.reservation_status_completed
import good4.composeapp.generated.resources.reservation_status_expired
import good4.composeapp.generated.resources.reservation_status_pending
import good4.composeapp.generated.resources.student_reservations_code_title
import good4.composeapp.generated.resources.student_reservations_remaining_prefix
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReservationCard(
    modifier: Modifier = Modifier,
    title: String?,
    productName: String,
    businessName: String,
    businessAddress: String? = null,
    businessAddressUrl: String? = null,
    status: CodeStatus,
    code: String?,
    remainingTime: String?,
    showCancelButton: Boolean = false,
    cancelButtonLabel: String? = null,
    onCancelClick: (() -> Unit)? = null
) {
    val displayAddress = toDisplayAddressOrNull(businessAddress)
    val mapsAddress = businessAddressUrl?.takeIf { it.isNotBlank() }

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
            if (!title.isNullOrBlank()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = productName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                StatusBadge(status = status)
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
                    text = businessName,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            if (displayAddress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (mapsAddress != null) {
                                Modifier.clickable { openMaps(mapsAddress) }
                            } else {
                                Modifier
                            }
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayAddress,
                        fontSize = 14.sp,
                        textDecoration = if (mapsAddress != null) TextDecoration.Underline else null
                    )
                }
            }

            if (status == CodeStatus.PENDING && code != null) {
                Spacer(modifier = Modifier.height(12.dp))

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
                            text = code,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 4.sp
                        )
                        if (!remainingTime.isNullOrBlank()) {
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
                                            remainingTime,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            if (
                showCancelButton &&
                status == CodeStatus.PENDING &&
                !cancelButtonLabel.isNullOrBlank() &&
                onCancelClick != null
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCancelClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = SurfaceDefault
                    )
                ) {
                    Text(
                        text = cancelButtonLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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

        CodeStatus.CANCELLED -> Triple(
            ErrorRed.copy(alpha = 0.12f),
            ErrorRed,
            stringResource(Res.string.reservation_status_cancelled)
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
