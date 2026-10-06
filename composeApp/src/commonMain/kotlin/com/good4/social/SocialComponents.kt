package com.good4.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.good4.campuscloset.ClosetAvatar
import com.good4.campuscloset.MarketNotice
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ClosetBooksAccent
import com.good4.core.presentation.ClosetSportsAccent
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.StandardButtonHeight
import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Tile accent for a kind: mint for sport, warm orange for social. */
internal fun kindAccent(kind: String): Color = if (kind == "sport") ClosetSportsAccent else Color(0xFFF2A66F)

/** White pill segmented control, the same as "Tümü / Takip ettiklerim" on Topluluklar. */
@Composable
internal fun SocialSegmented(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = SurfaceMuted) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, label ->
                val isSelected = index == selected
                Surface(
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SurfaceDefault else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary)
                    }
                }
            }
        }
    }
}

/** Poster with the day and time, then the title and one short line under it. */
@Composable
internal fun SocialPosterCard(activity: SocialActivity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val instant = activity.startsAtInstant
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SocialActivityPoster(
            type = activity.type, kind = activity.kind,
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f).clip(RoundedCornerShape(14.dp)),
            dayLabel = instant?.let { dayHeading(localDateOf(it)) },
            timeLabel = instant?.let(::clockLabel), game = activity.game
        )
        Column(Modifier.padding(horizontal = 2.dp)) {
            Text(activity.title, color = TextPrimary, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
                minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${spotsLabel(activity)} · ${typeLabel(activity.type)}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
                minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A list row for "Etkinliklerim": small poster, title, when, and one status line. */
@Composable
internal fun SocialActivityRow(
    activity: SocialActivity,
    status: String?,
    statusColor: Color,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    val instant = activity.startsAtInstant
    val muted = !activity.isUpcoming
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SocialActivityPoster(activity.type, activity.kind, Modifier.width(44.dp).height(55.dp).clip(RoundedCornerShape(9.dp)), game = activity.game)
        Column(Modifier.weight(1f)) {
            Text(activity.title, color = if (muted) TextSecondary else TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            instant?.let { Text("${dayLabel(localDateOf(it))} ${clockLabel(it)}", color = TextSecondary, fontSize = 12.sp) }
            status?.let { Text(it, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
        }
        trailing()
    }
}

/** The student's photo, or their initials until they add one. */
@Composable
internal fun SocialAvatar(name: String, photoUrl: String?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(modifier.size(size).clip(CircleShape)) {
        ClosetAvatar(name, Modifier.fillMaxSize())
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(model = photoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

private val SAFETY_RULES = listOf(
    Res.string.social_rule_public_places,
    Res.string.social_rule_personal_info,
    Res.string.social_rule_respect,
    Res.string.social_rule_report
)

/** Shown once before a verified student opens an activity or asks to join one. */
@Composable
internal fun SocialTermsContent(
    accepting: Boolean,
    error: String?,
    shownName: String,
    maskedName: String,
    onAccept: (showName: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var checked by remember { mutableStateOf(false) }
    // Names stay hidden unless the student chooses otherwise.
    var showName by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.social_rules_title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(stringResource(Res.string.social_rules_intro), fontSize = 14.sp, lineHeight = 20.sp, color = TextPrimary)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SAFETY_RULES.forEach { Text("•  ${stringResource(it)}", fontSize = 14.sp, lineHeight = 20.sp, color = TextPrimary) }
        }
        MarketNotice(stringResource(Res.string.social_rules_moderation), color = TextSecondary)
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { checked = !checked },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(Res.string.social_rules_checkbox), fontSize = 14.sp, lineHeight = 19.sp, color = TextPrimary)
        }
        SocialNameChoice(showName, shownName, maskedName, onChange = { showName = it })
        error?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
        Button(
            onClick = { onAccept(showName) }, enabled = checked && !accepting,
            modifier = Modifier.fillMaxWidth().height(StandardButtonHeight), shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (accepting) CircularProgressIndicator(Modifier.size(StandardButtonLoadingIndicatorSize), strokeWidth = 2.dp)
            else Text(stringResource(Res.string.social_kabul_ediyorum_devam_et), fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Sticky bottom bar for stand-alone screens; it keeps clear of the home indicator and rounded corners. */
@Composable
internal fun SocialBottomBar(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = SurfaceDefault, shadowElevation = 4.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/** The "Adımı göster" switch, with what other students will see for the current choice. */
@Composable
internal fun SocialNameChoice(showName: Boolean, shownName: String, maskedName: String, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceMuted, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(stringResource(Res.string.social_show_name), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(if (showName) Res.string.social_name_preview_shown else Res.string.social_name_preview_masked,
                        if (showName) shownName.ifBlank { "Ayşe Y." } else maskedName.ifBlank { "A.. Y.." }),
                    color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp
                )
            }
            Switch(checked = showName, onCheckedChange = onChange, enabled = enabled)
        }
    }
}

@Composable
internal fun SocialPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, loading: Boolean = false, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick, enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(StandardButtonHeight), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(StandardButtonLoadingIndicatorSize), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        else Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun SocialCenteredProgress() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
