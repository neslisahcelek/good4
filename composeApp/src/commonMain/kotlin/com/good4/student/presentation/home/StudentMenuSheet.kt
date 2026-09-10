package com.good4.student.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceCanvasWarm
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary

private data class StudentMenuItem(
    val title: String,
    val icon: ImageVector,
    val accent: Color,
    val isEnabled: Boolean = false,
    val opensNumbers: Boolean = false,
    val url: String? = null
)

private const val TENNIS_COURT_RESERVATION_URL =
    "https://sporalanlari.akdeniz.edu.tr/Takvim/Haftalik/3"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudentMenuSheet(onDismiss: () -> Unit) {
    var numbersOpen by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val menuItems = listOf(
        StudentMenuItem(
            title = "Tenis Kortu Rezervasyonu",
            icon = Icons.Outlined.SportsTennis,
            accent = Color(0xFFF2A66F),
            isEnabled = true,
            url = TENNIS_COURT_RESERVATION_URL
        ),
        StudentMenuItem("Yeni Alan", Icons.Outlined.GridView, Color(0xFF8CB7ED)),
        StudentMenuItem("Yeni Alan", Icons.Outlined.GridView, Color(0xFF75D9BE)),
        StudentMenuItem("Yeni Alan", Icons.Outlined.GridView, Color(0xFFB997EB)),
        StudentMenuItem("Yeni Alan", Icons.Outlined.GridView, Color(0xFFAAA4F2)),
        StudentMenuItem(
            title = "Numaralar",
            icon = Icons.Outlined.Phone,
            accent = Color(0xFF68CCDC),
            isEnabled = true,
            opensNumbers = true
        ),
        StudentMenuItem("Yeni Alan", Icons.Outlined.GridView, Color(0xFFF2A66F)),
        StudentMenuItem("Yeni Alan", Icons.Outlined.GridView, Color(0xFF8CB7ED)),
        StudentMenuItem("Yeni Alan", Icons.Outlined.GridView, Color(0xFF75D9BE)),
        StudentMenuItem("Yeni Alan", Icons.Outlined.GridView, Color(0xFFB997EB)),
        StudentMenuItem("Yeni Alan", Icons.Outlined.GridView, Color(0xFFAAA4F2)),
        StudentMenuItem("Yeni Alan", Icons.Outlined.GridView, Color(0xFF68CCDC))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceCanvasWarm,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .padding(top = 10.dp)
        ) {
            if (numbersOpen) {
                MenuSheetHeader(
                    title = "Numaralar",
                    subtitle = "Dokunarak doğrudan arayabilirsin",
                    onBack = { numbersOpen = false }
                )
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PhoneContactCard(
                        title = "Kampüs Güvenlik İhbar Hattı",
                        number = "0242 310 22 22",
                        accent = Color(0xFFF2A66F),
                        onClick = { uriHandler.openUri("tel:+902423102222") }
                    )
                    PhoneContactCard(
                        title = "SKS / Mediko-Sosyal",
                        number = "0242 310 21 51",
                        accent = Color(0xFF68CCDC),
                        onClick = { uriHandler.openUri("tel:+902423102151") }
                    )
                }
            } else {
                MenuSheetHeader(
                    title = "Menü",
                    subtitle = "Kampüs araçları ve hızlı bağlantılar",
                    onClose = onDismiss
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 28.dp)
                ) {
                    items(menuItems) { item ->
                        val onItemClick: (() -> Unit)? = when {
                            item.opensNumbers -> ({ numbersOpen = true })
                            item.url != null -> {
                                val url = item.url
                                ({ uriHandler.openUri(url) })
                            }
                            else -> null
                        }
                        StudentMenuCard(
                            item = item,
                            onClick = onItemClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuSheetHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Menüye dön",
                    tint = TextPrimary
                )
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary
            )
        }
        if (onClose != null) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Menüyü kapat", tint = TextSecondary)
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
private fun StudentMenuCard(
    item: StudentMenuItem,
    onClick: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.18f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
        ) {
            Text(
                text = item.title,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp, end = 54.dp),
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.accent.copy(alpha = if (item.isEnabled) 0.9f else 0.66f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
                    .size(62.dp)
            )
        }
    }
}

@Composable
private fun PhoneContactCard(
    title: String,
    number: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(15.dp),
                color = accent.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Phone, contentDescription = null, tint = PrimaryGreen)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Text(number, fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
            }
            Text("Ara", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PrimaryGreen)
        }
    }
}
