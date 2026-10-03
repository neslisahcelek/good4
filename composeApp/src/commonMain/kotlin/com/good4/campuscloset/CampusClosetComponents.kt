package com.good4.campuscloset

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val TURKISH_MONTHS = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")

internal fun formatMarketTime(iso: String?): String {
    val instant = iso?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return ""
    val zone = TimeZone.currentSystemDefault()
    val time = instant.toLocalDateTime(zone)
    val today = Clock.System.now().toLocalDateTime(zone).date
    val clock = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    return if (time.date == today) clock else "${time.dayOfMonth} ${TURKISH_MONTHS[time.monthNumber - 1]} $clock"
}

@Composable
internal fun ListingStatusChip(status: String, modifier: Modifier = Modifier) {
    val color = when (status) {
        "published" -> MaterialTheme.colorScheme.primary
        "pending", "reserved" -> Color(0xFFE08A1E)
        "rejected", "expired" -> ErrorRed
        else -> TextSecondary
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            statusLabel(status),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Listing photo, or the category's own colour and icon while a photo is missing or loading. */
@Composable
internal fun ListingPhoto(url: String?, modifier: Modifier = Modifier, category: String? = null, iconSize: Int = 36) {
    val style = CATEGORY_STYLES[category] ?: CATEGORY_STYLES[null]!!
    Box(modifier.background(style.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Icon(style.icon, contentDescription = null, tint = style.accent, modifier = Modifier.size(iconSize.dp))
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** "şimdi", "12 dk önce", "3 sa önce", "dün", "4 gün önce", then the date. */
internal fun formatRelativeTime(iso: String?): String {
    val instant = iso?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return ""
    val minutes = (Clock.System.now() - instant).inWholeMinutes
    return when {
        minutes < 1 -> "şimdi"
        minutes < 60 -> "$minutes dk önce"
        minutes < 24 * 60 -> "${minutes / 60} sa önce"
        minutes < 48 * 60 -> "dün"
        minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)} gün önce"
        else -> formatMarketTime(iso).substringBeforeLast(' ')
    }
}

/** Round heart used on cards and the listing detail to save a listing. */
@Composable
internal fun FavoriteButton(saved: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onToggle,
        modifier = modifier.size(34.dp),
        shape = CircleShape,
        color = SurfaceDefault.copy(alpha = 0.92f),
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (saved) "Kaydedilenlerden çıkar" else "Kaydet",
                tint = if (saved) ErrorRed else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun ListingGridCard(
    listing: MarketListing,
    showUniversity: Boolean,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) {
        Column {
            Box {
                ListingPhoto(
                    listing.photos.firstOrNull()?.thumbUrl,
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                    category = listing.category
                )
                val free = listing.price == 0
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (free) MaterialTheme.colorScheme.primary else SurfaceDefault,
                    shadowElevation = 1.dp
                ) {
                    Text(
                        formatPrice(listing.price),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (free) MaterialTheme.colorScheme.onPrimary else TextPrimary
                    )
                }
                if (listing.status != "published") {
                    ListingStatusChip(listing.status, Modifier.align(Alignment.TopStart).padding(8.dp))
                }
                if (onToggleFavorite != null) {
                    FavoriteButton(listing.isFavorite, onToggleFavorite, Modifier.align(Alignment.TopEnd).padding(6.dp))
                }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    listing.title,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    listOf(
                        conditionLabel(listing.condition),
                        formatRelativeTime(listing.publishedAt ?: listing.createdAt)
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun MarketNotice(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(text, modifier = Modifier.padding(12.dp), fontSize = 13.sp, lineHeight = 18.sp, color = TextPrimary)
    }
}

@Composable
internal fun CenteredState(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (actionLabel != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction) { Text(actionLabel, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
internal fun ReportDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, note: String) -> Unit
) {
    var reason by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MARKET_REPORT_REASONS.forEach { (id, label) ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = reason == id, role = Role.RadioButton) { reason = id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = reason == id, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = TextPrimary)
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    label = { Text("Açıklama (isteğe bağlı)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { reason?.let { onSubmit(it, note) } }, enabled = reason != null) {
                Text("Şikayet et", color = if (reason != null) ErrorRed else TextSecondary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

private val BANNED_ITEMS = listOf(
    "Sigara, tütün, nargile ve elektronik sigara ürünleri",
    "Alkollü içkiler",
    "Uyuşturucu ve uyarıcı maddeler, reçeteli ilaçlar",
    "Silah, mermi, kurusıkı ve sustalı bıçak",
    "Kaçak, bandrolsüz, çalıntı veya sahte (replika) ürünler",
    "Sahte belge, kimlik ve öğrenci kartı, sınav soruları",
    "Canlı hayvan"
)

/** Shown once before a verified student starts using Kampüs Dolabı. */
@Composable
internal fun CampusClosetTermsContent(
    accepting: Boolean,
    error: String?,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    var checked by remember { mutableStateOf(false) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Kampüs Dolabı kuralları", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(
            "Kampüs Dolabı, .edu.tr adresini doğrulayan öğrenciler arasında elden teslim ikinci el alışveriş içindir. " +
                "Good4 satışın tarafı değildir, ödeme almaz ve aracılık etmez.",
            fontSize = 14.sp, lineHeight = 20.sp, color = TextPrimary
        )
        MarketNotice(
            "Aşağıdaki ürünlerin satışı yasaktır. Bu ürünlerin satılması veya satışına aracılık edilmesi " +
                "Türk Ceza Kanunu ve ilgili kanunlar (4207, 4733 ve 6136 sayılı Kanunlar, TCK 188) kapsamında suçtur. " +
                "Bu tür ilanlar kaldırılır, hesaplar kapatılır ve gerektiğinde yetkili makamlara bildirilir.",
            color = ErrorRed
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BANNED_ITEMS.forEach { item ->
                Text("•  $item", fontSize = 14.sp, lineHeight = 20.sp, color = TextPrimary)
            }
        }
        Text(
            "• Yasak içerik içeren ilanlar ve mesajlar otomatik olarak engellenir; tekrarlayan denemeler erişimini askıya alır.\n" +
                "• İlanlar yayına alınmadan önce Good4 ekibi tarafından incelenir.\n" +
                "• Telefon numaranı yalnızca anlaştığın kişiyle mesajlarda paylaş.\n" +
                "• Şikayet edilen ilanlar ve konuşmalar Good4 ekibi tarafından incelenebilir.",
            fontSize = 13.sp, lineHeight = 19.sp, color = TextSecondary
        )
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { checked = !checked },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Kuralları okudum. Yasak ürünlerin satışının suç olduğunu biliyorum ve kabul ediyorum.",
                fontSize = 14.sp, lineHeight = 19.sp, color = TextPrimary
            )
        }
        error?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
        Button(
            onClick = onAccept,
            enabled = checked && !accepting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (accepting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Kabul ediyorum, devam et", fontWeight = FontWeight.SemiBold)
        }
    }
}
