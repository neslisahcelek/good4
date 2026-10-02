package com.good4.dining.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.dining.domain.DailyMeal
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val IstanbulZone = TimeZone.of("Europe/Istanbul")

/** What one meal slot shows: title, place, and the items served (empty when not published). */
private data class MealContent(
    val meal: DailyMeal,
    val title: String,
    val place: String,
    val icon: ImageVector,
    val items: List<String>,
    val emptyText: String,
    val calories: Int? = null
)

private fun AkdenizDiningMenuState.meals(): List<MealContent> {
    val weekend = runCatching { LocalDate.parse(loadedDate).dayOfWeek }.getOrNull()
        ?.let { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY } ?: false
    val notPublished = if (isLoading) "Yükleniyor…" else "Bugün için yayınlanmadı"
    // When today's KYK list is missing, the next published day is shown and labelled, never passed off as today.
    val kykPlace = kykDayLabel?.let { "KYK · $it" } ?: "KYK"
    return listOf(
        MealContent(
            DailyMeal.KYK_BREAKFAST, "Kahvaltı", kykPlace, Icons.Outlined.BakeryDining,
            kykDay?.breakfast.orEmpty(), notPublished
        ),
        MealContent(
            DailyMeal.CAFETERIA, "Öğle ve Akşam", "Merkezi Yemekhane", Icons.Outlined.Restaurant,
            cafeteriaToday?.meals.orEmpty(),
            if (weekend && !isLoading) "Yemekhane hafta sonu kapalı" else notPublished,
            cafeteriaToday?.calories
        ),
        MealContent(
            DailyMeal.KYK_DINNER, "Akşam Yemeği", kykPlace, Icons.Outlined.DinnerDining,
            kykDay?.dinner.orEmpty(), notPublished
        )
    )
}

/** Morning shows breakfast, midday the cafeteria, afternoon onwards the KYK dinner. */
private fun mealForNow(): DailyMeal {
    val hour = Clock.System.now().toLocalDateTime(IstanbulZone).hour
    return when {
        hour < 10 -> DailyMeal.KYK_BREAKFAST
        hour < 15 -> DailyMeal.CAFETERIA
        else -> DailyMeal.KYK_DINNER
    }
}

/** Home screen widget: a vertical stack of the three meals, like an iOS Smart Stack. */
@Composable
fun DailyMenuWidget(
    state: AkdenizDiningMenuState,
    onMealClick: (DailyMeal) -> Unit,
    modifier: Modifier = Modifier
) {
    val meals = state.meals()
    val pagerState = rememberPagerState(initialPage = mealForNow().ordinal) { meals.size }
    // The tap lives on the surface, not inside the pager: on iOS a clickable within the pager page
    // lost the first tap to the pager's scroll handling.
    Surface(
        onClick = { onMealClick(meals[pagerState.currentPage].meal) },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp
    ) {
        Box(Modifier.fillMaxSize()) {
            VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val content = meals[page]
                Box(Modifier.fillMaxSize()) {
                    Icon(
                        content.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = .13f),
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 12.dp, y = 12.dp).size(76.dp)
                    )
                    Column(
                        Modifier.padding(start = 12.dp, top = 12.dp, end = 20.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(content.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(content.title, fontSize = 14.sp, lineHeight = 17.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                        Text(content.place, fontSize = 10.5.sp, lineHeight = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        if (content.items.isEmpty()) {
                            Text(content.emptyText, fontSize = 11.sp, lineHeight = 14.sp, color = TextSecondary, maxLines = 2)
                        } else {
                            // The whole menu at a glance: one line per item, long alternatives are shortened.
                            content.items.forEach { item ->
                                Text(item, fontSize = 10.5.sp, lineHeight = 13.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            // Page dots on the side, as in the iOS widget stack.
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 7.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(meals.size) { index ->
                    Box(
                        Modifier
                            .size(5.dp)
                            .background(
                                if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else TextSecondary.copy(alpha = .3f),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

/** Full page with only today's menus; reloads by itself when the day changes. */
@Composable
fun DailyMenuScreen(
    state: AkdenizDiningMenuState,
    initialMeal: DailyMeal,
    onRefreshIfDayChanged: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val meals = state.meals()
    // The date header is item 0 and meal N is item N + 1; breakfast keeps the header in view.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (initialMeal == DailyMeal.KYK_BREAKFAST) 0 else initialMeal.ordinal + 1
    )
    com.good4.review.ReviewFeaturePrompt(
        feature = com.good4.review.ReviewFeature.DINING_MENU,
        contentReady = !state.isLoading && (state.cafeteriaToday != null || state.kykDay != null),
        isScrolling = listState.isScrollInProgress
    )
    LifecycleResumeEffect(Unit) {
        onRefreshIfDayChanged()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.loadedDate) {
        // Wake up just after midnight while the page stays open.
        val now = Clock.System.now().toLocalDateTime(IstanbulZone)
        val secondsLeft = (24 * 3600) - (now.hour * 3600 + now.minute * 60 + now.second) + 5
        delay(secondsLeft * 1000L)
        onRefreshIfDayChanged()
    }

    Good4NestedScaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = "Günün Menüsü",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    state.loadedDate.toTurkishLongDate(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            meals.forEach { content ->
                item(key = content.meal.name) {
                    MealSection(
                        content = content,
                        onBalanceClick = if (content.meal == DailyMeal.CAFETERIA) {
                            { uriHandler.openUri(AKDENIZ_BALANCE_URL) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun MealSection(content: MealContent, onBalanceClick: (() -> Unit)?) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = SurfaceDefault, shadowElevation = 1.dp) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = PistachioGreen) {
                    Icon(content.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp).size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(content.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(content.place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (content.items.isEmpty()) {
                if (content.emptyText == "Yükleniyor…") {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                } else {
                    Text(content.emptyText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            } else {
                content.items.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.padding(vertical = 3.dp))
                }
                content.calories?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("Toplam $it kcal", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
            }
            if (onBalanceClick != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onBalanceClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(19.dp))
                    Text("Yemekhane bakiyesi yükle", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

private val TurkishMonths = listOf(
    "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
)
private val TurkishDays = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

private fun String.toTurkishLongDate(): String = runCatching {
    val date = LocalDate.parse(this)
    "${date.dayOfMonth} ${TurkishMonths[date.monthNumber - 1]} ${date.year}, ${TurkishDays[date.dayOfWeek.ordinal]}"
}.getOrDefault("")
