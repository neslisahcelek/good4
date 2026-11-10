package com.good4.community.discover

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.good4.community.Community
import com.good4.community.CommunityEntryDto
import com.good4.community.CommunityFeaturedEvent
import com.good4.community.CouponDeadline
import com.good4.community.couponDeadline
import com.good4.community.couponDeadlineText
import com.good4.community.formatEventShort
import com.good4.community.relativeEventDay
import com.good4.community.shortMonthName
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.CommunityAvatarPalette
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.LightGood4Colors
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import good4.composeapp.generated.resources.*
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

internal object DiscoverDefaults {
    val Gutter = 20.dp
    val SectionGap = 28.dp
    val HeadingGap = 12.dp
    val CardGap = 12.dp
    val CardShape = RoundedCornerShape(18.dp)
    val HeroShape = RoundedCornerShape(22.dp)
    val ControlShape = RoundedCornerShape(16.dp)
}

/** Avatar ink stays dark in both themes because the avatar tones are light. */
private val AvatarInk = LightGood4Colors.textPrimary

/** A short press shrink so cards feel tappable; no ripple, matching the rest of the redesign. */
@Composable
internal fun Modifier.pressable(interaction: MutableInteractionSource, onClick: () -> Unit, role: Role = Role.Button): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(durationMillis = 120), label = "press_scale")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interaction, indication = null, role = role, onClick = onClick)
}

@Composable
internal fun DiscoverSectionHeading(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        if (trailing != null) {
            Text(trailing, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
        }
    }
}

@Composable
internal fun DiscoverSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val borderColor by animateColorAsState(if (focused) PrimaryGreen else BorderMuted, tween(160), label = "search_border")
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
        cursorBrush = SolidColor(PrimaryGreen),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(DiscoverDefaults.ControlShape)
                    .background(SurfaceDefault)
                    .border(if (focused) 1.5.dp else 1.dp, borderColor, DiscoverDefaults.ControlShape)
                    .padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = if (focused) PrimaryGreen else TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(stringResource(Res.string.community_discover_search_hint), fontSize = 15.sp, color = TextSecondary)
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    val clear = stringResource(Res.string.community_discover_search_clear)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { onQueryChange("") }
                            .semantics { contentDescription = clear },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(SurfaceMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    )
}

/** Two-option segmented control; the selected surface slides between options. */
@Composable
internal fun DiscoverTabs(followedOnly: Boolean, enabled: Boolean, onSelect: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(DiscoverDefaults.ControlShape)
            .background(SurfaceMuted)
            .padding(4.dp)
    ) {
        val half = maxWidth / 2
        val indicatorOffset by animateDpAsState(if (followedOnly) half else 0.dp, tween(220), label = "tab_indicator")
        Box(
            Modifier
                .offset(x = indicatorOffset)
                .width(half)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDefault)
                .border(1.dp, BorderMuted, RoundedCornerShape(12.dp))
        )
        Row(Modifier.fillMaxSize()) {
            listOf(
                false to stringResource(Res.string.community_discover_tab_all),
                true to stringResource(Res.string.community_discover_tab_followed)
            ).forEach { (followed, label) ->
                val selected = followedOnly == followed
                val color by animateColorAsState(if (selected) TextPrimary else TextSecondary, tween(220), label = "tab_text")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = enabled
                        ) { onSelect(followed) }
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = color)
                }
            }
        }
    }
}

/** Up to two letters from the community name, Turkish-aware ("i" -> "İ"), skipping filler words. */
internal fun communityInitials(name: String): String {
    val filler = setOf("ve", "&", "topluluğu", "toplulugu", "kulübü", "kulubu")
    val words = name.split(' ', '-', '/', '.')
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.first().isLetterOrDigit() && it.lowercase() !in filler }
    val letters = words.take(2).map { it.first() }.ifEmpty { name.trim().take(1).toList() }
    return letters.joinToString("") { c ->
        when (c) {
            'i' -> "İ"
            'ı' -> "I"
            else -> c.uppercase()
        }
    }
}

/** Same community, same tone on every launch: picked from the palette by a hash of the id. */
internal fun communityAvatarTone(id: String): Color {
    val hash = id.fold(7) { acc, c -> (acc * 31 + c.code) and 0x7fffffff }
    return CommunityAvatarPalette[hash % CommunityAvatarPalette.size]
}

@Composable
internal fun CommunityAvatar(community: Community, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.3f)
    val logo = community.data.logoUrl
    // Decorative: the card already reads the community name, so letters are not announced again.
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        if (logo.isNotBlank()) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(SurfaceMuted)
            )
        } else {
            val tone = communityAvatarTone(community.id)
            Box(Modifier.fillMaxSize().background(tone), contentAlignment = Alignment.Center) {
                // A quiet corner arc gives each tile a little depth without extra decoration.
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Color.White.copy(alpha = 0.22f), radius = this.size.minDimension * 0.55f, center = Offset(this.size.width, 0f))
                }
                Text(
                    communityInitials(community.data.name),
                    fontSize = (size.value * 0.36f).sp,
                    fontWeight = FontWeight.Bold,
                    color = AvatarInk
                )
            }
        }
    }
}

