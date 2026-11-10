package com.good4.community.discover

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ClosetOfferAccent
import com.good4.core.presentation.CommunityAccent
import com.good4.core.presentation.CommunityAvatarPalette
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** State A: inline note inside the events section, about 90 dp tall. */
@Composable
internal fun EventsEmptyNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(DiscoverDefaults.CardShape)
            .background(SurfaceDefault)
            .border(1.dp, BorderMuted, DiscoverDefaults.CardShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(PistachioGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.community_discover_events_empty_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(stringResource(Res.string.community_discover_events_empty_body), fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary)
        }
    }
}

/** State B: quieter than the events note (no card edge, round coupon mark) so the two never look alike. */
@Composable
internal fun CouponsEmptyNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(DiscoverDefaults.CardShape)
            .background(SurfaceMuted)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(ClosetOfferAccent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = ClosetOfferAccent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(stringResource(Res.string.community_discover_coupons_empty_title), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(stringResource(Res.string.community_discover_coupons_empty_body), fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
        }
    }
}

/** State C: the main section is empty, so this one is a bit larger, but still frameless. */
@Composable
internal fun CommunitiesEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GroupMark(Modifier.size(width = 112.dp, height = 56.dp))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(Res.string.community_discover_communities_empty_title), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, textAlign = TextAlign.Center)
        Text(stringResource(Res.string.community_discover_communities_empty_body), fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

/** State D: the followed tab is empty; the action goes back to "Tümü". */
@Composable
internal fun FollowedEmptyState(onDiscover: () -> Unit, modifier: Modifier = Modifier) {
    CenteredState(
        art = { GroupMark(Modifier.size(width = 128.dp, height = 64.dp), highlightFirst = true) },
        title = stringResource(Res.string.community_discover_followed_empty_title),
        body = stringResource(Res.string.community_discover_followed_empty_body),
        modifier = modifier
    ) {
        DiscoverPrimaryButton(stringResource(Res.string.community_discover_followed_empty_action), onDiscover)
    }
}

/** State E: search found nothing; offers clearing the query. */
@Composable
internal fun SearchEmptyState(query: String, onClear: () -> Unit, modifier: Modifier = Modifier) {
    CenteredState(
        art = { IconMark(Icons.Outlined.SearchOff, tint = TextSecondary, background = SurfaceMuted) },
        title = stringResource(Res.string.community_discover_search_empty_title),
        body = stringResource(Res.string.community_discover_search_empty_body, query.trim()),
        modifier = modifier
    ) {
        DiscoverSecondaryButton(stringResource(Res.string.community_discover_search_clear), onClear)
    }
}

/** State F: nothing anywhere; one calm, positive screen instead of three stacked empty blocks. */
@Composable
internal fun AllEmptyState(modifier: Modifier = Modifier) {
    CenteredState(
        art = { CampusMark(Modifier.size(width = 168.dp, height = 112.dp)) },
        title = stringResource(Res.string.community_discover_all_empty_title),
        body = stringResource(Res.string.community_discover_all_empty_body),
        titleSize = 22,
        modifier = modifier.padding(top = 24.dp)
    )
}

/** State H: load failed. Brick red marks the problem without painting the screen. */
@Composable
internal fun LoadErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CenteredState(
        art = { IconMark(Icons.Outlined.ErrorOutline, tint = ErrorRed, background = ErrorRed.copy(alpha = 0.10f)) },
        title = stringResource(Res.string.community_discover_error_title),
        body = stringResource(Res.string.community_discover_error_body),
        modifier = modifier
    ) {
        DiscoverSecondaryButton(stringResource(Res.string.community_discover_error_retry), onRetry, tint = ErrorRed)
    }
}

@Composable
private fun CenteredState(
    art: @Composable () -> Unit,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    titleSize: Int = 18,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        art()
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = titleSize.sp, lineHeight = (titleSize + 5).sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
        Text(body, fontSize = 14.sp, lineHeight = 20.sp, color = TextSecondary, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(10.dp))
            action()
        }
    }
}

@Composable
private fun IconMark(icon: ImageVector, tint: Color, background: Color) {
    Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
    }
}

/** Three overlapping community tiles in palette tones: "a group of communities". */
@Composable
private fun GroupMark(modifier: Modifier = Modifier, highlightFirst: Boolean = false) {
    val tones = listOf(CommunityAvatarPalette[0], CommunityAvatarPalette[5], CommunityAvatarPalette[1])
    val surface = SurfaceDefault
    Canvas(modifier) {
        val tile = size.height
        val step = (size.width - tile) / 2f
        tones.forEachIndexed { index, tone ->
            val left = step * index
            val alpha = if (highlightFirst && index > 0) 0.45f else 1f
            drawRoundRect(surface, Offset(left - 3f, -3f), Size(tile + 6f, tile + 6f), CornerRadius(tile * 0.32f))
            drawRoundRect(tone.copy(alpha = alpha), Offset(left, 0f), Size(tile, tile), CornerRadius(tile * 0.3f))
            drawCircle(Color.White.copy(alpha = 0.22f * alpha), radius = tile * 0.5f, center = Offset(left + tile, 0f))
        }
    }
}

/** A small original campus scene in Good4 greens: rounded blocks and a sun, nothing literal. */
@Composable
private fun CampusMark(modifier: Modifier = Modifier) {
    val ground = PistachioGreen
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(ground, Offset(0f, h * 0.72f), Size(w, h * 0.28f), CornerRadius(h * 0.14f))
        drawCircle(DeepGreen.copy(alpha = 0.55f), radius = h * 0.17f, center = Offset(w * 0.80f, h * 0.22f))
        val blocks = listOf(
            Triple(0.10f, 0.44f, CommunityAccent),
            Triple(0.32f, 0.22f, PrimaryGreen),
            Triple(0.56f, 0.36f, DeepGreen)
        )
        blocks.forEach { (x, top, tone) ->
            drawRoundRect(tone, Offset(w * x, h * top), Size(w * 0.2f, h * (0.80f - top)), CornerRadius(w * 0.05f))
            // A short column of window rows keeps the shapes readable as buildings, not faces.
            var rowTop = top + 0.10f
            while (rowTop < 0.66f) {
                drawRoundRect(
                    Color.White.copy(alpha = 0.55f),
                    Offset(w * (x + 0.045f), h * rowTop),
                    Size(w * 0.11f, h * 0.045f),
                    CornerRadius(w * 0.01f)
                )
                rowTop += 0.12f
            }
        }
    }
}

/** State G: skeletons shaped like the real cards, with one slow, low-contrast pulse. */
@Composable
internal fun DiscoverSkeleton(modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.community_discover_loading)
    val transition = rememberInfiniteTransition(label = "discover_skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "discover_skeleton_alpha"
    )
    val block = SurfaceMuted
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = pulse }
            .semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.width(190.dp).height(22.dp).clip(RoundedCornerShape(8.dp)).background(block))
        Box(Modifier.fillMaxWidth().height(248.dp).clip(DiscoverDefaults.HeroShape).background(block))
        Spacer(Modifier.height(DiscoverDefaults.SectionGap - 12.dp))
        Box(Modifier.width(170.dp).height(22.dp).clip(RoundedCornerShape(8.dp)).background(block))
        repeat(3) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(DiscoverDefaults.CardShape)
                    .border(1.dp, BorderMuted, DiscoverDefaults.CardShape)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(17.dp)).background(block))
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.62f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(block))
                    Box(Modifier.fillMaxWidth(0.86f).height(11.dp).clip(RoundedCornerShape(6.dp)).background(block))
                }
            }
        }
    }
}
