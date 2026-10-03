package com.good4.campuscloset

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.*

internal val ClosetOfferAccent = Color(0xFFE08A1E)

@Composable
internal fun ClosetCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun ClosetSectionHeading(title: String, icon: ImageVector, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TiltedIcon(icon, PrimaryGreen, size = 32, iconSize = 18)
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp) }
        }
    }
}

@Composable
internal fun ClosetAvatar(name: String, modifier: Modifier = Modifier) {
    Surface(modifier.size(46.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2)
                    .joinToString("") { it.take(1).uppercase() }.ifBlank { "Ö" },
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp
            )
        }
    }
}

@Composable
internal fun ClosetEmptyState(icon: ImageVector, title: String, description: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TiltedIcon(icon, PrimaryGreen, size = 60, iconSize = 30)
        Spacer(Modifier.height(20.dp))
        Text(title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(description, color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun RemoveListingDialog(listingTitle: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("İlan yayından kaldırılsın mı?") },
        text = {
            Text("\"$listingTitle\" Kampüs Dolabı’ndan kaldırılır ve fotoğrafları silinir. Bu işlem geri alınamaz. Yeniden paylaşmak için yeni ilan oluşturman gerekir.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Yayından kaldır", color = ErrorRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

/** Only the price of a posted listing can change; 0 means free. */
@Composable
internal fun PriceEditDialog(currentPrice: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var free by remember { mutableStateOf(currentPrice == 0) }
    var text by remember { mutableStateOf(if (currentPrice > 0) currentPrice.toString() else "") }
    val price = if (free) 0 else text.toIntOrNull()
    val valid = price != null && price in 0..CampusClosetLimits.MAX_PRICE && (free || price > 0) && price != currentPrice
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fiyatı düzenle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Yalnızca fiyat değişir; ilan yeniden incelemeye gitmez.", fontSize = 13.sp, color = TextSecondary)
                OutlinedTextField(
                    value = text,
                    onValueChange = { value -> text = value.filter(Char::isDigit).take(6) },
                    enabled = !free,
                    label = { Text("Yeni fiyat (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ücretsiz / bağış", modifier = Modifier.weight(1f))
                    Switch(checked = free, onCheckedChange = { free = it })
                }
            }
        },
        confirmButton = { TextButton(onClick = { price?.let(onSave) }, enabled = valid) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}
