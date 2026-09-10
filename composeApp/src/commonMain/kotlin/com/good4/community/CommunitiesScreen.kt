package com.good4.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.good4.core.presentation.*
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.ProductImagePicker
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CommunitiesScreen(onBack: () -> Unit, viewModel: CommunityViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var managing by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<CommunityEntryDto?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var profileEditor by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<CommunityEntry?>(null) }
    val community = state.selected
    Good4NestedScaffold(topBar = {
        Good4TopBar(title = community?.data?.name ?: "Topluluklar", navigationIcon = {
            IconButton(onClick = { if (community == null) onBack() else { viewModel.back(); managing = false } }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
            }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (community == null) {
                item { Text("Kampüste kendine bir yer bul.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Topluluk ara") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                val filtered = state.communities.filter { it.data.name.contains(query, ignoreCase = true) || it.data.description.contains(query, ignoreCase = true) }
                items(filtered, key = { it.id }) { item ->
                    Surface(onClick = { tab = 0; viewModel.select(item) }, shape = RoundedCornerShape(20.dp), color = SurfaceDefault) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CommunityLogo(item.data.logoUrl)
                            Column { Text(item.data.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(item.data.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 2) }
                        }
                    }
                }
                if (!state.loading && state.error == null && filtered.isEmpty()) item { EmptyCommunityContent(if (query.isBlank()) "Topluluklar yakında burada" else "Aramanıza uygun topluluk bulunamadı", "Topluluklar eklendiğinde burada keşfedebilirsiniz.") }
            } else {
                item { Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) { CommunityLogo(community.data.logoUrl); Text(community.data.description, color = TextSecondary) } }
                if (state.canManage) {
                    item { OutlinedButton(onClick = { managing = !managing }, modifier = Modifier.fillMaxWidth()) { Text(if (managing) "Yönetimi kapat" else "Topluluğunu Yönet") } }
                    if (managing) item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { editingId = null; editor = CommunityEntryDto() }, modifier = Modifier.fillMaxWidth()) { Text("Etkinlik ekle") }
                            OutlinedButton(onClick = { editingId = null; editor = CommunityEntryDto(kind = "coupon", status = "pending") }, modifier = Modifier.fillMaxWidth()) { Text("Kupon ekle") }
                            TextButton(onClick = { profileEditor = true }, modifier = Modifier.fillMaxWidth()) { Text("Topluluk bilgilerini düzenle") }
                        }
                    }
                }
                item { TabRow(selectedTabIndex = tab, containerColor = SurfaceCanvasWarm, contentColor = PrimaryGreen) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Etkinlikler") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Topluluğa Özel Kuponlar") })
                } }
                val entries = state.entries.filter { it.data.kind == if (tab == 0) "event" else "coupon" }
                items(entries, key = { it.id }) { entry ->
                    Surface(onClick = { detail = entry }, shape = RoundedCornerShape(20.dp), color = SurfaceDefault) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (entry.data.imageUrl.isNotBlank()) AsyncImage(entry.data.imageUrl, null, Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                            Text(entry.data.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${entry.data.date} ${entry.data.time}", color = PrimaryGreen)
                            Text(entry.data.location, color = TextSecondary)
                            if (entry.data.status != "published") Text(if (entry.data.status == "pending") "Onay bekliyor" else "İptal edildi", color = TextSecondary)
                        }
                    }
                }
                if (!state.loading && state.error == null && entries.isEmpty()) item { EmptyCommunityContent(if (tab == 0) "Henüz etkinlik yok" else "Henüz kupon yok", if (tab == 0) "Yeni etkinlikler burada görünecek." else "Topluluğun fırsatları burada yer alacak.") }
            }
            if (state.loading) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryGreen) } }
            state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); TextButton(onClick = { if (community == null) viewModel.load() else viewModel.select(community) }) { Text("Tekrar dene") } } }
        }
    }
    if (editor != null && community != null) {
        EntryEditor(initial = editor!!, saving = state.saving, error = state.error, onDismiss = { if (!state.saving) { editor = null; viewModel.clearError() } }, onSave = { draft, image -> viewModel.save(editingId, draft, image) { editor = null } })
    }
    if (profileEditor && community != null) {
        CommunityProfileEditor(community.data, state.saving, state.error, { if (!state.saving) { profileEditor = false; viewModel.clearError() } }) { data, image -> viewModel.updateProfile(data, image) { profileEditor = false } }
    }
    detail?.let { entry ->
        AlertDialog(onDismissRequest = { detail = null }, title = { Text(entry.data.title) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${entry.data.date} ${entry.data.time}"); Text(entry.data.location); Text(entry.data.description)
                if (entry.data.kind == "coupon" && entry.data.code.isNotBlank()) Text("Kupon kodu: ${entry.data.code}", fontWeight = FontWeight.Bold)
                if (state.canManage) {
                    TextButton(onClick = { editingId = entry.id; editor = entry.data; detail = null }) { Text("Düzenle") }
                    TextButton(onClick = { viewModel.cancel(entry.id) { detail = null } }, enabled = !state.saving && entry.data.status != "cancelled") { Text("Yayından kaldır") }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }, confirmButton = { TextButton(onClick = { detail = null }) { Text("Kapat") } })
    }
}

@Composable
private fun CommunityLogo(url: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = PistachioGreen, modifier = Modifier.size(60.dp)) {
        if (url.isNotBlank()) AsyncImage(url, null, contentScale = ContentScale.Crop)
        else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Groups, null, tint = PrimaryGreen, modifier = Modifier.size(30.dp)) }
    }
}

