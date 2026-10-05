package com.good4.campuscloset

import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
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
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.campus_closet_back)) }
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
    CampusClosetMyListingsContent(state, onBack, onOpenListing, viewModel::load, viewModel::updateStatus,
        onRenew = viewModel::renew, onEditPrice = viewModel::beginPriceEdit, onPriceChange = viewModel::setEditPrice,
        onFreeChange = viewModel::setEditFree, onDismissPrice = viewModel::dismissPriceEdit, onSavePrice = viewModel::savePriceEdit)
}

@Composable
internal fun CampusClosetMyListingsContent(
    state: CampusClosetMyListingsState,
    onBack: () -> Unit,
    onOpenListing: (String) -> Unit,
    onRetry: () -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onRenew: (String) -> Unit = {},
    onEditPrice: (MarketListing) -> Unit = {},
    onPriceChange: (String) -> Unit = {},
    onFreeChange: (Boolean) -> Unit = {},
    onDismissPrice: () -> Unit = {},
    onSavePrice: () -> Unit = {}
) {
    var listingToRemove by remember { mutableStateOf<MarketListing?>(null) }
    ListScaffold(stringResource(Res.string.campus_closet_ilanlarim), onBack) {
        when {
            state.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            state.loadError != null -> CenteredState(state.loadError, stringResource(Res.string.campus_closet_retry), onRetry)
            state.listings.isEmpty() -> ClosetEmptyState(Icons.Outlined.Inventory2, stringResource(Res.string.campus_closet_dolabin_henuz_bos), stringResource(Res.string.campus_closet_ilk_ilanini_paylas_artik_kullanmadigin_urunler_kampuste_yeni_birine))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text(stringResource(Res.string.campus_closet_ilanin_var, state.listings.size), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(Res.string.campus_closet_ilanlarinin_durumunu_buradan_takip_et), color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
                state.message?.let { message -> item { MarketNotice(message, color = ErrorRed) } }
                items(state.listings, key = { it.id }) { listing ->
                    MyListingCard(listing, state.busyId == listing.id, { onOpenListing(listing.id) }) { action ->
                        when (action) {
                            "remove" -> listingToRemove = listing
                            "editPrice" -> onEditPrice(listing)
                            "renew" -> onRenew(listing.id)
                            else -> onUpdateStatus(listing.id, action)
                        }
                    }
                }
            }
        }
    }
    state.priceEdit?.let { draft ->
        PriceEditDialog(draft, onPriceChange, onFreeChange, onDismissPrice, onSavePrice)
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
                    if (days == 0) stringResource(Res.string.campus_closet_bugun_yayindan_kalkacak) else stringResource(Res.string.campus_closet_gun_sonra_yayindan_kalkacak, days),
                    fontSize = 12.sp, color = if (days <= 3) ErrorRed else TextSecondary
                )
            }
            "expired" -> Text(
                if (listing.renewsLeft > 0) stringResource(Res.string.campus_closet_suresi_doldu_30_gun_daha_yayinda_tutabilirsin) else stringResource(Res.string.campus_closet_suresi_doldu_yeni_ilan_verebilirsin),
                fontSize = 12.sp, color = ErrorRed
            )
            "inactive" -> Text(stringResource(Res.string.campus_closet_inactive_notice), fontSize = 12.sp, color = TextSecondary)
            "pending" -> Text(stringResource(Res.string.campus_closet_inceleniyor_yaklasik_30_dakika_icinde_yayina_alinir), fontSize = 12.sp, lineHeight = 17.sp, color = TextSecondary)
            "rejected" -> MarketNotice(listing.rejectReason ?: stringResource(Res.string.campus_closet_ilanin_yayinlanmadi_ayrintilar_icin_ilanini_acabilirsin), color = ErrorRed)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ListingAction(stringResource(Res.string.campus_closet_ilani_ac), enabled = !busy, onClick = onOpen)
            if (listing.canRenew(kotlinx.datetime.Clock.System.now().toEpochMilliseconds())) {
                ListingAction(stringResource(Res.string.campus_closet_30_gun_uzat), !busy) { onAction("renew") }
            }
            if (listing.status in listOf("pending", "published", "reserved")) {
                ListingAction(stringResource(Res.string.campus_closet_edit_price), !busy) { onAction("editPrice") }
            }
            when (listing.status) {
                "published", "reserved" -> {
                    ListingAction(stringResource(Res.string.campus_closet_yayindan_kaldir), !busy, destructive = true) { onAction("remove") }
                    ListingAction(stringResource(Res.string.campus_closet_satildi), !busy) { onAction("markSold") }
                }
                "sold", "expired" -> ListingAction(stringResource(Res.string.campus_closet_yayindan_kaldir), !busy, destructive = true) { onAction("remove") }
                "pending", "rejected" -> ListingAction(stringResource(Res.string.campus_closet_ilani_kaldir), !busy, destructive = true) { onAction("remove") }
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
    ListScaffold(stringResource(Res.string.campus_closet_mesajlar), onBack, actions = {
        IconButton(onClick = onOpenBlocked) { Icon(Icons.Outlined.Block, stringResource(Res.string.campus_closet_engellediklerin), tint = TextSecondary) }
    }) {
        when {
            state.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            state.loadError != null -> CenteredState(state.loadError, stringResource(Res.string.campus_closet_retry), onRetry)
            state.conversations.isEmpty() -> ClosetEmptyState(Icons.AutoMirrored.Outlined.Chat, stringResource(Res.string.campus_closet_konusma_burada_baslar), stringResource(Res.string.campus_closet_begendigin_bir_ilanin_saticisina_yaz_urun_ve_bulusma_ayrintilarini))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { Text(stringResource(Res.string.campus_closet_kampusteki_konusmalarin), color = TextSecondary, fontSize = 13.sp) }
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
                Text(conversation.otherName.ifBlank { stringResource(Res.string.campus_closet_student) }, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (conversation.isSeller) stringResource(Res.string.campus_closet_alici) else stringResource(Res.string.campus_closet_satici), color = TextSecondary, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(formatMarketTime(conversation.lastMessageAt), fontSize = 11.sp, color = TextSecondary)
                if (conversation.unread > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text(if (conversation.unread > 99) stringResource(Res.string.campus_closet_99) else conversation.unread.toString(), modifier = Modifier.padding(horizontal = 3.dp))
                    }
                }
            }
        }
        Text(
            (if (conversation.lastMessageMine) stringResource(Res.string.campus_closet_sen) else "") + conversation.lastMessageText,
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
                    Text(stringResource(Res.string.campus_closet_teklif_bekliyor), color = ClosetOfferAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
    ListScaffold(stringResource(Res.string.campus_closet_kaydedilenler), onBack) {
        when {
            state.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            state.loadError != null -> CenteredState(state.loadError!!, stringResource(Res.string.campus_closet_retry), viewModel::load)
            state.listings.isEmpty() -> ClosetEmptyState(
                Icons.Outlined.FavoriteBorder, stringResource(Res.string.campus_closet_kaydettigin_ilan_yok),
                stringResource(Res.string.campus_closet_begendigin_ilanlardaki_kalbe_dokun_burada_toplanirlar_satilan_ya_da)
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
    ListScaffold(stringResource(Res.string.campus_closet_engellediklerin), onBack) {
        when {
            state.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            state.loadError != null -> CenteredState(state.loadError!!, stringResource(Res.string.campus_closet_retry), viewModel::load)
            state.blocked.isEmpty() -> ClosetEmptyState(Icons.Outlined.Block, stringResource(Res.string.campus_closet_engelledigin_kimse_yok), stringResource(Res.string.campus_closet_bir_konusmada_kullaniciyi_engellersen_burada_gorunur_ve_engeli_buradan))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.message?.let { message -> item { MarketNotice(message, color = ErrorRed) } }
                items(state.blocked, key = { it.conversationId }) { entry ->
                    ClosetCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ClosetAvatar(entry.otherName)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(entry.otherName.ifBlank { stringResource(Res.string.campus_closet_student) }, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(entry.listingTitle, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            val busy = state.busyId == entry.conversationId
                            ListingAction(if (busy) stringResource(Res.string.campus_closet_kaldiriliyor) else stringResource(Res.string.campus_closet_engeli_kaldir), enabled = state.busyId == null) {
                                viewModel.unblock(entry.conversationId)
                            }
                        }
                    }
                }
            }
        }
    }
}
