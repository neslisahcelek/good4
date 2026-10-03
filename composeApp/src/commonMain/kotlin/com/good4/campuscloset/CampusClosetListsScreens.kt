package com.good4.campuscloset

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.*
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
private fun ListScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Good4Scaffold(topBar = {
        Good4TopBar(title = title, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
        }, actions = actions)
    }) { padding ->
        Box(Modifier.fillMaxSize().background(AppBackground).padding(padding)) { content() }
    }
}

@Composable
fun CampusClosetMyListingsScreen(
    onBack: () -> Unit,
    onOpenListing: (String) -> Unit,
    viewModel: CampusClosetMyListingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) { viewModel.load(); onPauseOrDispose { } }
    CampusClosetMyListingsContent(state, onBack, onOpenListing, viewModel::load, viewModel::updateStatus, viewModel::updatePrice, viewModel::renew)
}

@Composable
internal fun CampusClosetMyListingsContent(
    state: CampusClosetMyListingsState,
    onBack: () -> Unit,
    onOpenListing: (String) -> Unit,
    onRetry: () -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onUpdatePrice: (String, Int) -> Unit = { _, _ -> },
    onRenew: (String) -> Unit = {}
) {
    var listingToRemove by remember { mutableStateOf<MarketListing?>(null) }
    var listingToReprice by remember { mutableStateOf<MarketListing?>(null) }
    ListScaffold("İlanlarım", onBack) {
        when {
            state.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            state.loadError != null -> CenteredState(state.loadError, "Tekrar dene", onRetry)
            state.listings.isEmpty() -> ClosetEmptyState(Icons.Outlined.Inventory2, "Dolabın henüz boş", "İlk ilanını paylaş; artık kullanmadığın ürünler kampüste yeni birine ulaşsın.")
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text("${state.listings.size} ilanın var", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("İlanlarının durumunu buradan takip et.", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
                state.message?.let { message -> item { MarketNotice(message, color = ErrorRed) } }
                items(state.listings, key = { it.id }) { listing ->
                    MyListingCard(listing, state.busyId == listing.id, { onOpenListing(listing.id) }) { action ->
                        when (action) {
                            "remove" -> listingToRemove = listing
                            "editPrice" -> listingToReprice = listing
                            "renew" -> onRenew(listing.id)
                            else -> onUpdateStatus(listing.id, action)
                        }
                    }
                }
            }
        }
    }
    listingToReprice?.let { listing ->
        PriceEditDialog(
            currentPrice = listing.price,
            onDismiss = { listingToReprice = null },
            onSave = { price ->
                listingToReprice = null
                onUpdatePrice(listing.id, price)
            }
        )
    }
    listingToRemove?.let { listing ->
        RemoveListingDialog(
            listingTitle = listing.title,
            onDismiss = { listingToRemove = null },
            onConfirm = {
                listingToRemove = null
                onUpdateStatus(listing.id, "remove")
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MyListingCard(listing: MarketListing, busy: Boolean, onOpen: () -> Unit, onAction: (String) -> Unit) {
    ClosetCard {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ListingPhoto(listing.photos.firstOrNull()?.thumbUrl, Modifier.size(82.dp).clip(RoundedCornerShape(14.dp)), listing.category, iconSize = 30)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(listing.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(formatPrice(listing.price), color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                ListingStatusChip(listing.status)
            }
        }
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        when (listing.status) {
            "published", "reserved" -> listing.daysLeft(now)?.let { days ->
                Text(
                    if (days == 0) "Bugün yayından kalkacak" else "$days gün sonra yayından kalkacak",
                    fontSize = 12.sp, color = if (days <= 3) ErrorRed else TextSecondary
                )
            }
            "expired" -> Text(
                if (listing.renewsLeft > 0) "Süresi doldu · 30 gün daha yayında tutabilirsin." else "Süresi doldu · yeni ilan verebilirsin.",
                fontSize = 12.sp, color = ErrorRed
            )
            "pending" -> Text("İnceleniyor · yaklaşık 30 dakika içinde yayına alınır.", fontSize = 12.sp, lineHeight = 17.sp, color = TextSecondary)
            "rejected" -> MarketNotice(listing.rejectReason ?: "İlanın yayınlanmadı. Ayrıntılar için ilanını açabilirsin.", color = ErrorRed)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ListingAction("İlanı aç", enabled = !busy, onClick = onOpen)
            if (listing.canRenew(kotlinx.datetime.Clock.System.now().toEpochMilliseconds())) {
                ListingAction("30 gün uzat", !busy) { onAction("renew") }
            }
            if (listing.status in listOf("pending", "published", "reserved")) {
                ListingAction("Fiyatı düzenle", !busy) { onAction("editPrice") }
            }
            when (listing.status) {
                "published", "reserved" -> {
                    ListingAction("Yayından kaldır", !busy, destructive = true) { onAction("remove") }
                    ListingAction("Satıldı", !busy) { onAction("markSold") }
                }
                "sold", "expired" -> ListingAction("Yayından kaldır", !busy, destructive = true) { onAction("remove") }
                "pending", "rejected" -> ListingAction("İlanı kaldır", !busy, destructive = true) { onAction("remove") }
            }
            if (busy) CircularProgressIndicator(Modifier.padding(10.dp).size(18.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun ListingAction(label: String, enabled: Boolean, destructive: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (destructive) ErrorRed else MaterialTheme.colorScheme.primary)
    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun CampusClosetInboxScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenBlocked: () -> Unit = {},
    viewModel: CampusClosetInboxViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) { viewModel.load(); onPauseOrDispose { } }
    CampusClosetInboxContent(state, onBack, onOpenChat, viewModel::load, onOpenBlocked)
}

@Composable
internal fun CampusClosetInboxContent(
    state: CampusClosetInboxState,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenBlocked: () -> Unit = {}
) {
    ListScaffold("Mesajlar", onBack, actions = {
        IconButton(onClick = onOpenBlocked) { Icon(Icons.Outlined.Block, "Engellediklerin", tint = TextSecondary) }
    }) {
        when {
            state.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            state.loadError != null -> CenteredState(state.loadError, "Tekrar dene", onRetry)
            state.conversations.isEmpty() -> ClosetEmptyState(Icons.AutoMirrored.Outlined.Chat, "Konuşma burada başlar", "Beğendiğin bir ilanın satıcısına yaz; ürün ve buluşma ayrıntılarını birlikte konuşun.")
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { Text("Kampüsteki konuşmaların", color = TextSecondary, fontSize = 13.sp) }
                items(state.conversations, key = { it.id }) { conversation ->
                    ConversationCard(conversation) { onOpenChat(conversation.id) }
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(conversation: MarketConversation, onClick: () -> Unit) {
    ClosetCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ClosetAvatar(conversation.otherName)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(conversation.otherName.ifBlank { "Öğrenci" }, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (conversation.isSeller) "Alıcı" else "Satıcı", color = TextSecondary, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(formatMarketTime(conversation.lastMessageAt), fontSize = 11.sp, color = TextSecondary)
                if (conversation.unread > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text(if (conversation.unread > 99) "99+" else conversation.unread.toString(), modifier = Modifier.padding(horizontal = 3.dp))
                    }
                }
            }
        }
        Text(
            (if (conversation.lastMessageMine) "Sen: " else "") + conversation.lastMessageText,
            color = if (conversation.unread > 0) TextPrimary else TextSecondary, fontSize = 14.sp, lineHeight = 19.sp,
            fontWeight = if (conversation.unread > 0) FontWeight.Medium else FontWeight.Normal,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ListingPhoto(conversation.listingThumbUrl, Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)), iconSize = 20)
            Text(conversation.listingTitle, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        if (conversation.offer?.status == "pending") {
            Surface(shape = RoundedCornerShape(8.dp), color = ClosetOfferAccent.copy(alpha = 0.12f)) {
                Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalOffer, null, tint = ClosetOfferAccent, modifier = Modifier.size(14.dp))
                    Text("Teklif bekliyor", color = ClosetOfferAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun CampusClosetFavoritesScreen(
    onBack: () -> Unit,
    onOpenListing: (String) -> Unit,
    viewModel: CampusClosetFavoritesViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) { viewModel.load(); onPauseOrDispose { } }
    ListScaffold("Kaydedilenler", onBack) {
        when {
            state.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            state.loadError != null -> CenteredState(state.loadError!!, "Tekrar dene", viewModel::load)
            state.listings.isEmpty() -> ClosetEmptyState(
                Icons.Outlined.FavoriteBorder, "Kaydettiğin ilan yok",
                "Beğendiğin ilanlardaki kalbe dokun; burada toplanırlar. Satılan ya da kaldırılan ilanlar listeden düşer."
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                state.message?.let { message -> item(span = { GridItemSpan(2) }) { MarketNotice(message, color = ErrorRed) } }
                items(state.listings, key = { it.id }) { listing ->
                    ListingGridCard(listing, showUniversity = false, onToggleFavorite = { viewModel.remove(listing) }) {
                        onOpenListing(listing.id)
                    }
                }
            }
        }
    }
}

@Composable
fun CampusClosetBlockedScreen(
    onBack: () -> Unit,
    viewModel: CampusClosetBlockedViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) { viewModel.load(); onPauseOrDispose { } }
    ListScaffold("Engellediklerin", onBack) {
        when {
            state.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            state.loadError != null -> CenteredState(state.loadError!!, "Tekrar dene", viewModel::load)
            state.blocked.isEmpty() -> ClosetEmptyState(Icons.Outlined.Block, "Engellediğin kimse yok", "Bir konuşmada kullanıcıyı engellersen burada görünür ve engeli buradan kaldırabilirsin.")
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.message?.let { message -> item { MarketNotice(message, color = ErrorRed) } }
                items(state.blocked, key = { it.conversationId }) { entry ->
                    ClosetCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ClosetAvatar(entry.otherName)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(entry.otherName.ifBlank { "Öğrenci" }, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(entry.listingTitle, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            val busy = state.busyId == entry.conversationId
                            ListingAction(if (busy) "Kaldırılıyor…" else "Engeli kaldır", enabled = state.busyId == null) {
                                viewModel.unblock(entry.conversationId)
                            }
                        }
                    }
                }
            }
        }
    }
}
