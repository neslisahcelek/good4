package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.*
import com.good4.dining.presentation.AkdenizDiningMenuState
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.*
import kotlin.math.roundToInt
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.akdeniz_campus_weather
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun CampusSummaryCards(
    diningMenuState: AkdenizDiningMenuState,
    onMenuClick: () -> Unit
) {
    var temperature by remember { mutableStateOf("—") }
    var weatherLabel by remember { mutableStateOf("Yükleniyor…") }
    LaunchedEffect(Unit) {
        val client = HttpClient { install(HttpTimeout) { requestTimeoutMillis = 10_000 } }
        try {
            // Central Antalya campus; no device location permission is needed.
            val response = client.get("https://api.open-meteo.com/v1/forecast?latitude=36.898&longitude=30.651&current=temperature_2m,weather_code&timezone=Europe%2FIstanbul")
            check(response.status.value in 200..299)
            val current = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("current").jsonObject
            temperature = "${current.getValue("temperature_2m").jsonPrimitive.double.roundToInt()}°"
            weatherLabel = when (current.getValue("weather_code").jsonPrimitive.int) {
                0 -> "Açık"
                1, 2 -> "Parçalı bulutlu"
                3 -> "Bulutlu"
                45, 48 -> "Sisli"
                in 51..67, in 80..82 -> "Yağmurlu"
                in 71..77, 85, 86 -> "Karlı"
                95, 96, 99 -> "Gök gürültülü"
                else -> "Hava durumu"
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            weatherLabel = "Şu an alınamıyor"
        } finally {
            client.close()
        }
    }
    val today = Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Istanbul")).date.toString()
    val menu = diningMenuState.menu?.days?.firstOrNull { it.date == today }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(Modifier.weight(1f).heightIn(min = 192.dp), shape = RoundedCornerShape(20.dp), color = PrimaryGreen, shadowElevation = 2.dp) {
            Box {
                Image(
                    painter = painterResource(Res.drawable.akdeniz_campus_weather),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.42f)))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Akdeniz Üniversitesi", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Antalya · Kampüs", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .8f))
                Text(temperature, style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(weatherLabel, style = MaterialTheme.typography.bodySmall, color = Color.White)
                Text("Open-Meteo", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .8f))
            }
            }
        }
        Surface(Modifier.weight(1f).heightIn(min = 192.dp).clickable(onClick = onMenuClick), shape = RoundedCornerShape(20.dp), color = SurfaceDefault, shadowElevation = 2.dp) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Default.Restaurant, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                Text("Günün Menüsü", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                if (menu != null) {
                    menu.meals.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                } else {
                    Text(if (diningMenuState.isLoading) "Yükleniyor…" else "Bugün için menü yayınlanmadı", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text("TL yükle ↗", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PrimaryGreen)
            }
        }
    }
}
