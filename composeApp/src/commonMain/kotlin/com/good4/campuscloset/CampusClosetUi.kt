package com.good4.campuscloset

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
