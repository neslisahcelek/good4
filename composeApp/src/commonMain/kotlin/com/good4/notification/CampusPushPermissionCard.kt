package com.good4.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary

@Composable
fun CampusPushPermissionCard() {
    val device by CampusPushNotifications.device.collectAsState()
    if (device.permission == "granted" || device.permission == "unknown" || device.permission == "unavailable") return
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault, border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Yeni mesaj ve teklifleri kaçırma.", fontSize = 14.sp, color = TextPrimary)
            TextButton(onClick = CampusPushNotifications::requestPermission) {
                Text("Bildirimleri aç", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
