package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.product_list_credit_label
import good4.composeapp.generated.resources.product_list_delivery_suffix
import good4.composeapp.generated.resources.product_list_greeting_prefix
import good4.composeapp.generated.resources.product_list_greeting_suffix
import good4.composeapp.generated.resources.product_list_renewal_days_suffix
import good4.composeapp.generated.resources.product_list_renewal_hours_suffix
import good4.composeapp.generated.resources.product_list_renewal_prefix
import good4.composeapp.generated.resources.product_list_renewal_soon
import good4.composeapp.generated.resources.product_list_renewal_text_suffix
import good4.composeapp.generated.resources.product_list_subtitle
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration

@Composable
fun StudentStatusCard(
    modifier: Modifier = Modifier,
    userName: String,
    remainingCredits: Int,
    renewalDuration: Duration?,
    deliveryTimeMinutes: Int
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = PrimaryGreen
            )
            .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 28.dp)
    ) {
        Text(
            text = stringResource(Res.string.product_list_greeting_prefix) +
                    userName +
                    stringResource(Res.string.product_list_greeting_suffix),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = SurfaceDefault
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(Res.string.product_list_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = SurfaceDefault.copy(alpha = 0.85f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = SurfaceDefault.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = SurfaceDefault.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = SurfaceDefault,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(Res.string.product_list_credit_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = SurfaceDefault.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = remainingCredits.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = SurfaceDefault,
                            fontWeight = FontWeight.Bold
                        )
                    }


                    Box(
                        modifier = Modifier
                            .background(
                                color = SurfaceDefault.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = SurfaceDefault,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = deliveryTimeMinutes.toString() +
                                        stringResource(Res.string.product_list_delivery_suffix),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SurfaceDefault
                            )
                        }
                    }
                }

                if (renewalDuration != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SurfaceDefault.copy(alpha = 0.2f))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = SurfaceDefault.copy(alpha = 0.75f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(Res.string.product_list_renewal_prefix) +
                                    formatRenewalDuration(
                                        duration = renewalDuration,
                                        daysSuffix = stringResource(Res.string.product_list_renewal_days_suffix),
                                        hoursSuffix = stringResource(Res.string.product_list_renewal_hours_suffix),
                                        soon = stringResource(Res.string.product_list_renewal_soon),
                                        suffix = stringResource(Res.string.product_list_renewal_text_suffix)
                                    ),
                            style = MaterialTheme.typography.bodySmall,
                            color = SurfaceDefault.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatRenewalDuration(
    duration: Duration,
    daysSuffix: String,
    hoursSuffix: String,
    soon: String,
    suffix: String
): String {
    return duration.toComponents { days, hours, _, _, _ ->
        buildString {
            if (days > 0L) append("$days$daysSuffix")
            if (hours > 0) append("$hours$hoursSuffix")
            if (days == 0L && hours == 0) append(soon)
            append(suffix)
        }.trim()
    }
}

@Preview
@Composable
fun Preview() {
    MaterialTheme {
        StudentStatusCard(
            userName = "Neslişah",
            remainingCredits = 5,
            renewalDuration = Duration.ZERO,
            deliveryTimeMinutes = 15
        )
    }
}