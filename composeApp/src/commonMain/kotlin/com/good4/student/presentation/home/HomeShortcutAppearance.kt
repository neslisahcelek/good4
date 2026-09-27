package com.good4.student.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.good4.core.presentation.PrimaryGreen
import com.good4.dining.presentation.AKDENIZ_BALANCE_URL
import com.good4.student.home.HomeShortcut

data class HomeShortcutAppearance(val title: String, val icon: ImageVector, val accent: Color)

@Composable
fun HomeShortcut.appearance(communityManager: Boolean = false): HomeShortcutAppearance = when (this) {
    HomeShortcut.COMMUNITIES -> HomeShortcutAppearance(if (communityManager) "Topluluğu Yönet" else "Topluluklar", Icons.Outlined.Groups, Color(0xFF75D9BE))
    HomeShortcut.CLASS_SCHEDULE -> HomeShortcutAppearance("Ders Programı", Icons.Outlined.MenuBook, Color(0xFF4B9FD1))
    HomeShortcut.CAMPUS_MAP -> HomeShortcutAppearance("Kampüs Haritası", Icons.Outlined.Map, PrimaryGreen)
    HomeShortcut.ACADEMIC_CALENDAR -> HomeShortcutAppearance("Akademik Takvim", Icons.Outlined.CalendarMonth, Color(0xFFA58DEB))
    HomeShortcut.SUSPENDED_MEALS -> HomeShortcutAppearance("Askıda Yemek", Icons.Outlined.ShoppingCart, Color(0xFF8CB7ED))
    HomeShortcut.TOP_UP -> HomeShortcutAppearance("TL Yükle", Icons.Outlined.AccountBalanceWallet, Color(0xFFF2A66F))
    HomeShortcut.TENNIS -> HomeShortcutAppearance("Tenis Kortu Rezervasyonu", Icons.Outlined.SportsTennis, Color(0xFFF2A66F))
    HomeShortcut.PHONE_NUMBERS -> HomeShortcutAppearance("Numaralar", Icons.Outlined.Phone, Color(0xFF68CCDC))
    HomeShortcut.FEEDBACK -> HomeShortcutAppearance("Geri Bildirim", Icons.Outlined.Feedback, PrimaryGreen)
}

internal val HomeShortcut.externalUrl: String? get() = when (this) {
    HomeShortcut.TOP_UP -> AKDENIZ_BALANCE_URL
    HomeShortcut.TENNIS -> "https://sporalanlari.akdeniz.edu.tr/Takvim/Haftalik/3"
    else -> null
}
