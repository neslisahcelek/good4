package com.good4.dining.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.dining.domain.AkdenizDiningMenuDay
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.akdeniz_menu_calories
import good4.composeapp.generated.resources.akdeniz_menu_empty
import good4.composeapp.generated.resources.akdeniz_menu_title
import good4.composeapp.generated.resources.akdeniz_menu_today
import good4.composeapp.generated.resources.akdeniz_menu_top_up
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

const val AKDENIZ_BALANCE_URL =
    "https://sks.akdeniz.edu.tr/tr/merkez_yemekhane_bakiye_yukleme_sistemi-12936"

@Composable
fun AkdenizDiningMenuCard(
    state: AkdenizDiningMenuState,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val today = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = PrimaryGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.akdeniz_menu_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    state.menu?.weekLabel?.takeIf { it.isNotBlank() }?.let { weekLabel ->
                        Text(
                            text = weekLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = PrimaryGreen
                    )
                }

                state.menu == null -> {
                    Text(
                        text = stringResource(Res.string.akdeniz_menu_empty),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                else -> {
                    val sortedDays = state.menu.days.sortedBy { it.date }
                    val todayIndex = sortedDays.indexOfFirst { it.date == today }
                        .coerceAtLeast(0)
                    val menuListState = rememberLazyListState(
                        initialFirstVisibleItemIndex = todayIndex
                    )
                    LazyRow(
                        state = menuListState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sortedDays, key = { it.date }) { day ->
                            DiningMenuDayCard(
                                day = day,
                                isToday = day.date == today
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { uriHandler.openUri(AKDENIZ_BALANCE_URL) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.akdeniz_menu_top_up),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DiningMenuDayCard(
    day: AkdenizDiningMenuDay,
    isToday: Boolean
) {
    Surface(
        modifier = Modifier.width(250.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isToday) PistachioGreen else SurfaceMuted,
        border = BorderStroke(
            width = 1.dp,
            color = if (isToday) PrimaryGreen else BorderMuted
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.dayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (isToday) {
                    Text(
                        text = stringResource(Res.string.akdeniz_menu_today),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
            }
            Text(
                text = day.date.toTurkishDisplayDate(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            day.meals.forEach { meal ->
                Text(
                    text = "• $meal",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            day.calories?.let { calories ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.akdeniz_menu_calories, calories),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun String.toTurkishDisplayDate(): String {
    val parts = split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else this
}
