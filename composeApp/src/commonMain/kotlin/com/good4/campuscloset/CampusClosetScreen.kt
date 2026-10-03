package com.good4.campuscloset

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import org.koin.compose.viewmodel.koinViewModel

/** Category icon and accent, taken from the home shortcut palette so the market feels part of Good4. */
internal data class CategoryStyle(val label: String, val icon: ImageVector, val accent: Color)

internal val CATEGORY_STYLES: Map<String?, CategoryStyle> = mapOf(
    null to CategoryStyle("Tümü", Icons.Outlined.GridView, PrimaryGreen),
    "clothing" to CategoryStyle("Kıyafet", Icons.Outlined.Checkroom, Color(0xFFE59AC0)),
    "accessories" to CategoryStyle("Aksesuar", Icons.Outlined.ShoppingBag, Color(0xFFF2A66F)),
    "electronics" to CategoryStyle("Elektronik", Icons.Outlined.Devices, Color(0xFF4B9FD1)),
    "sports" to CategoryStyle("Spor", Icons.Outlined.SportsSoccer, Color(0xFF75D9BE)),
    "books" to CategoryStyle("Kitap", Icons.Outlined.MenuBook, Color(0xFFA58DEB)),
    "dorm" to CategoryStyle("Yurt & Ev", Icons.Outlined.Bed, Color(0xFF8CB7ED)),
    "hobby" to CategoryStyle("Hobi", Icons.Outlined.MusicNote, Color(0xFF68CCDC)),
    "other" to CategoryStyle("Diğer", Icons.Outlined.Category, Color(0xFFB8B08D))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusClosetScreen(
    onBack: () -> Unit,
    onOpenListing: (String) -> Unit,
    onNewListing: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenMyListings: () -> Unit,
    viewModel: CampusClosetFeedViewModel = koinViewModel(),
    eduViewModel: CampusEmailVerificationViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val eduState by eduViewModel.state.collectAsStateWithLifecycle()
    val me = state.me
    var showVerification by rememberSaveable { mutableStateOf(false) }

    // Coming back from a listing, the inbox or the form refreshes the feed and the unread badge.
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        eduViewModel.onResume()
        onPauseOrDispose { eduViewModel.onPause() }
    }
    LaunchedEffect(eduState.verifiedEmail) {
        if (eduState.verifiedEmail != null) {
            showVerification = false
            viewModel.load()
        }
    }
    // A link opened from the e-mail brings the student back here; show the progress in the sheet.
    LaunchedEffect(eduState.hasReceivedLink, eduState.isConfirming) {
        if (eduState.hasReceivedLink || eduState.isConfirming) showVerification = true
    }

    val canUse = me != null && me.eduVerified && me.termsAccepted && me.suspendedUntil == null
    Good4Scaffold(
        topBar = {
            Good4TopBar(
                title = "Kampüs Dolabı",
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
                },
                actions = {
                    if (canUse) {
                        HeaderActions(
                            unreadCount = me?.unreadCount ?: 0,
                            onOpenMyListings = onOpenMyListings,
                            onOpenInbox = onOpenInbox
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (canUse) {
                ExtendedFloatingActionButton(
                    onClick = onNewListing,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("İlan ver", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(AppBackground).padding(padding)) {
            when {
                me == null && state.isLoading -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center)
                )
                me == null -> CenteredState(state.loadError ?: "Yüklenemedi.", "Tekrar dene", viewModel::load)
                !me.enabled -> CenteredState("Kampüs Dolabı şu an bakımda. Daha sonra tekrar dene.")
                me.eduVerified && !me.termsAccepted -> CampusClosetTermsContent(
                    accepting = state.acceptingTerms,
                    error = state.termsError,
                    onAccept = { viewModel.acceptTerms(me.termsVersion) }
                )
                else -> FeedGrid(
                    state = state,
                    me = me,
                    linkSentTo = eduState.sentTo,
                    canSell = canUse,
                    onSelectCategory = viewModel::selectCategory,
                    onLoadMore = viewModel::loadMore,
                    onRetry = viewModel::load,
                    onOpenListing = onOpenListing,
                    onNewListing = onNewListing,
                    onVerify = { showVerification = true }
                )
            }
        }
    }

    if (showVerification) {
        ModalBottomSheet(
            onDismissRequest = { showVerification = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AppBackground
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding()
            ) {
                CampusEmailVerificationCard(
                    state = eduState,
                    onEmailChange = eduViewModel::setEmail,
                    onSendLink = eduViewModel::sendLink,
                    onChangeEmail = eduViewModel::changeEmail,
                    onRetryLink = eduViewModel::retryLink
                )
            }
        }
    }
}

/** Same white pill as the bell and profile buttons on the home header. */
@Composable
private fun HeaderActions(unreadCount: Int, onOpenMyListings: () -> Unit, onOpenInbox: () -> Unit) {
    Surface(
        modifier = Modifier.padding(end = 8.dp),
        shape = RoundedCornerShape(50),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) {
        Row {
            IconButton(onClick = onOpenMyListings) {
                Icon(Icons.Outlined.Inventory2, contentDescription = "İlanlarım", tint = TextPrimary)
            }
            IconButton(onClick = onOpenInbox) {
                BadgedBox(badge = {
                    if (unreadCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(unreadCount.coerceAtMost(99).toString())
                        }
                    }
                }) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Mesajlar", tint = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun FeedGrid(
    state: CampusClosetFeedState,
    me: MarketMe,
    linkSentTo: String?,
    canSell: Boolean,
    onSelectCategory: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenListing: (String) -> Unit,
    onNewListing: () -> Unit,
    onVerify: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = query.trim().lowercase()
    val visibleListings = if (normalizedQuery.isEmpty()) state.listings
    else state.listings.filter { it.title.lowercase().contains(normalizedQuery) }

    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= gridState.layoutInfo.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, state.nextBefore) {
        if (nearEnd && state.nextBefore != null) onLoadMore()
    }
    val full = GridItemSpan(2)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (canSell) {
            item(span = { full }) { com.good4.notification.CampusPushPermissionCard() }
        }
        if (!me.eduVerified) {
            item(span = { full }) {
                VerifyCard(linkSentTo = linkSentTo, onClick = onVerify)
            }
        }
        me.suspendedUntil?.let {
            item(span = { full }) {
                MarketNotice(
                    "Yasak ürün içeren denemeler nedeniyle Kampüs Dolabı erişimin ${formatMarketTime(it)} tarihine kadar kapalı. " +
                        "İlanlara göz atabilirsin.",
                    color = ErrorRed
                )
            }
        }
        item(span = { full }) {
            SearchField(query = query, onQueryChange = { query = it.take(60) })
        }
        item(span = { full }) {
            CategoryRow(selected = state.category, onSelect = onSelectCategory)
        }
        item(span = { full }) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    CATEGORY_STYLES[state.category]?.takeIf { state.category != null }?.label ?: "Yeni eklenenler",
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f)
                )
                if (visibleListings.isNotEmpty()) {
                    Text("${visibleListings.size} ilan", fontSize = 13.sp, color = TextSecondary)
                }
            }
        }
        when {
            state.isLoading -> item(span = { full }) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            state.loadError != null && state.listings.isEmpty() -> item(span = { full }) {
                CenteredState(state.loadError, "Tekrar dene", onRetry)
            }
            visibleListings.isEmpty() -> item(span = { full }) {
                EmptyCloset(
                    searching = normalizedQuery.isNotEmpty(),
                    filtered = state.category != null,
                    canSell = canSell,
                    onPrimary = if (canSell) onNewListing else onVerify
                )
            }
            else -> {
                items(visibleListings, key = { it.id }) { listing ->
                    ListingGridCard(listing, showUniversity = !me.eduVerified) { onOpenListing(listing.id) }
                }
                if (state.isLoadingMore) {
                    item(span = { full }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

/** Tilted icon tile from the home quick actions. */
@Composable
internal fun TiltedIcon(icon: ImageVector, accent: Color, size: Int = 36, iconSize: Int = 20) {
    val tint = if (accent == PrimaryGreen) MaterialTheme.colorScheme.primary else accent
    Box(Modifier.size(size.dp)) {
        Box(
            Modifier.align(Alignment.Center).offset(x = 2.dp, y = 2.dp).size(size.dp)
                .graphicsLayer(rotationZ = -5f)
                .background(TextPrimary.copy(alpha = 0.16f), RoundedCornerShape((size * 0.3).dp))
        )
        Surface(
            modifier = Modifier.align(Alignment.Center).size(size.dp).graphicsLayer(rotationZ = -5f),
            shape = RoundedCornerShape((size * 0.3).dp),
            color = tint.copy(alpha = 0.24f),
            border = BorderStroke(1.dp, tint.copy(alpha = 0.88f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize.dp))
            }
        }
    }
}

@Composable
private fun VerifyCard(linkSentTo: String?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (linkSentTo != null) "Bağlantı e-postana gönderildi" else "Okul e-postanı doğrula",
                    fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary
                )
                Text(
                    if (linkSentTo != null) "Outlook'u ve gereksiz e-posta / spam klasörünü kontrol et."
                    else "İlan vermek, mesaj ve teklif göndermek için @ogr.akdeniz.edu.tr adresin gerekli.",
                    fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            TiltedIcon(Icons.Outlined.School, PrimaryGreen)
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Ürün ara", color = TextSecondary, maxLines = 1) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Aramayı temizle", tint = TextSecondary)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceDefault,
            unfocusedContainerColor = SurfaceDefault,
            unfocusedBorderColor = BorderMuted.copy(alpha = 0.55f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun CategoryRow(selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
    ) {
        items(CATEGORY_STYLES.entries.toList(), key = { it.key ?: "all" }) { (id, style) ->
            val isSelected = selected == id
            Surface(
                onClick = { onSelect(id) },
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDefault,
                border = BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else BorderMuted.copy(alpha = 0.55f)
                ),
                shadowElevation = 1.dp
            ) {
                Column(
                    Modifier.width(64.dp).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TiltedIcon(style.icon, style.accent, size = 28, iconSize = 16)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        style.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCloset(searching: Boolean, filtered: Boolean, canSell: Boolean, onPrimary: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TiltedIcon(Icons.Outlined.Checkroom, Color(0xFFE59AC0), size = 56, iconSize = 28)
            Spacer(Modifier.height(16.dp))
            Text(
                when {
                    searching -> "Aradığın ürün yok"
                    filtered -> "Bu kategori henüz boş"
                    else -> "Dolap henüz boş"
                },
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (searching) "Başka bir kelime dene ya da kategorilere göz at."
                else "Kullanmadığın kıyafetler, kitaplar ve eşyalar başka bir öğrencinin işine yarar. İlk ilanı sen ver.",
                fontSize = 13.sp, lineHeight = 19.sp, color = TextSecondary, textAlign = TextAlign.Center
            )
            if (!searching) {
                Spacer(Modifier.height(16.dp))
                Surface(onClick = onPrimary, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                    Text(
                        if (canSell) "İlan ver" else "Doğrula, satmaya başla",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                    )
                }
            }
        }
    }
}