@Composable
internal fun DiscoverCommunityCard(community: Community, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val openLabel = stringResource(Res.string.community_discover_open_community, community.data.name)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .pressable(interaction, onClick)
            .clip(DiscoverDefaults.CardShape)
            .background(SurfaceDefault)
            .border(1.dp, BorderMuted, DiscoverDefaults.CardShape)
            .padding(14.dp)
            .semantics(mergeDescendants = true) { contentDescription = openLabel },
        verticalAlignment = Alignment.CenterVertically
    ) {
        CommunityAvatar(community, size = 56.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                community.data.name,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (community.data.description.isNotBlank()) {
                Text(
                    community.data.description,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
    }
}

/** The first upcoming event, large: poster if there is one, otherwise a typographic date card. */
@Composable
internal fun HighlightEventCard(event: CommunityFeaturedEvent, today: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val data = event.entry.data
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressable(interaction, onClick)
            .clip(DiscoverDefaults.HeroShape)
            .background(SurfaceDefault)
            .border(1.dp, BorderMuted, DiscoverDefaults.HeroShape)
    ) {
        Box(Modifier.fillMaxWidth().height(184.dp)) {
            if (data.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = stringResource(Res.string.community_discover_event_poster, data.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(PistachioGreen)
                )
                EventDateBadge(data, Modifier.align(Alignment.TopStart).padding(12.dp))
            } else {
                PosterlessEventArt(data, large = true, modifier = Modifier.fillMaxSize())
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            relativeEventDay(data.date, today)?.let { DiscoverPill(it) }
            Text(
                data.title,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOf(event.community.data.name, formatEventShort(data)).filter { it.isNotBlank() }.joinToString(" · "),
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun CompactEventCard(event: CommunityFeaturedEvent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val data = event.entry.data
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .width(150.dp)
            .pressable(interaction, onClick)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(16.dp))) {
            if (data.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = stringResource(Res.string.community_discover_event_poster, data.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(PistachioGreen)
                )
            } else {
                PosterlessEventArt(data, large = false, modifier = Modifier.fillMaxSize())
            }
        }
        Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                data.title,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(formatEventShort(data), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PrimaryGreen, maxLines = 1)
        }
    }
}

/** Day and month on a light badge over a poster. */
@Composable
private fun EventDateBadge(data: CommunityEntryDto, modifier: Modifier = Modifier) {
    val day = runCatching { LocalDate.parse(data.date).dayOfMonth.toString() }.getOrNull() ?: return
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDefault)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(day, fontSize = 20.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(shortMonthName(data.date), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
    }
}

/** Typographic stand-in for an event without a poster: the date carries the card, never a fake image. */
@Composable
private fun PosterlessEventArt(data: CommunityEntryDto, large: Boolean, modifier: Modifier = Modifier) {
    val day = runCatching { LocalDate.parse(data.date).dayOfMonth.toString() }.getOrNull() ?: "–"
    Box(modifier.clipToBounds().background(PistachioGreen)) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * if (large) 0.62f else 0.58f
            drawCircle(DeepGreen.copy(alpha = 0.32f), radius = r, center = Offset(size.width * 0.92f, size.height * 0.12f))
            drawCircle(PrimaryGreen.copy(alpha = 0.10f), radius = r * 0.55f, center = Offset(size.width * 0.70f, size.height * 1.02f))
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(if (large) 18.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy((-4).dp)
        ) {
            Text(day, fontSize = if (large) 64.sp else 34.sp, lineHeight = if (large) 66.sp else 36.sp, fontWeight = FontWeight.Black, color = PrimaryGreen)
            Text(
                listOf(shortMonthName(data.date), data.time).filter { it.isNotBlank() }.joinToString(" · "),
                fontSize = if (large) 15.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryGreen
            )
        }
    }
}

@Composable
internal fun DiscoverPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PistachioGreen)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = PrimaryGreen
    )
}

@Composable
internal fun DiscoverCouponCard(coupon: CommunityFeaturedEvent, today: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val data = coupon.entry.data
    val interaction = remember { MutableInteractionSource() }
    val isFreeItem = data.discountType == "freeItem"
    val amount = when (data.discountType) {
        "freeItem" -> stringResource(Res.string.community_discover_coupon_free)
        "fixed" -> stringResource(Res.string.community_coupon_stub_fixed, data.discountValue)
        else -> stringResource(Res.string.community_coupon_stub_percentage, data.discountValue)
    }
    val title = if (isFreeItem && data.freeItem.isNotBlank()) data.freeItem else data.title
    val deadline = couponDeadline(data.date, today)
    val urgent = deadline == CouponDeadline.Today || deadline == CouponDeadline.Tomorrow
    Column(
        modifier = modifier
            .width(176.dp)
            .pressable(interaction, onClick)
            .clip(DiscoverDefaults.CardShape)
            .background(SurfaceDefault)
            .border(1.dp, BorderMuted, DiscoverDefaults.CardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(amount, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, color = PrimaryGreen, maxLines = 1)
        Text(
            title,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(coupon.community.data.name, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (deadline != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = if (urgent) ErrorRed else TextSecondary, modifier = Modifier.size(14.dp))
                Text(couponDeadlineText(deadline), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (urgent) ErrorRed else TextSecondary, maxLines = 1)
            }
        }
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(Res.string.community_discover_coupon_action), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
internal fun DiscoverPrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(48.dp)
            .pressable(interaction, onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(PrimaryGreen)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
internal fun DiscoverSecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = TextPrimary) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(48.dp)
            .pressable(interaction, onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDefault)
            .border(BorderStroke(1.dp, if (tint == TextPrimary) BorderMuted else tint.copy(alpha = 0.45f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}
