package com.good4.student.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.PrimaryGreen
import com.good4.dining.presentation.AKDENIZ_BALANCE_URL
import com.good4.student.home.HomeShortcut
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.social_title
import org.jetbrains.compose.resources.stringResource

data class HomeShortcutAppearance(val title: String, val icon: ImageVector, val accent: Color, val tag: String? = null)

@Composable
fun HomeShortcut.appearance(communityManager: Boolean = false): HomeShortcutAppearance = when (this) {
    HomeShortcut.COMMUNITIES -> HomeShortcutAppearance(if (communityManager) "Topluluğu Yönet" else "Topluluklar", Icons.Outlined.Groups, Color(0xFF75D9BE))
    HomeShortcut.CLASS_SCHEDULE -> HomeShortcutAppearance("Ders Programı", Icons.AutoMirrored.Outlined.MenuBook, Color(0xFF4B9FD1))
    HomeShortcut.CAMPUS_MAP -> HomeShortcutAppearance("Kampüs Haritası", Icons.Outlined.Map, PrimaryGreen)
    HomeShortcut.ACADEMIC_CALENDAR -> HomeShortcutAppearance("Akademik Takvim", Icons.Outlined.CalendarMonth, Color(0xFFA58DEB))
    HomeShortcut.SUSPENDED_MEALS -> HomeShortcutAppearance("Askıda Yemek", Icons.Outlined.ShoppingCart, Color(0xFF8CB7ED))
    HomeShortcut.CAMPUS_CLOSET -> HomeShortcutAppearance("Kampüs Dolabı", Icons.Outlined.Checkroom, Color(0xFFE59AC0), tag = "2. el alışveriş")
    HomeShortcut.SOCIAL_ACTIVITIES -> HomeShortcutAppearance(stringResource(Res.string.social_title), SocialMeetupIcon, Color(0xFFF2A66F))
    HomeShortcut.TOP_UP -> HomeShortcutAppearance("TL Yükle", Icons.Outlined.AccountBalanceWallet, Color(0xFFF2A66F))
    HomeShortcut.TENNIS -> HomeShortcutAppearance("Tenis Kortu Rezervasyonu", Icons.Outlined.SportsTennis, Color(0xFFF2A66F))
    HomeShortcut.PHONE_NUMBERS -> HomeShortcutAppearance("Numaralar", Icons.Outlined.Phone, Color(0xFF68CCDC))
    HomeShortcut.FEEDBACK -> HomeShortcutAppearance("Geri Bildirim", Icons.Outlined.Feedback, PrimaryGreen)
}

private val SocialMeetupIcon = ImageVector.Builder(
    name = "SocialMeetup", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f,
).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        // A conversation bubble above two people keeps the shortcut distinct from sports.
        moveTo(12f, 2f)
        lineTo(20f, 2f)
        curveTo(21.1f, 2f, 22f, 2.9f, 22f, 4f)
        lineTo(22f, 7f)
        curveTo(22f, 8.1f, 21.1f, 9f, 20f, 9f)
        lineTo(16f, 9f)
        lineTo(13f, 11f)
        lineTo(13f, 9f)
        lineTo(12f, 9f)
        curveTo(10.9f, 9f, 10f, 8.1f, 10f, 7f)
        lineTo(10f, 4f)
        curveTo(10f, 2.9f, 10.9f, 2f, 12f, 2f)
        close()
        moveTo(3.5f, 13f)
        arcToRelative(2.5f, 2.5f, 0f, true, false, 5f, 0f)
        arcToRelative(2.5f, 2.5f, 0f, true, false, -5f, 0f)
        moveTo(13.5f, 14f)
        arcToRelative(2.5f, 2.5f, 0f, true, false, 5f, 0f)
        arcToRelative(2.5f, 2.5f, 0f, true, false, -5f, 0f)
        moveTo(1.5f, 22f)
        lineTo(1.5f, 20.5f)
        curveTo(1.5f, 16.5f, 10.5f, 16.5f, 10.5f, 20.5f)
        lineTo(10.5f, 22f)
        moveTo(12f, 22f)
        lineTo(12f, 21f)
        curveTo(12f, 17.5f, 20f, 17.5f, 20f, 21f)
        lineTo(20f, 22f)
    }
}.build()

internal val HomeShortcut.externalUrl: String? get() = when (this) {
    HomeShortcut.TOP_UP -> AKDENIZ_BALANCE_URL
    HomeShortcut.TENNIS -> "https://sporalanlari.akdeniz.edu.tr/Takvim/Haftalik/3"
    else -> null
}
