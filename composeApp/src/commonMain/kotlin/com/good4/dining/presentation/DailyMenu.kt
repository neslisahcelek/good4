package com.good4.dining.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import com.good4.core.presentation.components.StandardButtonHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.good4.campuscloset.ClosetCard
import com.good4.campuscloset.TiltedIcon
import com.good4.community.StatusChip
import com.good4.community.shortMonthName
import com.good4.core.presentation.ClosetAccessoriesAccent
import com.good4.core.presentation.ClosetBooksAccent
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.dining.domain.DailyMeal
import com.good4.community.NoticeCard
import com.good4.core.presentation.ErrorRed
import com.good4.dining.domain.MEAL_RATING_MIN_VOTES
import com.good4.dining.domain.MealRating
import com.good4.dining.domain.MealVote
import com.good4.dining.domain.ratingOpensAt
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    val calories: Int? = null,
    /** False when the items belong to another day (the KYK fallback), so they cannot be rated today. */
    val isToday: Boolean = true
)

@Composable
private fun AkdenizDiningMenuState.meals(): List<MealContent> {
    val weekend = runCatching { LocalDate.parse(loadedDate).dayOfWeek }.getOrNull()
        ?.let { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY } ?: false
    val notPublished = if (isLoading) stringResource(Res.string.loading) else stringResource(Res.string.daily_menu_not_published)
    // When today's KYK list is missing, the next published day is shown and labelled, never passed off as today.
    val kykPlace = kykDayLabel?.let { "KYK · $it" } ?: "KYK"
    return listOf(
        MealContent(
            DailyMeal.KYK_BREAKFAST, stringResource(Res.string.daily_menu_breakfast), kykPlace, Icons.Outlined.BakeryDining,
            kykDay?.breakfast.orEmpty(), notPublished, isToday = kykDayLabel == null
        ),
        MealContent(
            DailyMeal.CAFETERIA, stringResource(Res.string.daily_menu_cafeteria), stringResource(Res.string.campus_closet_merkezi_yemekhane), Icons.Outlined.Restaurant,
            cafeteriaToday?.meals.orEmpty(),
            if (weekend && !isLoading) stringResource(Res.string.daily_menu_weekend_closed) else notPublished,
            cafeteriaToday?.calories
        ),
        MealContent(
            DailyMeal.KYK_DINNER, stringResource(Res.string.daily_menu_dinner), kykPlace, Icons.Outlined.DinnerDining,
            kykDay?.dinner.orEmpty(), notPublished, isToday = kykDayLabel == null
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
    onRate: (DailyMeal, MealVote) -> Unit,
    onDismissRatingError: () -> Unit,
    onRetryRatings: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(Clock.System.now().toLocalDateTime(IstanbulZone)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = Clock.System.now().toLocalDateTime(IstanbulZone)
        }
    }
    val hour = now.hour
    val uriHandler = LocalUriHandler.current
    val meals = state.meals()
    val current = when {
        hour < 10 -> DailyMeal.KYK_BREAKFAST
        hour < 15 -> DailyMeal.CAFETERIA
        else -> DailyMeal.KYK_DINNER
    }
    // The date header is item 0 and meal N is item N + 1; breakfast keeps the date in view.
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
                title = stringResource(Res.string.product_list_section_today_menu),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { DayHeader(state.loadedDate) }
            state.ratingError?.let { message ->
                item(key = "rating-error") {
                    NoticeCard(message.asString(), ErrorRed,
                        if (state.ratingLoadFailed) stringResource(Res.string.campus_closet_retry) else stringResource(Res.string.daily_menu_dismiss),
                        if (state.ratingLoadFailed) onRetryRatings else onDismissRatingError)
                }
            }
            meals.forEach { content ->
                item(key = content.meal.name) {
                    MealSection(
                        content = content,
                        isNow = content.meal == current && content.items.isNotEmpty(),
                        isLoading = state.isLoading,
                        rating = state.ratings[content.meal],
                        ratingOpen = !state.isLoading && state.loadedDate == now.date.toString() && content.isToday && content.meal.ratingOpensAt(hour),
                        ratingInFlight = content.meal in state.ratingInFlight,
                        onRate = { vote -> onRate(content.meal, vote) },
                        onBalanceClick = if (content.meal == DailyMeal.CAFETERIA) {
                            { uriHandler.openUri(AKDENIZ_BALANCE_URL) }
                        } else null
                    )
                }
            }
        }
    }
}

/** Each meal keeps one accent everywhere it appears, like the closet categories. */
private val DailyMeal.accent: Color
    get() = when (this) {
        DailyMeal.KYK_BREAKFAST -> ClosetAccessoriesAccent
        DailyMeal.CAFETERIA -> PrimaryGreen
        DailyMeal.KYK_DINNER -> ClosetBooksAccent
    }

