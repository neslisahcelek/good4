package com.good4.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.LocalThemeController
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    // Highlight what was new when the screen opened; opening it counts as seeing everything,
    // which clears the dot on the home bell.
    var unreadIds by remember { mutableStateOf(NotificationInbox.unseenIds()) }
    LaunchedEffect(Unit) { NotificationInbox.markAllSeen() }
    val theme = LocalThemeController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PrimaryGreen,
            shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding())
                    .height(86.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = Color.White)
                }
                Text(
                    text = "Bildirimler",
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { theme.setDark(!theme.isDark) }) {
                    Icon(Icons.Outlined.DarkMode, "Gece modunu değiştir", tint = Color.White)
                }
                IconButton(onClick = { unreadIds = emptySet() }) {
                    Icon(Icons.Filled.DoneAll, "Tümünü okundu işaretle", tint = Color.White)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(studentNotifications, key = { it.id }) { notification ->
                NotificationCard(
                    title = notification.title,
                    source = notification.source,
                    date = notification.date,
                    body = notification.body,
                    unread = notification.id in unreadIds,
                    onClick = { unreadIds = unreadIds - notification.id }
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    title: String,
    source: String,
    date: String,
    body: String,
    unread: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDefault,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.25f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("$source · $date", color = TextSecondary, fontSize = 12.sp)
                }
                if (unread) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(9.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                    )
                }
            }
            Text(
                body,
                color = TextSecondary,
                lineHeight = 21.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
