package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.*
import com.good4.dining.domain.DailyMeal
import com.good4.dining.presentation.AkdenizDiningMenuState
import com.good4.dining.presentation.DailyMenuWidget
import com.good4.weather.CampusWeatherRepository
import org.koin.compose.koinInject
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.math.roundToInt
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.akdeniz_campus_weather
import good4.composeapp.generated.resources.akdeniz_campus_weather_night
import org.jetbrains.compose.resources.painterResource

private data class CampusWeather(val temperature: String, val label: String)

// Process-wide so returning to the home tab reuses the last reading instead of
// hitting Firestore and showing "Yükleniyor…" on every visit.
private object CampusWeatherCache {
    private const val MAX_AGE_MILLIS = 15 * 60 * 1000L
    // The server refreshes every 30 minutes; anything older means the job is failing.
    private const val STALE_AFTER_MILLIS = 3 * 60 * 60 * 1000L
    private val mutex = Mutex()
    private var fetchedAtMillis = 0L
    var latest: CampusWeather? = null
        private set

    /** Returns a fresh reading, or the last good one when the refresh fails. */
    suspend fun load(repository: CampusWeatherRepository): CampusWeather? = mutex.withLock {
        val now = Clock.System.now().toEpochMilliseconds()
        if (latest != null && now - fetchedAtMillis < MAX_AGE_MILLIS) return@withLock latest
        fetch(repository, now)?.let {
            latest = it
            fetchedAtMillis = now
        }
        latest
    }

    private suspend fun fetch(repository: CampusWeatherRepository, now: Long): CampusWeather? = try {
        repository.current()
            ?.takeIf { it.temperature != null && now - (it.updatedAtMillis ?: 0L) < STALE_AFTER_MILLIS }
            ?.let { CampusWeather("${it.temperature!!.roundToInt()}°", it.label ?: "Hava durumu") }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}

// Approximate Antalya sunrise and sunset per month (Turkey stays on UTC+3 all year), in minutes.
private val AntalyaSunriseMinutes = intArrayOf(445, 430, 400, 365, 335, 325, 335, 355, 380, 400, 425, 445)
private val AntalyaSunsetMinutes = intArrayOf(1055, 1085, 1110, 1135, 1160, 1180, 1180, 1155, 1120, 1085, 1055, 1045)

/** True between sunset and sunrise in Antalya, when the evening campus photo is shown. */
private fun isCampusNight(): Boolean {
    val now = Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Istanbul"))
    val minutes = now.hour * 60 + now.minute
    val month = now.monthNumber - 1
    return minutes < AntalyaSunriseMinutes[month] || minutes >= AntalyaSunsetMinutes[month]
}

@Composable
internal fun CampusSummaryCards(
    diningMenuState: AkdenizDiningMenuState,
    onDailyMenuClick: (DailyMeal) -> Unit
) {
    val weatherRepository: CampusWeatherRepository = koinInject()
    val uriHandler = LocalUriHandler.current
    val cachedWeather = CampusWeatherCache.latest
    var temperature by remember { mutableStateOf(cachedWeather?.temperature ?: "—") }
    var weatherLabel by remember { mutableStateOf(cachedWeather?.label ?: "Yükleniyor…") }
    LaunchedEffect(Unit) {
        val weather = CampusWeatherCache.load(weatherRepository)
        if (weather != null) {
            temperature = weather.temperature
            weatherLabel = weather.label
        } else {
            weatherLabel = "Şu an alınamıyor"
        }
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(Modifier.weight(1f).height(168.dp), shape = RoundedCornerShape(16.dp), color = PrimaryGreen, shadowElevation = 1.dp) {
            Box {
                val night = isCampusNight()
                Image(
                    painter = painterResource(
                        if (night) Res.drawable.akdeniz_campus_weather_night else Res.drawable.akdeniz_campus_weather
                    ),
                    // The evening photo is tall; crop towards the bottom so the palm-lined road stays in view.
                    alignment = if (night) BiasAlignment(0f, 0.8f) else Alignment.Center,
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
                    // CC BY 4.0 credit required by MET Norway; tapping opens their licence page.
                    Text(
                        "Veri: MET Norway",
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://www.met.no/en/free-meteorological-data/Licensing-and-crediting")
                        }
                    )
                }
            }
        }
        DailyMenuWidget(
            state = diningMenuState,
            onMealClick = onDailyMenuClick,
            modifier = Modifier.weight(1f).height(168.dp)
        )
    }
}
