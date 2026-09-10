package com.good4.student.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudentMenuSheet(onDismiss: () -> Unit) {
    var numbersOpen by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceCanvasWarm,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.72f).padding(horizontal = 20.dp)) {
            if (numbersOpen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { numbersOpen = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menüye dön", tint = PrimaryGreen)
                    }
                    Text("Numaralar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(20.dp))
                listOf(
                    Triple("Kampüs Güvenlik İhbar Hattı", "0242 310 22 22", "+902423102222"),
                    Triple("SKS / Mediko-Sosyal", "0242 310 21 51", "+902423102151")
                ).forEach { (title, number, dialNumber) ->
                    Surface(
                        onClick = { uriHandler.openUri("tel:$dialNumber") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        shape = RoundedCornerShape(20.dp), color = SurfaceDefault
                    ) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(number, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                            Icon(Icons.Outlined.Phone, contentDescription = "Arama ekranını aç", tint = PrimaryGreen)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items((0 until 12).toList()) { index ->
                        val isNumbers = index == 5
                        Surface(
                            onClick = { if (isNumbers) numbersOpen = true },
                            enabled = isNumbers,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = SurfaceDefault
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isNumbers) "Numaralar" else "Yeni Alan",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Icon(
                                    if (isNumbers) Icons.Outlined.Phone else Icons.Outlined.GridView,
                                    contentDescription = null,
                                    tint = PrimaryGreen.copy(alpha = if (isNumbers) 1f else 0.3f),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