@Composable
private fun EmptyCommunityContent(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Groups, null, tint = PrimaryGreen, modifier = Modifier.size(42.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

internal fun validateCommunityEntry(entry: CommunityEntryDto): String? {
    if (entry.title.isBlank() || entry.description.isBlank() || entry.location.isBlank()) return "Başlık, açıklama ve ${if (entry.kind == "coupon") "işletme" else "konum"} alanlarını doldurun."
    if (runCatching { LocalDate.parse(entry.date) }.isFailure) return "Tarihi yıl-ay-gün biçiminde girin. Örnek: 2026-10-15"
    if (entry.kind == "event" && !Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(entry.time)) return "Saati 14:30 biçiminde girin."
    if (entry.kind == "coupon" && entry.code.isBlank()) return "Kupon kodunu girin."
    if (entry.title.length > 120 || entry.description.length > 4000 || entry.location.length > 200 || entry.code.length > 80) return "Bazı alanlar çok uzun. Lütfen kısaltın."
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryEditor(initial: CommunityEntryDto, saving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (CommunityEntryDto, ByteArray?) -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    var image by remember { mutableStateOf<ByteArray?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf(false) }
    var datePicker by remember { mutableStateOf(false) }
    var timePicker by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState()
    val timeState = rememberTimePickerState(initialHour = initial.time.substringBefore(':').toIntOrNull() ?: 14, initialMinute = initial.time.substringAfter(':').toIntOrNull() ?: 0, is24Hour = true)
    val coupon = draft.kind == "coupon"
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = SurfaceCanvasWarm) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (preview) "Önizleme" else if (coupon) "Kupon ekle" else "Etkinlik ekle", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (!preview) {
                ProductImagePicker(currentRemoteImageUrl = draft.imageUrl, pendingImageBytes = image, isUploading = saving, onPendingImageChange = { image = it }, onError = { localError = it })
                EditorField("Başlık", draft.title, saving) { draft = draft.copy(title = it) }
                OutlinedButton(onClick = { datePicker = true }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text((if (coupon) "Son kullanım tarihi: " else "Tarih: ") + draft.date.ifBlank { "Seç" }) }
                if (!coupon) OutlinedButton(onClick = { timePicker = true }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Saat: " + draft.time.ifBlank { "Seç" }) }
                EditorField(if (coupon) "İşletme" else "Konum", draft.location, saving) { draft = draft.copy(location = it) }
                EditorField(if (coupon) "Avantaj ve kullanım şartları" else "Açıklama", draft.description, saving, singleLine = false) { draft = draft.copy(description = it) }
                if (coupon) EditorField("Kupon kodu", draft.code, saving) { draft = draft.copy(code = it) }
            } else {
                Text(draft.title, style = MaterialTheme.typography.titleLarge)
                Text("${draft.date} ${draft.time}"); Text(draft.location); Text(draft.description)
                if (coupon) Text("Kupon kodu: ${draft.code}")
                TextButton(onClick = { preview = false }, enabled = !saving) { Text("Düzenlemeye dön") }
            }
            if (coupon) Text("Kuponunuz Good4 onayından sonra görünür olacak.", color = TextSecondary)
            (localError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                localError = validateCommunityEntry(draft)
                if (localError == null) { if (preview) onSave(draft.copy(title = draft.title.trim(), description = draft.description.trim(), location = draft.location.trim()), image) else preview = true }
            }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Kaydediliyor…" else if (!preview) "Önizle" else if (coupon) "Onaya gönder" else "Yayınla") }
        }
    }
    if (datePicker) DatePickerDialog(onDismissRequest = { datePicker = false }, confirmButton = {
        TextButton(enabled = pickerState.selectedDateMillis != null, onClick = {
            pickerState.selectedDateMillis?.let { draft = draft.copy(date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString()) }
            datePicker = false
        }) { Text("Seç") }
    }, dismissButton = { TextButton(onClick = { datePicker = false }) { Text("Vazgeç") } }) { DatePicker(state = pickerState) }
    if (timePicker) AlertDialog(onDismissRequest = { timePicker = false }, title = { Text("Saat seç") }, text = { TimeInput(timeState) }, confirmButton = {
        TextButton(onClick = { draft = draft.copy(time = "${timeState.hour.toString().padStart(2, '0')}:${timeState.minute.toString().padStart(2, '0')}"); timePicker = false }) { Text("Seç") }
    }, dismissButton = { TextButton(onClick = { timePicker = false }) { Text("Vazgeç") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityProfileEditor(initial: CommunityDto, saving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (CommunityDto, ByteArray?) -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    var image by remember { mutableStateOf<ByteArray?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Topluluk bilgileri", style = MaterialTheme.typography.headlineSmall)
            ProductImagePicker(currentRemoteImageUrl = draft.logoUrl, pendingImageBytes = image, isUploading = saving, onPendingImageChange = { image = it }, onError = { localError = it })
            EditorField("Topluluk adı", draft.name, saving) { draft = draft.copy(name = it) }
            EditorField("Kısa açıklama", draft.description, saving, false) { draft = draft.copy(description = it) }
            (localError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { onSave(draft.copy(name = draft.name.trim(), description = draft.description.trim()), image) }, enabled = !saving && draft.name.isNotBlank() && draft.name.length <= 120 && draft.description.length <= 1000, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Kaydediliyor…" else "Kaydet") }
        }
    }
}

@Composable
private fun EditorField(label: String, value: String, saving: Boolean, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, enabled = !saving, singleLine = singleLine, shape = RoundedCornerShape(14.dp))
}
