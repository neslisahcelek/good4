package com.good4.dining.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.dining.domain.AkdenizDiningMenuDay
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.akdeniz_menu_calories
import good4.composeapp.generated.resources.akdeniz_menu_empty
import good4.composeapp.generated.resources.akdeniz_menu_title
import good4.composeapp.generated.resources.akdeniz_menu_today
import good4.composeapp.generated.resources.akdeniz_menu_today_empty
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
    val today = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PistachioGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.padding(9.dp).size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.akdeniz_menu_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                state.menu?.weekLabel?.takeIf { it.isNotBlank() }?.let { weekLabel ->
                    Text(
                        text = "Akdeniz Üniversitesi · $weekLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = SurfaceDefault,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                    )
                }

                else -> {
                    val todayMenu = state.menu.days.firstOrNull { it.date == today }
                    if (todayMenu == null) {
                        Text(
                            text = stringResource(Res.string.akdeniz_menu_today_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        DiningMenuDayCard(
                            day = todayMenu
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun DiningMenuDayCard(
    day: AkdenizDiningMenuDay,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
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
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = PistachioGreen
                ) {
                    Text(
                        text = stringResource(Res.string.akdeniz_menu_today),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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

private fun String.toTurkishDisplayDate(): String {
    val parts = split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else this
}
