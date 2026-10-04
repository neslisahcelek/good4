package com.good4.campuscloset

import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val dismissKeyboard = {
        focusManager.clearFocus()
        keyboardController?.hide()
        Unit
    }
    Good4Scaffold(
        modifier = Modifier.pointerInput(focusManager, keyboardController) {
            detectTapGestures(onTap = { dismissKeyboard() })
        }.imePadding(),
        topBar = {
            Good4TopBar(title = stringResource(Res.string.campus_closet_create_listing), navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.campus_closet_back)) }
            }, actions = {
                if (keyboardVisible) {
                    TextButton(onClick = dismissKeyboard) { Text(stringResource(Res.string.campus_closet_bitti)) }
                }
            })
        },
        bottomBar = {
            if (!state.submitted) {
                Surface(color = SurfaceDefault, shadowElevation = 4.dp) {
                    Column(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            state.validationMessage?.asString() ?: stringResource(Res.string.campus_closet_ilanin_yaklasik_30_dakika_icinde_incelenir),
                            color = TextSecondary, fontSize = 12.sp
                        )
                        Button(
                            onClick = { dismissKeyboard(); onSubmit() }, enabled = state.canSubmit,
                            modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (state.submitting) {
                                CircularProgressIndicator(Modifier.size(StandardButtonLoadingIndicatorSize), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (state.submitting) stringResource(Res.string.campus_closet_gonderiliyor) else stringResource(Res.string.campus_closet_incelemeye_gonder), fontWeight = FontWeight.SemiBold)
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
                Text(stringResource(Res.string.campus_closet_dolabinda_yer_ac_kampuste_paylas), color = TextSecondary, fontSize = 14.sp)
                ClosetCard {
                    ClosetSectionHeading(stringResource(Res.string.campus_closet_fotograflar), Icons.Outlined.AddPhotoAlternate, stringResource(Res.string.campus_closet_en_az_1_en_fazla_3_fotograf_ekle))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(CampusClosetLimits.MAX_PHOTOS) { index ->
                            PhotoSlot(
                                bytes = state.photos.getOrNull(index), index = index,
                                enabled = !state.submitting,
                                modifier = Modifier.weight(1f),
                                onAdd = { dismissKeyboard(); pickingPhoto = true }, onRemove = { onRemovePhoto(index) }
                            )
                        }
                    }
                    Text(stringResource(Res.string.campus_closet_ilk_fotograf_ilanin_kapak_gorseli_olur_urunu_farkli_acilardan), fontSize = 12.sp, lineHeight = 17.sp, color = TextSecondary)
                }
                ClosetCard {
                    ClosetSectionHeading(stringResource(Res.string.campus_closet_urun_bilgisi), Icons.Outlined.Inventory2)
                    FormLabel(stringResource(Res.string.campus_closet_kategori))
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
                                    Text(stringResource(label), fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) MaterialTheme.colorScheme.primary else TextPrimary)
                                }
                            }
                        }
                    }
                    FormLabel(stringResource(Res.string.campus_closet_urunun_durumu))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MARKET_CONDITIONS.forEach { (id, label) ->
                            FilterChip(
                                selected = state.condition == id, onClick = { onCondition(id) }, enabled = !state.submitting,
                                label = { Text(stringResource(label), fontSize = 12.sp) }, shape = RoundedCornerShape(12.dp),
                                leadingIcon = if (state.condition == id) ({ Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }) else null,
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), selectedLabelColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.title, onValueChange = onTitle, enabled = !state.submitting,
                        label = { Text(stringResource(Res.string.campus_closet_baslik)) }, placeholder = { Text(stringResource(Res.string.campus_closet_orn_kislik_mont_m_beden)) },
                        supportingText = { Text(stringResource(Res.string.campus_closet_en_az_3_karakter, state.title.trim().length, CampusClosetLimits.MAX_TITLE)) },
                        isError = state.title.isNotEmpty() && state.title.trim().length < 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = formColors()
                    )
                    OutlinedTextField(
                        value = state.description, onValueChange = onDescription, enabled = !state.submitting,
                        label = { Text(stringResource(Res.string.campus_closet_aciklama)) }, placeholder = { Text(stringResource(Res.string.campus_closet_durumu_bedeni_ve_kullanim_suresi)) },
                        supportingText = { Text(stringResource(Res.string.campus_closet_en_az_10_karakter_telefon_numarasi_yazma, state.description.trim().length, CampusClosetLimits.MAX_DESCRIPTION)) },
                        isError = state.description.isNotEmpty() && state.description.trim().length < 10,
                        minLines = 4, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = formColors()
                    )
                }
                ClosetCard {
                    ClosetSectionHeading(stringResource(Res.string.campus_closet_fiyat), Icons.Outlined.LocalOffer)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(Res.string.campus_closet_ucretsiz_bagis), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(Res.string.campus_closet_bir_arkadasina_hediye_et), color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(checked = state.isFree, onCheckedChange = onFree, enabled = !state.submitting,
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                    }
                    if (!state.isFree) {
                        OutlinedTextField(
                            value = state.priceText, onValueChange = onPrice, enabled = !state.submitting,
                            label = { Text(stringResource(Res.string.campus_closet_fiyat)) }, suffix = { Text(stringResource(Res.string.campus_closet_currency)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = formColors()
                        )
                    } else {
                        Text(stringResource(Res.string.campus_closet_ilaninda_fiyat_yerine_ucretsiz_yazacak), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
                MarketNotice(
                    stringResource(Res.string.campus_closet_ilanlar_yayinlanmadan_once_incelenir_sigara_alkol_ilac_silah_kacak),
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
                ClosetSectionHeading(stringResource(Res.string.campus_closet_fotograf_ekle), Icons.Outlined.AddPhotoAlternate, stringResource(Res.string.campus_closet_urunun_net_gorundugu_bir_fotograf_sec))
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
                AsyncImage(model = bytes, contentDescription = stringResource(Res.string.campus_closet_fotograf, index + 1), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Surface(
                    onClick = onRemove, enabled = enabled, shape = CircleShape, color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(30.dp)
                ) { Icon(Icons.Outlined.Close, stringResource(Res.string.campus_closet_fotograf_kaldir, index + 1), tint = Color.White, modifier = Modifier.padding(6.dp)) }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.AddPhotoAlternate, stringResource(Res.string.campus_closet_fotograf_ekle_2, index + 1), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    Text(if (index == 0) stringResource(Res.string.campus_closet_kapak) else stringResource(Res.string.campus_closet_fotograf, index + 1), fontSize = 11.sp, color = TextSecondary)
                }
            }
            if (index == 0 && bytes != null) {
                Surface(modifier = Modifier.align(Alignment.BottomStart).padding(5.dp), shape = RoundedCornerShape(6.dp), color = SurfaceDefault) {
                    Text(stringResource(Res.string.campus_closet_kapak), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
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
                Text(stringResource(Res.string.campus_closet_ilanin_inceleniyor), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(Res.string.campus_closet_yaklasik_30_dakika_icinde_yayina_alinir_durumunu_ilanlarim_sayfasindan), color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onOpenMyListings, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text(stringResource(Res.string.campus_closet_ilanlarima_git), fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text(stringResource(Res.string.campus_closet_kampus_dolabi_na_don), color = TextSecondary) }
            }
        }
    }
}
