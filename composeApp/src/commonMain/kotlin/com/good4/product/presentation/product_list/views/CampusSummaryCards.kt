package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(Modifier.weight(1f).height(116.dp), shape = RoundedCornerShape(16.dp), color = PrimaryGreen, shadowElevation = 1.dp) {
            Box {
                Image(
                    painter = painterResource(Res.drawable.akdeniz_campus_weather),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.34f)))
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Akdeniz Üniversitesi", fontSize = 13.sp, lineHeight = 16.sp, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text("Antalya · Kampüs", fontSize = 11.sp, lineHeight = 14.sp, color = Color.White.copy(alpha = .86f))
                    Spacer(Modifier.weight(1f))
                    Text(temperature, fontSize = 34.sp, lineHeight = 36.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    Text(weatherLabel, fontSize = 12.sp, lineHeight = 14.sp, color = Color.White)
                }
            }
        }
        Surface(Modifier.weight(1f).height(116.dp).clickable(onClick = onMenuClick), shape = RoundedCornerShape(16.dp), color = SurfaceDefault, shadowElevation = 1.dp) {
            Box(Modifier.fillMaxSize()) {
                Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = PrimaryGreen.copy(alpha = .13f), modifier = Modifier.align(Alignment.BottomEnd).offset(x = 12.dp, y = 12.dp).size(76.dp))
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Günün Menüsü", fontSize = 14.sp, lineHeight = 17.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                if (menu != null) {
                        menu.meals.take(3).forEach { Text(it, fontSize = 10.5.sp, lineHeight = 13.sp, color = TextSecondary, maxLines = 1) }
                } else {
                        Text(if (diningMenuState.isLoading) "Yükleniyor…" else "Bugün için menü yayınlanmadı", fontSize = 11.sp, lineHeight = 14.sp, color = TextSecondary)
                }
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TL yükle", fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, color = PrimaryGreen)
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Outlined.AddCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
