package com.good4.campusevents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.community.CommunityBackHandler
import com.good4.core.presentation.components.Good4LoadingState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.good4.community.EventCategory
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.social.SocialActivityPoster
import com.good4.social.dayHeading
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** The events box: day sections of poster cards, filtered by category. No registration or following here. */
@Composable
fun CampusEventsScreen(
    ui: CampusEventsUi,
    onBack: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onOpenEvent: (CampusEvent) -> Unit,
    onRetry: () -> Unit = {}
) {
    Good4Scaffold(
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.campus_events_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back), tint = TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "subtitle", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "${stringResource(Res.string.campus_events_subtitle)} · ${stringResource(Res.string.campus_events_count, ui.events.size)}",
                    color = TextSecondary, fontSize = 13.sp
                )
            }
            if (ui.categories.size > 1) {
                item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                    CategoryFilters(ui.categories, ui.selectedCategory, onSelectCategory)
                }
            }
            when {
                ui.loading && ui.events.isEmpty() -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                    Good4LoadingState(modifier = Modifier.fillMaxWidth().padding(48.dp))
                }
                ui.error != null && ui.events.isEmpty() -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                    LoadError(ui.error.asString(), onRetry)
                }
                ui.events.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyEvents(stringResource(Res.string.campus_events_empty_title), stringResource(Res.string.campus_events_empty_body))
                }
                ui.visible.isEmpty() -> item(key = "filter-empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyEvents(stringResource(Res.string.campus_events_filter_empty), null)
                }
                else -> ui.days.forEach { (date, events) ->
                    item(key = "day-$date", span = { GridItemSpan(maxLineSpan) }) {
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
                            Text(dayHeading(date), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(stringResource(Res.string.campus_events_count, events.size), color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    items(events, key = { it.id }) { event -> CampusEventCard(event, onClick = { onOpenEvent(event) }) }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilters(categories: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "all") { FilterPill(stringResource(Res.string.campus_events_filter_all), selected == null) { onSelect(null) } }
        items(categories, key = { it }) { id -> FilterPill(EventCategory.labelFor(id), selected == id) { onSelect(id) } }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else SurfaceDefault,
        border = if (selected) null else BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) Color.White else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/** Poster on top (the event's own image, or a Sosyal-style line poster), then title, community and place. */
@Composable
internal fun CampusEventCard(event: CampusEvent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        EventPoster(event, Modifier.fillMaxWidth().aspectRatio(4f / 5f).clip(RoundedCornerShape(14.dp)))
        Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                event.title, color = TextPrimary, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                event.communityName, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (event.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(event.location, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun EventPoster(event: CampusEvent, modifier: Modifier, timeSize: Int = 22) {
    if (event.imageUrl.isBlank()) {
        val motif = posterMotifFor(event)
        SocialActivityPoster(motif.type, motif.kind, modifier, timeLabel = hhmm(event.start), timeSize = timeSize.sp)
        return
    }
    Box(modifier.background(SurfaceMuted)) {
        AsyncImage(
            model = event.imageUrl,
            contentDescription = stringResource(Res.string.campus_events_poster, event.title),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Same bottom fade as the line posters so the time reads on any photo.
        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().fillMaxHeight(0.4f)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0f), Color.Black.copy(alpha = 0.7f))))
        )
        Text(
            hhmm(event.start), color = Color.White, fontSize = timeSize.sp, fontWeight = FontWeight.SemiBold,
            lineHeight = timeSize.sp, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
        )
    }
}

@Composable
private fun EmptyEvents(title: String, body: String?) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(SurfaceMuted), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.EventAvailable, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        }
        Text(title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        body?.let { Text(it, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun LoadError(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(message, color = TextPrimary, fontSize = 15.sp, textAlign = TextAlign.Center)
        Surface(onClick = onRetry, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
            Text(
                stringResource(Res.string.campus_events_retry), color = Color.White, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}

/** Route entry: the list, or the detail of the event the student opened (back returns to the list). */
@Composable
fun CampusEventsRoute(viewModel: CampusEventsViewModel, onBack: () -> Unit) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val open = ui.openEvent
    if (open != null) {
        CommunityBackHandler(onBack = viewModel::closeEvent)
        CampusEventDetailScreen(open, onBack = viewModel::closeEvent)
    } else {
        CampusEventsScreen(
            ui = ui,
            onBack = onBack,
            onSelectCategory = viewModel::selectCategory,
            onOpenEvent = { viewModel.openEvent(it.id) },
            onRetry = viewModel::load
        )
    }
}

/** Event detail: poster band like a Sosyal activity, then who organizes it, when, where and the description. */
@Composable
fun CampusEventDetailScreen(event: CampusEvent, onBack: () -> Unit) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxSize().background(AppBackground)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (event.imageUrl.isBlank()) {
                val motif = posterMotifFor(event)
                SocialActivityPoster(
                    motif.type, motif.kind, Modifier.fillMaxWidth().height(260.dp + statusBar),
                    dayLabel = dayHeading(event.start.date), timeLabel = hhmm(event.start),
                    timeSize = 34.sp, textPadding = 18.dp, alignEnd = true, artTopInset = statusBar + 60.dp
                )
            } else {
                // Real posters are mostly portrait with text on them, so they are shown whole, not cropped.
                Box(Modifier.fillMaxWidth().background(SurfaceMuted).padding(top = statusBar + 60.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = event.imageUrl,
                        contentDescription = stringResource(Res.string.campus_events_poster, event.title),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(320.dp).clip(RoundedCornerShape(14.dp))
                    )
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(EventCategory.labelFor(event.categoryId), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(event.title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
                }
                OrganizerRow(event.communityName)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoRow(Icons.Outlined.CalendarMonth, dayHeading(event.start.date))
                    InfoRow(
                        Icons.Outlined.Schedule,
                        event.end?.let { stringResource(Res.string.campus_events_time_range, hhmm(event.start), hhmm(it)) } ?: hhmm(event.start)
                    )
                    if (event.location.isNotBlank()) InfoRow(Icons.Outlined.LocationOn, event.location)
                }
                if (event.description.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(Res.string.campus_events_about), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(event.description, color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        Surface(
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(12.dp),
            shape = CircleShape,
            color = SurfaceDefault,
            border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
            shadowElevation = 1.dp
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back), tint = TextPrimary, modifier = Modifier.padding(9.dp).size(20.dp))
        }
    }
}

@Composable
private fun OrganizerRow(name: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceMuted).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceDefault), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(Res.string.campus_events_organizer), color = TextSecondary, fontSize = 12.sp)
            Text(name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = TextPrimary, fontSize = 15.sp)
    }
}