/** Today's date as a page header: the date tile beside the weekday, no card around it. */
@Composable
private fun DayHeader(date: String) {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    Row(
        Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.size(width = 52.dp, height = 58.dp),
            shape = RoundedCornerShape(14.dp),
            color = PistachioGreen,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(parsed?.dayOfMonth?.toString() ?: "–", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(shortMonthName(date), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            }
        }
        Column {
            Text(parsed?.let { TurkishDays[it.dayOfWeek.ordinal] }.orEmpty(), color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(Res.string.daily_menu_campus_today), color = TextSecondary, fontSize = 13.sp)
        }
    }
}

/** Section heading in the closet style, then one card with the dishes or a short note. */
@Composable
private fun MealSection(
    content: MealContent,
    isNow: Boolean,
    isLoading: Boolean,
    rating: MealRating?,
    ratingOpen: Boolean,
    ratingInFlight: Boolean,
    onRate: (MealVote) -> Unit,
    onBalanceClick: (() -> Unit)?
) {
    val accent = if (content.items.isEmpty()) TextSecondary else content.meal.accent
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TiltedIcon(content.icon, accent, size = 32, iconSize = 18)
            Column(Modifier.weight(1f)) {
                Text(content.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(content.place, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
            if (isNow) StatusChip(stringResource(Res.string.daily_menu_now), MaterialTheme.colorScheme.primary)
        }
        ClosetCard(
            modifier = if (isNow) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp)) else Modifier
        ) {
            when {
                content.items.isNotEmpty() -> {
                    Column {
                        content.items.forEachIndexed { index, item ->
                            if (index > 0) HorizontalDivider(Modifier.padding(start = 18.dp), color = BorderMuted.copy(alpha = .45f))
                            Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).background(accent, CircleShape))
                                Spacer(Modifier.width(12.dp))
                                Text(item, color = TextPrimary, fontSize = 15.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                    content.calories?.let { StatusChip(stringResource(Res.string.daily_menu_calories, it), TextSecondary) }
                    if (content.isToday) {
                        HorizontalDivider(color = BorderMuted.copy(alpha = .45f))
                        MealRatingRow(rating, ratingOpen, ratingInFlight, onRate)
                    }
                }
                isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                else -> Text(content.emptyText, color = TextSecondary, fontSize = 14.sp)
            }
            if (onBalanceClick != null) {
                OutlinedButton(
                    onClick = onBalanceClick,
                    modifier = Modifier.fillMaxWidth().height(StandardButtonHeight),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(Res.string.daily_menu_balance), modifier = Modifier.padding(start = 8.dp, end = 4.dp))
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/** "Nasıldı? 😋 😐 😕" — one tap, changeable the same day; results appear after voting. */
@Composable
private fun MealRatingRow(rating: MealRating?, open: Boolean, inFlight: Boolean, onRate: (MealVote) -> Unit) {
    val myVote = rating?.myVote
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (open) stringResource(Res.string.meal_rating_question) else stringResource(Res.string.meal_rating_opens_later),
                color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (open) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MealVote.entries.forEach { vote ->
                        val selected = vote == myVote
                        val label = vote.label
                        Surface(
                            onClick = { onRate(vote) },
                            enabled = !inFlight,
                            shape = CircleShape,
                            color = if (selected) PistachioGreen else SurfaceMuted,
                            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.size(44.dp).semantics { contentDescription = label }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(vote.emoji, fontSize = if (selected) 24.sp else 21.sp)
                            }
                        }
                    }
                }
            }
        }
        if (myVote != null && !inFlight) {
            val percentages = rating.percentages()
            Text(
                if (percentages == null) stringResource(Res.string.meal_rating_thanks, MEAL_RATING_MIN_VOTES, rating.total)
                else MealVote.entries.joinToString("  ·  ") { "${it.emoji} %${percentages.getValue(it)}" } + stringResource(Res.string.meal_rating_vote_count, rating.total),
                color = TextSecondary, fontSize = 12.sp
            )
        }
    }
}

private val MealVote.label: String
    @Composable get() = when (this) {
        MealVote.GOOD -> stringResource(Res.string.meal_rating_good)
        MealVote.OKAY -> stringResource(Res.string.meal_rating_okay)
        MealVote.BAD -> stringResource(Res.string.meal_rating_bad)
    }

private val TurkishMonths = listOf(
    "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
)
private val TurkishDays = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

private fun String.toTurkishLongDate(): String = runCatching {
    val date = LocalDate.parse(this)
    "${date.dayOfMonth} ${TurkishMonths[date.monthNumber - 1]}, ${TurkishDays[date.dayOfWeek.ordinal]}"
}.getOrDefault("")


@Preview
@Composable
private fun DailyMenuScreenPreview() {
    DailyMenuScreen(
        state = AkdenizDiningMenuState(loadedDate = todayInIstanbul(), isLoading = false),
        initialMeal = DailyMeal.KYK_BREAKFAST,
        onRefreshIfDayChanged = {},
        onRate = { _, _ -> },
        onDismissRatingError = {},
        onRetryRatings = {},
        onBackClick = {}
    )
}
