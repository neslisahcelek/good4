package com.good4.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.campuscloset.CenteredState
import com.good4.campuscloset.ClosetEmptyState
import com.good4.campuscloset.MarketNotice
import com.good4.campuscloset.ReportDialog
import com.good4.campuscloset.formatMarketTime
import com.good4.campuscloset.formatRelativeTime
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SocialInboxScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: SocialInboxViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }
    Good4Scaffold(
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.social_mesajlar),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.social_back)) } }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(AppBackground).padding(padding)) {
            val inbox = state.inbox
            when {
                state.isLoading -> SocialCenteredProgress()
                inbox == null -> CenteredState(state.loadError?.asString() ?: stringResource(Res.string.social_yuklenemedi),
                    stringResource(Res.string.social_retry), viewModel::load)
                inbox.conversations.isEmpty() -> ClosetEmptyState(Icons.Outlined.ChatBubbleOutline,
                    stringResource(Res.string.social_inbox_empty_title), stringResource(Res.string.social_inbox_empty_text))
                else -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(inbox.conversations, key = { it.id }) { conversation -> InboxRow(conversation) { onOpenChat(conversation.id) } }
                }
            }
        }
    }
}

@Composable
private fun InboxRow(conversation: SocialConversation, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SocialActivityPoster(conversation.activityType, conversation.activityKind, Modifier.width(44.dp).height(55.dp).clip(RoundedCornerShape(9.dp)))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(conversation.otherName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(formatRelativeTime(conversation.lastMessageAt), color = TextSecondary, fontSize = 11.sp)
            }
            Text(conversation.activityTitle, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(conversation.lastMessageText, color = if (conversation.unread > 0) TextPrimary else TextSecondary, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (conversation.unread > 0) Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(conversation.unread.coerceAtMost(99).toString()) }
            }
        }
    }
    HorizontalDivider(color = BorderMuted.copy(alpha = 0.35f))
}

@Composable
fun SocialChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    onOpenActivity: (String) -> Unit,
    viewModel: SocialChatViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(conversationId) { viewModel.load(conversationId) }
    LifecycleResumeEffect(conversationId) {
        viewModel.startPolling()
        onPauseOrDispose { viewModel.stopPolling() }
    }
    SocialChatContent(state, onBack, onOpenActivity, viewModel::setDraft, viewModel::send, viewModel::report, viewModel::block, viewModel::unblock)
}

@Composable
internal fun SocialChatContent(
    state: SocialChatState,
    onBack: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onReport: (String, String) -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit
) {
    val conversation = state.conversation
    var menuOpen by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }
    Good4Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            Good4TopBar(
                titleContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        conversation?.let { SocialAvatar(it.otherName, it.otherPhotoUrl, size = 38.dp) }
                        Column {
                            Text(conversation?.otherName ?: stringResource(Res.string.social_sohbet), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            conversation?.let {
                                Text(it.activityTitle, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { onOpenActivity(it.activityId) })
                            }
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.social_back)) } },
                actions = {
                    if (conversation != null) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, stringResource(Res.string.social_other)) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text(stringResource(Res.string.social_see_activity)) },
                                    onClick = { menuOpen = false; onOpenActivity(conversation.activityId) })
                                DropdownMenuItem(text = { Text(stringResource(Res.string.social_report)) }, onClick = { menuOpen = false; reporting = true })
                                if (conversation.blockedByMe) {
                                    DropdownMenuItem(text = { Text(stringResource(Res.string.social_engeli_kaldir)) }, onClick = { menuOpen = false; onUnblock() })
                                } else if (conversation.status != "blocked") {
                                    DropdownMenuItem(text = { Text(stringResource(Res.string.social_kullaniciyi_engelle), color = ErrorRed) },
                                        onClick = { menuOpen = false; confirmBlock = true })
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
                state.error?.let { Text(it.asString(), color = ErrorRed, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
                if (conversation != null) Composer(state, conversation, onDraftChange, onSend, onUnblock)
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(AppBackground).padding(padding).consumeWindowInsets(padding)) {
            when {
                state.isLoading -> SocialCenteredProgress()
                state.loadError != null -> CenteredState(state.loadError.asString(), stringResource(Res.string.social_geri_don), onBack)
                else -> LazyColumn(state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()) {
                    items(state.messages, key = { it.id }) { message -> Bubble(message) }
                }
            }
        }
    }
    if (reporting) {
        ReportDialog(stringResource(Res.string.social_konusmayi_sikayet_et), onDismiss = { reporting = false },
            onSubmit = { reason, note -> reporting = false; onReport(reason, note) }, reasons = SOCIAL_REPORT_REASONS)
    }
    if (confirmBlock) {
        AlertDialog(
            onDismissRequest = { confirmBlock = false },
            title = { Text(stringResource(Res.string.social_kullanici_engellensin_mi)) },
            text = { Text(stringResource(Res.string.social_block_text)) },
            confirmButton = { TextButton(onClick = { confirmBlock = false; onBlock() }) { Text(stringResource(Res.string.social_engelle), color = ErrorRed) } },
            dismissButton = { TextButton(onClick = { confirmBlock = false }) { Text(stringResource(Res.string.social_cancel)) } }
        )
    }
}

@Composable
private fun Bubble(message: SocialMessage) {
    if (message.type == "system") {
        Text(message.text, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        return
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start) {
        Surface(
            shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp,
            color = if (message.mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f).compositeOver(SurfaceDefault) else SurfaceDefault,
            border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(message.text, color = TextPrimary, fontSize = 15.sp, lineHeight = 21.sp)
                Text(formatMarketTime(message.createdAt), color = TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

@Composable
private fun Composer(state: SocialChatState, conversation: SocialConversation, onDraftChange: (String) -> Unit, onSend: () -> Unit, onUnblock: () -> Unit) {
    Surface(color = SurfaceDefault, shadowElevation = 4.dp) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            when {
                conversation.blockedByMe -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.social_bu_kullaniciyi_engelledin), color = TextSecondary, fontSize = 13.sp,
                        modifier = Modifier.weight(1f).padding(8.dp))
                    TextButton(onClick = onUnblock, enabled = !state.sending) { Text(stringResource(Res.string.social_engeli_kaldir)) }
                }
                conversation.readOnly -> Text(stringResource(Res.string.social_chat_closed), color = TextSecondary, fontSize = 13.sp,
                    modifier = Modifier.padding(8.dp))
                else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.draft, onValueChange = onDraftChange, placeholder = { Text(stringResource(Res.string.social_mesaj_yaz), fontSize = 14.sp) },
                        modifier = Modifier.weight(1f), maxLines = 4, shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderMuted.copy(alpha = 0.55f))
                    )
                    FilledIconButton(onClick = onSend, enabled = state.draft.isNotBlank() && !state.sending, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(42.dp)) {
                        if (state.sending) CircularProgressIndicator(Modifier.size(StandardButtonLoadingIndicatorSize), strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Filled.Send, stringResource(Res.string.social_gonder), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
