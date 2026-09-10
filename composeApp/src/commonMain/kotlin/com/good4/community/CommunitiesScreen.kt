package com.good4.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Good4NestedScaffold(
        topBar = {
            Good4TopBar(
                title = community?.data?.name ?: "Topluluklar",
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (community == null) {
                                onBack()
                            } else {
                                viewModel.back()
                                managing = false
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (community == null) {
                item { CommunityListIntro() }
                item {
                    CommunitySearchField(
                        query = query,
                        onQueryChange = { query = it }
                    )
                }
                item {
                    Text(
                        text = "Keşfet",
                        fontSize = 19.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                val filtered = state.communities.filter {
                    it.data.name.contains(query, ignoreCase = true) ||
                        it.data.description.contains(query, ignoreCase = true)
                }
                items(filtered, key = { it.id }) { item ->
                    CommunityListCard(
                        community = item,
                        onClick = {
                            tab = 0
                            viewModel.select(item)
                        }
                    )
                }
                if (!state.loading && state.error == null && filtered.isEmpty()) {
                    item {
                        EmptyCommunityContent(
                            title = if (query.isBlank()) "Topluluklar yakında burada" else "Aramana uygun topluluk bulunamadı",
                            subtitle = if (query.isBlank()) "Yeni topluluklar eklendikçe burada keşfedebilirsin." else "Farklı bir kelimeyle tekrar deneyebilirsin."
                        )
                    }
                }
            } else {
                item {
                    CommunityDetailHeader(
                        community = community,
                        canManage = state.canManage,
                        managing = managing,
                        onManageClick = { managing = !managing }
                    )
                }
                if (state.canManage) {
                    if (managing) {
                        item {
                            CommunityManagementActions(
                                onAddEvent = {
                                    editingId = null
                                    editor = CommunityEntryDto()
                                },
                                onAddCoupon = {
                                    editingId = null
                                    editor = CommunityEntryDto(kind = "coupon", status = "pending")
                                },
                                onEditProfile = { profileEditor = true }
                            )
                        }
                    }
                }
                item {
                    CommunityTabs(
                        selectedTab = tab,
                        onTabSelected = { tab = it }
                    )
                }
                val entries = state.entries.filter { it.data.kind == if (tab == 0) "event" else "coupon" }
                items(entries, key = { it.id }) { entry ->
                    CommunityEntryCard(
                        entry = entry,
                        onClick = { detail = entry }
                    )
                }
                if (!state.loading && state.error == null && entries.isEmpty()) {
                    item {
                        EmptyCommunityContent(
                            title = if (tab == 0) "Henüz etkinlik yok" else "Henüz kupon yok",
                            subtitle = if (tab == 0) "Yeni etkinlikler burada görünecek." else "Topluluğun fırsatları burada yer alacak.",
                            coupon = tab == 1
                        )
                    }
                }
            }
            if (state.loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryGreen)
                    }
                }
            }
            state.error?.let { error ->
                item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { if (community == null) viewModel.load() else viewModel.select(community) }) {
                        Text("Tekrar dene")
                    }
                }
            }
        }
    }
    if (editor != null && community != null) {
        EntryEditor(initial = editor!!, saving = state.saving, error = state.error, onDismiss = { if (!state.saving) { editor = null; viewModel.clearError() } }, onSave = { draft, image -> viewModel.save(editingId, draft, image) { editor = null } })
    }
    if (profileEditor && community != null) {
        CommunityProfileEditor(community.data, state.saving, state.error, { if (!state.saving) { profileEditor = false; viewModel.clearError() } }) { data, image -> viewModel.updateProfile(data, image) { profileEditor = false } }
    }
    detail?.let { entry ->
        CommunityEntryDetailDialog(
            entry = entry,
            canManage = state.canManage,
            saving = state.saving,
            error = state.error,
            onDismiss = { detail = null },
            onEdit = {
                editingId = entry.id
                editor = entry.data
                detail = null
            },
            onUnpublish = {
                viewModel.cancel(entry.id) { detail = null }
            }
        )
    }
}

@Composable
private fun CommunityListIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Kampüste kendine bir yer bul.",
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            text = "Etkinlikleri keşfet, fırsatları yakala ve topluluğuna katıl.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun CommunitySearchField(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.18f))
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Topluluk ara", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDefault,
                unfocusedContainerColor = SurfaceDefault,
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )
    }
}

@Composable
private fun CommunityListCard(community: Community, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CommunityLogo(community.data.logoUrl, size = 62.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = community.data.name,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = community.data.description,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextSecondary,
                    maxLines = 2
                )
                Text(
                    text = "Topluluğu keşfet",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryGreen
                )
            }
        }
    }
}

