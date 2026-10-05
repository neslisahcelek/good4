package com.good4.community

import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.good4.campuscloset.TiltedIcon
import com.good4.core.presentation.CommunityAccent
import com.good4.core.presentation.DraftAccent
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import kotlinx.datetime.LocalDate

/** The home "Topluluklar" shortcut colour, so the manager area keeps the same identity. */


@Composable
internal fun EventStatusChip(data: CommunityEntryDto, now: kotlinx.datetime.LocalDateTime) {
    val (label, color) = when {
        data.status == "draft" -> stringResource(Res.string.community_taslak) to DraftAccent
        data.status == "cancelled" -> stringResource(Res.string.community_iptal_edildi) to ErrorRed
        data.hasEnded(now) -> stringResource(Res.string.reservation_status_completed) to TextSecondary
        else -> stringResource(Res.string.campus_closet_yayinda) to MaterialTheme.colorScheme.primary
    }
    StatusChip(label, color)
}

@Composable
internal fun StatusChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.14f), border = BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** The event's cover, or its day and month in the same rounded tile when it has none. */
@Composable
internal fun EventDateTile(data: CommunityEntryDto, modifier: Modifier) {
    if (data.imageUrl.isNotBlank()) {
        AsyncImage(model = data.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.clip(RoundedCornerShape(14.dp)).background(PistachioGreen))
        return
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = PistachioGreen, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(runCatching { LocalDate.parse(data.date).dayOfMonth.toString() }.getOrDefault("–"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(shortMonthName(data.date), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun StickyActionBar(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = SurfaceDefault, shadowElevation = 4.dp) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/** The round outlined header button used by Kampüs Dolabı. */
@Composable
internal fun RoundIconAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.padding(end = 8.dp),
        shape = RoundedCornerShape(50),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) {
        IconButton(onClick = onClick) { Icon(icon, contentDescription = description, tint = TextPrimary) }
    }
}

@Composable
internal fun SmallToggle(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick, enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDefault,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else BorderMuted.copy(alpha = 0.55f)
        )
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else TextPrimary
        )
    }
}

@Composable
internal fun NoticeCard(text: String, color: Color, actionLabel: String, onAction: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.08f), border = BorderStroke(1.dp, color.copy(alpha = 0.3f))) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = color, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
            TextButton(onClick = onAction) { Text(actionLabel, color = color, fontSize = 13.sp) }
        }
    }
}

@Composable
internal fun InfoLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = TextPrimary)
    }
}

/** Long descriptions stay short by default so the counts and attendee list remain in view. */
@Composable
internal fun CollapsibleDescription(text: String, key: String, collapsedLines: Int = 3) {
    var expanded by rememberSaveable(key) { mutableStateOf(false) }
    var overflows by remember(text, collapsedLines) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text, fontSize = 14.sp, lineHeight = 20.sp, color = TextSecondary,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines, overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow }
        )
        if (overflows || expanded) {
            Text(
                if (expanded) stringResource(Res.string.community_daha_az_goster) else stringResource(Res.string.community_devamini_gor),
                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = 4.dp)
            )
        }
    }
}

/** The whole poster, uncropped, over a dimmed background. */
@Composable
internal fun PosterViewer(model: Any, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable(onClick = onDismiss).safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = model, contentDescription = stringResource(Res.string.community_kapak_gorseli), contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            Surface(
                onClick = onDismiss, shape = CircleShape, color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(40.dp)
            ) { Icon(Icons.Outlined.Close, stringResource(Res.string.notification_close), tint = Color.White, modifier = Modifier.padding(9.dp)) }
        }
    }
}

@Composable
internal fun StatTile(label: String, value: String, icon: ImageVector, accent: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TiltedIcon(icon, accent, size = 26, iconSize = 15)
            Column {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(label, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun FilterTile(
    label: String, count: Int, icon: ImageVector, accent: Color, selected: Boolean,
    modifier: Modifier, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDefault,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else BorderMuted.copy(alpha = 0.55f)
        ),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TiltedIcon(icon, accent, size = 28, iconSize = 16)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(Res.string.community_label_count, label, count),
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun RegistrationProgress(registrations: Int, capacity: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                if (capacity > 0) stringResource(Res.string.community_registration_progress, registrations, capacity) else stringResource(Res.string.community_registration_unlimited, registrations),
                fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f)
            )
            if (capacity > 0) Text(stringResource(Res.string.community_percentage, (registrations * 100 / capacity).coerceAtMost(100)), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        if (capacity > 0) {
            LinearProgressIndicator(
                progress = { (registrations.toFloat() / capacity).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = SurfaceMuted,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}
