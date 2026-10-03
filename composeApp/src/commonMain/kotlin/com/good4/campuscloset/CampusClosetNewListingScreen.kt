package com.good4.campuscloset

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.good4.core.presentation.*
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.ProductImagePicker
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CampusClosetNewListingScreen(
    onBack: () -> Unit,
    onOpenMyListings: () -> Unit,
    viewModel: CampusClosetNewListingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CampusClosetNewListingContent(
        state = state, onBack = onBack, onOpenMyListings = onOpenMyListings,
        onCategory = viewModel::setCategory, onCondition = viewModel::setCondition,
        onTitle = viewModel::setTitle, onDescription = viewModel::setDescription,
        onPrice = viewModel::setPrice, onFree = viewModel::setFree,
        onAddPhoto = viewModel::addPhoto, onRemovePhoto = viewModel::removePhoto,
        onError = viewModel::showError, onSubmit = viewModel::submit
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun CampusClosetNewListingContent(
    state: CampusClosetNewListingState,
    onBack: () -> Unit,
    onOpenMyListings: () -> Unit,
    onCategory: (String) -> Unit,
    onCondition: (String) -> Unit,
    onTitle: (String) -> Unit,
    onDescription: (String) -> Unit,
    onPrice: (String) -> Unit,
    onFree: (Boolean) -> Unit,
    onAddPhoto: (ByteArray) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onError: (String) -> Unit,
    onSubmit: () -> Unit
) {
    var pickingPhoto by remember { mutableStateOf(false) }
    Good4Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            Good4TopBar(title = "İlan ver", navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
            })
        },
        bottomBar = {
            if (!state.submitted) {
                Surface(color = SurfaceDefault, shadowElevation = 4.dp) {
                    Column(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("İlanın yaklaşık 30 dakika içinde incelenir.", color = TextSecondary, fontSize = 12.sp)
                        Button(
                            onClick = onSubmit, enabled = state.canSubmit,
                            modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (state.submitting) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (state.submitting) "Gönderiliyor…" else "İncelemeye gönder", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.submitted) {
            Box(Modifier.fillMaxSize().padding(padding)) { SubmittedState(onOpenMyListings, onBack) }
        } else {
            Column(
                Modifier.fillMaxSize().background(AppBackground).padding(padding).consumeWindowInsets(padding)
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Dolabında yer aç, kampüste paylaş.", color = TextSecondary, fontSize = 14.sp)
                ClosetCard {
                    ClosetSectionHeading("Fotoğraflar", Icons.Outlined.AddPhotoAlternate, "En az 1, en fazla 3 fotoğraf ekle.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(CampusClosetLimits.MAX_PHOTOS) { index ->
                            PhotoSlot(
                                bytes = state.photos.getOrNull(index), index = index,
                                enabled = !state.submitting,
                                modifier = Modifier.weight(1f),
                                onAdd = { pickingPhoto = true }, onRemove = { onRemovePhoto(index) }
                            )
                        }
                    }
                    Text("İlk fotoğraf ilanın kapak görseli olur. Ürünü farklı açılardan göster.", fontSize = 12.sp, lineHeight = 17.sp, color = TextSecondary)
                }
                ClosetCard {
                    ClosetSectionHeading("Ürün bilgisi", Icons.Outlined.Inventory2)
                    FormLabel("Kategori")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MARKET_CATEGORIES.forEach { (id, label) ->
                            val style = CATEGORY_STYLES.getValue(id)
                            val selected = state.category == id
                            Surface(
                                modifier = Modifier.selectable(selected, enabled = !state.submitting, role = Role.RadioButton) { onCategory(id) },
                                shape = RoundedCornerShape(14.dp), color = SurfaceDefault,
                                border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else BorderMuted.copy(alpha = 0.55f))
                            ) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TiltedIcon(style.icon, style.accent, size = 26, iconSize = 15)
                                    Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) MaterialTheme.colorScheme.primary else TextPrimary)
                                }
                            }
                        }
                    }
                    FormLabel("Ürünün durumu")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MARKET_CONDITIONS.forEach { (id, label) ->
                            FilterChip(
                                selected = state.condition == id, onClick = { onCondition(id) }, enabled = !state.submitting,
                                label = { Text(label, fontSize = 12.sp) }, shape = RoundedCornerShape(12.dp),
                                leadingIcon = if (state.condition == id) ({ Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }) else null,
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), selectedLabelColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.title, onValueChange = onTitle, enabled = !state.submitting,
                        label = { Text("Başlık") }, placeholder = { Text("Örn. Kışlık mont, M beden") },
                        supportingText = { Text("${state.title.length}/${CampusClosetLimits.MAX_TITLE}") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = formColors()
                    )
                    OutlinedTextField(
                        value = state.description, onValueChange = onDescription, enabled = !state.submitting,
                        label = { Text("Açıklama") }, placeholder = { Text("Durumu, bedeni ve kullanım süresi…") },
                        supportingText = { Text("${state.description.length}/${CampusClosetLimits.MAX_DESCRIPTION} · Telefon numarası yazma") },
                        minLines = 4, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = formColors()
                    )
                }
                ClosetCard {
                    ClosetSectionHeading("Fiyat", Icons.Outlined.LocalOffer)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Ücretsiz / bağış", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Bir arkadaşına hediye et.", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(checked = state.isFree, onCheckedChange = onFree, enabled = !state.submitting,
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                    }
                    if (!state.isFree) {
                        OutlinedTextField(
                            value = state.priceText, onValueChange = onPrice, enabled = !state.submitting,
                            label = { Text("Fiyat") }, suffix = { Text("₺") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = formColors()
                        )
                    } else {
                        Text("İlanında fiyat yerine “Ücretsiz” yazacak.", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
                MarketNotice(
                    "İlanlar yayınlanmadan önce incelenir. Sigara, alkol, ilaç, silah, kaçak ve sahte ürünlerin satışı yasaktır.",
                    color = TextSecondary
                )
                state.error?.let { MarketNotice(it, color = ErrorRed) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    if (pickingPhoto && state.photos.size < CampusClosetLimits.MAX_PHOTOS) {
        ModalBottomSheet(
            onDismissRequest = { pickingPhoto = false }, containerColor = AppBackground,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ClosetSectionHeading("Fotoğraf ekle", Icons.Outlined.AddPhotoAlternate, "Ürünün net göründüğü bir fotoğraf seç.")
                ProductImagePicker(
                    currentRemoteImageUrl = "", pendingImageBytes = null, isUploading = false,
                    onPendingImageChange = { bytes -> bytes?.let { onAddPhoto(it); pickingPhoto = false } },
                    onError = { onError(it); pickingPhoto = false }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun formColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = BorderMuted.copy(alpha = 0.55f),
    focusedContainerColor = SurfaceDefault, unfocusedContainerColor = SurfaceDefault
)

@Composable
private fun FormLabel(label: String) {
    Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun PhotoSlot(bytes: ByteArray?, index: Int, enabled: Boolean, modifier: Modifier, onAdd: () -> Unit, onRemove: () -> Unit) {
    Surface(
        onClick = onAdd, enabled = enabled && bytes == null,
        modifier = modifier.aspectRatio(1f), shape = RoundedCornerShape(14.dp),
        color = SurfaceMuted, border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bytes != null) {
                AsyncImage(model = bytes, contentDescription = "Fotoğraf ${index + 1}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Surface(
                    onClick = onRemove, enabled = enabled, shape = CircleShape, color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(30.dp)
                ) { Icon(Icons.Outlined.Close, "Fotoğraf ${index + 1} kaldır", tint = Color.White, modifier = Modifier.padding(6.dp)) }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.AddPhotoAlternate, "Fotoğraf ${index + 1} ekle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    Text(if (index == 0) "Kapak" else "Fotoğraf ${index + 1}", fontSize = 11.sp, color = TextSecondary)
                }
            }
            if (index == 0 && bytes != null) {
                Surface(modifier = Modifier.align(Alignment.BottomStart).padding(5.dp), shape = RoundedCornerShape(6.dp), color = SurfaceDefault) {
                    Text("Kapak", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun SubmittedState(onOpenMyListings: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        ClosetCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                TiltedIcon(Icons.Outlined.CheckCircle, PrimaryGreen, size = 64, iconSize = 32)
                Spacer(Modifier.height(24.dp))
                Text("İlanın inceleniyor", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text("Yaklaşık 30 dakika içinde yayına alınır. Durumunu İlanlarım sayfasından takip edebilirsin.", color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onOpenMyListings, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("İlanlarıma git", fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("Kampüs Dolabı'na dön", color = TextSecondary) }
            }
        }
    }
}