@Composable
private fun CommunityDetailHeader(
    community: Community,
    canManage: Boolean,
    managing: Boolean,
    onManageClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CommunityLogo(community.data.logoUrl, size = 64.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = community.data.name,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = community.data.description,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary
                    )
                }
            }
            if (canManage) {
                OutlinedButton(
                    onClick = onManageClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
                ) {
                    Icon(Icons.Outlined.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (managing) "Yönetimi kapat" else "Topluluğunu yönet", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CommunityManagementActions(
    onAddEvent: () -> Unit,
    onAddCoupon: () -> Unit,
    onEditProfile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PistachioGreen.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddEvent,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = SurfaceDefault
                    )
                ) {
                    Text("Etkinlik ekle")
                }
                OutlinedButton(
                    onClick = onAddCoupon,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.35f))
                ) {
                    Text("Kupon ekle")
                }
            }
            TextButton(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryGreen)
            ) {
                Text("Topluluk bilgilerini düzenle")
            }
        }
    }
}

@Composable
private fun CommunityTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceMuted
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Etkinlikler", "Kuponlar").forEachIndexed { index, label ->
                Surface(
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = if (selectedTab == index) SurfaceDefault else androidx.compose.ui.graphics.Color.Transparent,
                    shadowElevation = if (selectedTab == index) 1.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selectedTab == index) PrimaryGreen else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityEntryCard(entry: CommunityEntry, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.18f))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (entry.data.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = entry.data.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(13.dp),
                        color = PistachioGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (entry.data.kind == "coupon") Icons.Outlined.LocalOffer else Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.data.title, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("${entry.data.date} ${entry.data.time}".trim(), fontSize = 13.sp, lineHeight = 18.sp, color = PrimaryGreen)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(entry.data.location, fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary)
                }
                if (entry.data.status != "published") {
                    Text(
                        text = if (entry.data.status == "pending") "Onay bekliyor" else "İptal edildi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityEntryDetailDialog(
    entry: CommunityEntry,
    canManage: Boolean,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onUnpublish: () -> Unit
) {
    val isCoupon = entry.data.kind == "coupon"
    val dateTime = listOf(entry.data.date, entry.data.time)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDefault,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = PistachioGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isCoupon) Icons.Outlined.LocalOffer else Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                    Text(
                        text = entry.data.title,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 7.dp),
                        fontSize = 22.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Kapat",
                            tint = TextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                CommunityDetailInfoRow(
                    icon = Icons.Outlined.CalendarMonth,
                    text = dateTime
                )
                CommunityDetailInfoRow(
                    icon = if (isCoupon) Icons.Outlined.Storefront else Icons.Outlined.LocationOn,
                    text = entry.data.location
                )

                Text(
                    text = entry.data.description,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = TextSecondary
                )

                if (isCoupon && entry.data.code.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = PistachioGreen.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            PrimaryGreen.copy(alpha = 0.14f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Outlined.LocalOffer,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(21.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Kupon kodu",
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = entry.data.code,
                                    fontSize = 17.sp,
                                    lineHeight = 21.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }
                    }
                }

                if (canManage) {
                    HorizontalDivider(color = BorderMuted.copy(alpha = 0.32f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onEdit,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen,
                                contentColor = SurfaceDefault
                            )
                        ) {
                            Text("Düzenle", fontWeight = FontWeight.Medium)
                        }
                        OutlinedButton(
                            onClick = onUnpublish,
                            enabled = !saving && entry.data.status != "cancelled",
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(13.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                ErrorRed.copy(alpha = 0.45f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ErrorRed,
                                disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Yayından kaldır",
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    error?.let {
                        Text(
                            text = it,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = ErrorRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityDetailInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun CommunityLogo(url: String, size: androidx.compose.ui.unit.Dp = 60.dp) {
    Surface(shape = RoundedCornerShape(16.dp), color = PistachioGreen, modifier = Modifier.size(size)) {
        if (url.isNotBlank()) AsyncImage(url, null, contentScale = ContentScale.Crop)
        else Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Groups, null, tint = PrimaryGreen, modifier = Modifier.size(size * 0.5f)) }
    }
}

@Composable
private fun EmptyCommunityContent(title: String, subtitle: String, coupon: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp, horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (coupon) Icons.Outlined.LocalOffer else Icons.Outlined.Groups,
            contentDescription = null,
            tint = PrimaryGreen.copy(alpha = 0.8f),
            modifier = Modifier.size(40.dp)
        )
        Text(title, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(subtitle, fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary, textAlign = TextAlign.Center)
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
