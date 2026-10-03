package com.good4.campuscloset

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.*
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import org.koin.compose.viewmodel.koinViewModel

private val MEETING_POINTS = listOf("Kütüphane önü", "Yemekhane girişi", "Fakülte girişi", "Kampüs ana kapısı")

@Composable
fun CampusClosetChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    onOpenListing: (String) -> Unit,
    viewModel: CampusClosetChatViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(conversationId) { viewModel.load(conversationId) }
    LifecycleResumeEffect(conversationId) {
        viewModel.startPolling()
        onPauseOrDispose { viewModel.stopPolling() }
    }
    CampusClosetChatContent(
        conversationId, state, onBack, onOpenListing,
        onDraftChange = viewModel::setDraft,
        onMeetingPoint = { viewModel.appendToDraft("Buluşma için $it uygun mu? Hangi saat olur?") },
        onSend = viewModel::send,
        onOffer = viewModel::sendOffer,
        onRespondOffer = viewModel::respondOffer,
        onReport = viewModel::report,
        onBlock = viewModel::block
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CampusClosetChatContent(
    conversationId: String,
    state: CampusClosetChatState,
    onBack: () -> Unit,
    onOpenListing: (String) -> Unit,
    onDraftChange: (String) -> Unit,
    onMeetingPoint: (String) -> Unit,
    onSend: () -> Unit,
    onOffer: (Int) -> Unit,
    onRespondOffer: (Boolean) -> Unit,
    onReport: (String, String) -> Unit,
    onBlock: () -> Unit
) {
    val conversation = state.conversation
    var menuOpen by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    var offering by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val listingId = conversationId.substringBefore('_')
    LaunchedEffect(state.messages.size, state.isNew) {
        if (state.messages.isNotEmpty()) {
            // The safety notice and optional introduction precede the messages.
            listState.animateScrollToItem(state.messages.size + if (state.isNew) 1 else 0)
        }
    }
    Good4Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            Good4TopBar(
                titleContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        conversation?.let { ClosetAvatar(it.otherName, Modifier.size(38.dp)) }
                        Column {
                            Text(conversation?.otherName ?: "Sohbet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            conversation?.let { Text(if (it.isSeller) "Alıcı" else "Satıcı", fontSize = 12.sp, color = TextSecondary) }
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Diğer") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("İlanı görüntüle") }, onClick = { menuOpen = false; onOpenListing(listingId) })
                            if (conversation != null) {
                                DropdownMenuItem(text = { Text("Şikayet et") }, onClick = { menuOpen = false; reporting = true })
                                if (conversation.status != "blocked") {
                                    DropdownMenuItem(text = { Text("Kullanıcıyı engelle", color = ErrorRed) }, onClick = { menuOpen = false; confirmBlock = true })
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                state.info?.let { MarketNotice(it, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                state.error?.let { Text(it, color = ErrorRed, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
                if (!state.isLoading && state.loadError == null) {
                    Composer(
                        state,
                        canOffer = conversation?.isSeller != true && conversation?.offer?.status != "pending" && (conversation?.listingPrice ?: 1) > 0,
                        onDraftChange, onMeetingPoint, onSend, onOffer = { offering = true }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().background(AppBackground).padding(padding).consumeWindowInsets(padding)) {
            if (!state.isLoading && state.loadError == null) {
                ChatListingSummary(conversation, onOpen = { onOpenListing(listingId) })
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
                    state.loadError != null -> CenteredState(state.loadError, "Geri dön", onBack)
                    else -> LazyColumn(
                        state = listState, contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            MarketNotice("Kampüste kalabalık bir yerde buluşun. Ürünü görmeden ödeme yapmayın. Telefon numaranızı yalnızca anlaştığınız kişiyle paylaşın.", color = TextSecondary)
                        }
                        if (state.isNew) {
                            item { Text("Satıcıya ilk mesajını yaz. Ürünün durumu, buluşma yeri ve saati hakkında konuşabilirsiniz.", color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) }
                        }
                        items(state.messages, key = { it.id }) { message ->
                            MessageBubble(
                                message,
                                canRespond = conversation?.isSeller == true && message.type == "offer" && conversation.offer?.status == "pending" && message.id == state.messages.lastOrNull { it.type == "offer" }?.id,
                                busy = state.sending, onRespond = onRespondOffer
                            )
                        }
                    }
                }
            }
        }
    }
    if (reporting) {
        ReportDialog("Konuşmayı şikayet et", onDismiss = { reporting = false }, onSubmit = { reason, note -> reporting = false; onReport(reason, note) })
    }
    if (confirmBlock) {
        AlertDialog(
            onDismissRequest = { confirmBlock = false }, title = { Text("Kullanıcı engellensin mi?") },
            text = { Text("Bu kişi sana artık mesaj gönderemez ve ilanları sana gösterilmez.") },
            confirmButton = { TextButton(onClick = { confirmBlock = false; onBlock() }) { Text("Engelle", color = ErrorRed) } },
            dismissButton = { TextButton(onClick = { confirmBlock = false }) { Text("Vazgeç") } }
        )
    }
    if (offering) {
        ModalBottomSheet(onDismissRequest = { offering = false }, containerColor = SurfaceDefault) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ClosetSectionHeading("Teklif ver", Icons.Outlined.LocalOffer, "İlan fiyatı üzerinden indirim seç.")
                CampusClosetLimits.OFFER_PERCENTS.forEach { percent ->
                    Surface(
                        onClick = { offering = false; onOffer(percent) }, shape = RoundedCornerShape(16.dp), color = SurfaceDefault,
                        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("%$percent indirim", color = TextPrimary, modifier = Modifier.weight(1f))
                            conversation?.let { Text(formatPrice(offerPrice(it.listingPrice, percent)), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatListingSummary(conversation: MarketConversation?, onOpen: () -> Unit) {
    Surface(color = SurfaceDefault, shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(12.dp)) { ListingPhoto(conversation?.listingThumbUrl, Modifier.size(52.dp), iconSize = 24) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(conversation?.listingTitle?.ifBlank { "Kampüs ilanı" } ?: "Kampüs ilanı", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(conversation?.let { formatPrice(it.listingPrice) } ?: "Fiyatı ilanda gör", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onOpen, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("İlana git", fontSize = 12.sp) }
        }
    }
}

@Composable
private fun MessageBubble(message: MarketMessage, canRespond: Boolean, busy: Boolean, onRespond: (Boolean) -> Unit) {
    if (message.type == "offerResponse") {
        Text(message.text, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        return
    }
    val isOffer = message.type == "offer"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start) {
        Surface(
            shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp,
            color = when {
                isOffer -> ClosetOfferAccent.copy(alpha = 0.12f).compositeOver(SurfaceDefault)
                message.mine -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f).compositeOver(SurfaceDefault)
                else -> SurfaceDefault
            },
            border = BorderStroke(1.dp, if (isOffer) ClosetOfferAccent.copy(alpha = 0.5f) else BorderMuted.copy(alpha = 0.55f)),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isOffer) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.LocalOffer, null, tint = ClosetOfferAccent, modifier = Modifier.size(18.dp))
                        Text("Teklif${message.offerPercent?.let { " · %$it indirim" } ?: ""}", fontWeight = FontWeight.SemiBold, color = ClosetOfferAccent, fontSize = 13.sp)
                    }
                    message.offerPrice?.let { Text(formatPrice(it), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp) }
                }
                Text(message.text, color = TextPrimary, fontSize = 15.sp, lineHeight = 21.sp)
                Text(formatMarketTime(message.createdAt), color = TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
                if (canRespond) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedButton(onClick = { onRespond(false) }, enabled = !busy, shape = RoundedCornerShape(12.dp)) { Text("Reddet") }
                        Button(onClick = { onRespond(true) }, enabled = !busy, shape = RoundedCornerShape(12.dp)) { Text("Kabul et") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Composer(state: CampusClosetChatState, canOffer: Boolean, onDraftChange: (String) -> Unit, onMeetingPoint: (String) -> Unit, onSend: () -> Unit, onOffer: () -> Unit) {
    val conversation = state.conversation
    val blocked = conversation?.status == "blocked"
    Surface(color = SurfaceDefault, shadowElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            when {
                blocked -> Text(if (conversation?.blockedByMe == true) "Bu kullanıcıyı engelledin." else "Bu konuşmaya mesaj gönderilemiyor.", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                else -> {
                    if (state.draft.isBlank()) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MEETING_POINTS.forEach { point ->
                                AssistChip(onClick = { onMeetingPoint(point) }, label = { Text(point, fontSize = 12.sp) }, leadingIcon = { Icon(Icons.Outlined.LocationOn, null, Modifier.size(15.dp)) }, shape = RoundedCornerShape(12.dp))
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canOffer) {
                            Surface(onClick = onOffer, enabled = !state.sending, shape = RoundedCornerShape(14.dp), color = ClosetOfferAccent.copy(alpha = 0.12f), modifier = Modifier.size(42.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.LocalOffer, "Teklif ver", tint = ClosetOfferAccent, modifier = Modifier.size(21.dp)) }
                            }
                        }
                        OutlinedTextField(
                            value = state.draft, onValueChange = onDraftChange, placeholder = { Text("Mesaj yaz", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f), maxLines = 4, shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderMuted.copy(alpha = 0.55f))
                        )
                        FilledIconButton(onClick = onSend, enabled = state.draft.isNotBlank() && !state.sending, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(42.dp)) {
                            if (state.sending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.AutoMirrored.Filled.Send, "Gönder", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
