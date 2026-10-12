package com.good4.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.*

/** Shared ticket appearance for community coupons and Good4 business discounts. */
@Composable
fun CouponTicketCard(
    title: String,
    subtitle: String,
    amount: String?,
    stubLabel: String,
    deadlineLabel: String?,
    actionLabel: String,
    urgent: Boolean = false,
    prominentMetadata: Boolean = false,
    modifier: Modifier = Modifier.width(316.dp),
    onClick: () -> Unit
) {
    val stubColor = if (amount == null) SurfaceMuted else PistachioGreen
    val stubInk = if (amount == null) DraftAccent else MaterialTheme.colorScheme.primary
    val dashColor = BorderMuted
    Surface(
        onClick = onClick,
        modifier = modifier.then(
            if (prominentMetadata) Modifier.heightIn(min = 164.dp) else Modifier.height(104.dp)
        ),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDefault,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.35f))
    ) {
        Row(
            if (prominentMetadata) Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            else Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.width(84.dp).fillMaxHeight().background(stubColor),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (amount == null) {
                    Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = stubInk, modifier = Modifier.size(26.dp))
                } else {
                    Text(
                        amount,
                        fontSize = if (amount.length > 5) 20.sp else 26.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = stubInk,
                        maxLines = 1
                    )
                }
                Text(
                    stubLabel,
                    fontSize = 12.sp,
                    color = stubInk
                )
            }
            // Perforation between the stub and the body.
            androidx.compose.foundation.Canvas(Modifier.width(1.5.dp).fillMaxHeight()) {
                drawLine(
                    color = dashColor,
                    start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height),
                    strokeWidth = size.width,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(if (prominentMetadata) 4.dp else 0.dp)) {
                    Text(
                        title,
                        fontSize = if (prominentMetadata) 18.sp else 15.sp,
                        fontWeight = if (prominentMetadata) FontWeight.Bold else FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = if (prominentMetadata) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        fontSize = if (prominentMetadata) 14.sp else 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (prominentMetadata) TextPrimary else TextSecondary,
                        maxLines = if (prominentMetadata) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (prominentMetadata && deadlineLabel != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(deadlineLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!prominentMetadata && deadlineLabel != null) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = if (urgent) DraftAccent else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            deadlineLabel,
                            fontSize = 12.sp,
                            fontWeight = if (urgent) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (urgent) DraftAccent else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                        Text(
                            actionLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
