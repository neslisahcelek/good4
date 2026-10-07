package com.good4.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.campuscloset.CenteredState
import com.good4.campuscloset.ClosetEmptyState
import com.good4.community.SmallToggle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ClosetOfferAccent
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SocialScreen(
    onBack: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onCreate: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: SocialHomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    // Coming back from a detail, the form or a chat refreshes both lists and the badges.
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        if (tab == 1) viewModel.loadMine()
        onPauseOrDispose { }
    }
    LaunchedEffect(tab) { if (tab == 1) viewModel.loadMine() }
    SocialContent(
        state = state, tab = tab, onTab = { tab = it }, onBack = onBack, onOpenActivity = onOpenActivity, onCreate = onCreate,
        onOpenInbox = onOpenInbox, onOpenChat = onOpenChat, onSelectKind = viewModel::selectKind, onLoadMore = viewModel::loadMore,
        onRetry = viewModel::load, onRetryMine = viewModel::loadMine, onAcceptTerms = viewModel::acceptTerms,
        onUpdateProfile = viewModel::updateProfile, onProfilePhotoError = viewModel::profilePhotoError
    )
}

@Composable
internal fun SocialContent(
    state: SocialHomeState,
    tab: Int,
    onTab: (Int) -> Unit,
    onBack: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onCreate: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenChat: (String) -> Unit,
    onSelectKind: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onRetryMine: () -> Unit,
    onAcceptTerms: (Int, Boolean) -> Unit,
    onUpdateProfile: (showName: Boolean?, photo: ByteArray?, removePhoto: Boolean) -> Unit,
    onProfilePhotoError: (String) -> Unit
) {
    val me = state.me
    var showProfile by rememberSaveable { mutableStateOf(false) }
    // Browsing is open to every student; creating needs the school address and the rules.
    val needsTerms = me != null && me.enabled && me.eduVerified && !me.termsAccepted
    Good4Scaffold(
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.social_title),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.social_back)) } },
                actions = {
                    if (me?.eduVerified == true && me.termsAccepted) {
                        Surface(
                            modifier = Modifier.padding(end = 8.dp), shape = RoundedCornerShape(50), color = SurfaceDefault,
                            border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)), shadowElevation = 1.dp
                        ) {
                            IconButton(onClick = { showProfile = true }) {
                                Icon(Icons.Outlined.AccountCircle, stringResource(Res.string.social_profile_open), tint = TextPrimary)
                            }
                        }
                    }
                    if (me?.eduVerified == true) {
                        Surface(
                            modifier = Modifier.padding(end = 8.dp), shape = RoundedCornerShape(50), color = SurfaceDefault,
                            border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)), shadowElevation = 1.dp
                        ) {
                            IconButton(onClick = onOpenInbox) {
                                BadgedBox(badge = {
                                    if (me.unreadCount > 0) Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(me.unreadCount.coerceAtMost(99).toString()) }
                                }) {
                                    Icon(Icons.Outlined.ChatBubbleOutline, stringResource(Res.string.social_mesajlar), tint = TextPrimary)
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (me != null && me.enabled && !needsTerms && me.suspendedUntil == null) {
                Row(
                    Modifier.fillMaxWidth().background(AppBackground).navigationBarsPadding()
                        .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                ExtendedFloatingActionButton(
                    onClick = onCreate,
                    containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.social_create), fontWeight = FontWeight.SemiBold) }
                )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(AppBackground).padding(padding)) {
            when {
                me == null && state.isLoading -> SocialCenteredProgress()
                me == null -> CenteredState(state.loadError?.asString() ?: stringResource(Res.string.social_yuklenemedi),
                    stringResource(Res.string.social_retry), onRetry)
                !me.enabled -> CenteredState(stringResource(Res.string.social_error_disabled))
                needsTerms -> SocialTermsContent(
                    state.acceptingTerms, state.termsError?.asString(), me.shownName, me.maskedName,
                    onAccept = { showName -> onAcceptTerms(me.termsVersion, showName) }
                )
                else -> Column(Modifier.fillMaxSize()) {
                    SocialSegmented(
                        listOf(stringResource(Res.string.social_tab_discover), stringResource(Res.string.social_tab_mine)),
                        tab, onTab, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    if (tab == 0) FeedGrid(state, onOpenActivity, onSelectKind, onLoadMore, onRetry)
                    else MineList(state, me, onOpenActivity, onOpenChat, onRetryMine)
                }
            }
        }
    }
    if (showProfile && me != null) {
        SocialProfileSheet(
            me = me, saving = state.profileSaving, error = state.profileError,
            onShowName = { onUpdateProfile(it, null, false) },
            onPhoto = { onUpdateProfile(null, it, false) },
            onRemovePhoto = { onUpdateProfile(null, null, true) },
            onPhotoError = onProfilePhotoError,
            onDismiss = { showProfile = false }
        )
    }
}

@Composable
private fun FeedGrid(
    state: SocialHomeState,
    onOpenActivity: (String) -> Unit,
    onSelectKind: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallToggle(stringResource(Res.string.social_all), state.kind == null) { onSelectKind(null) }
                SmallToggle(stringResource(Res.string.social_kind_social), state.kind == "social") { onSelectKind("social") }
                SmallToggle(stringResource(Res.string.social_kind_sport), state.kind == "sport") { onSelectKind("sport") }
            }
        }
        when {
            state.isLoading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(48.dp)) { SocialCenteredProgress() }
            }
            state.loadError != null && state.activities.isEmpty() -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                CenteredState(state.loadError.asString(), stringResource(Res.string.social_retry), onRetry)
            }
            state.activities.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    ClosetEmptyState(Icons.Outlined.EventAvailable, stringResource(Res.string.social_empty_title), stringResource(Res.string.social_empty_text))
                }
            }
            else -> items(state.activities, key = { it.id }) { activity ->
                SocialPosterCard(activity, onClick = { onOpenActivity(activity.id) })
            }
        }
        if (state.nextAfter != null && !state.isLoading) {
            item(key = "more", span = { GridItemSpan(maxLineSpan) }) {
                TextButton(onClick = onLoadMore, enabled = !state.isLoadingMore) {
                    Text(stringResource(Res.string.social_load_more), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun MineList(
    state: SocialHomeState,
    me: SocialMe,
    onOpenActivity: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onRetry: () -> Unit
) {
    val mine = state.mine
    when {
        mine == null && state.mineLoading -> SocialCenteredProgress()
        mine == null -> CenteredState(state.mineError?.asString() ?: stringResource(Res.string.social_yuklenemedi),
            stringResource(Res.string.social_retry), onRetry)
        mine.organized.isEmpty() && mine.joined.isEmpty() -> ClosetEmptyState(
            Icons.Outlined.EventAvailable, stringResource(Res.string.social_mine_empty_title), stringResource(Res.string.social_mine_empty_text)
        )
        else -> {
            val opened = mine.organized.filter { it.isUpcoming }.sortedBy { it.startsAt }
            val going = mine.joined.filter { it.activity.isUpcoming && it.status == "accepted" }.sortedBy { it.activity.startsAt }
            val asked = mine.joined.filter { it.activity.isUpcoming && it.status != "accepted" }.sortedBy { it.activity.startsAt }
            val past = mine.organized.filterNot { it.isUpcoming }.map { it to null } +
                mine.joined.filterNot { it.activity.isUpcoming }.map { it.activity to it }
            var showPast by rememberSaveable { mutableStateOf(false) }
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), modifier = Modifier.fillMaxSize()) {
                if (opened.isNotEmpty()) {
                    item(key = "h-opened") { SectionTitle(stringResource(Res.string.social_mine_opened)) }
                    items(opened, key = { "o-${it.id}" }) { activity ->
                        SocialActivityRow(
                            activity,
                            status = if (activity.pendingCount > 0) stringResource(Res.string.social_new_requests, activity.pendingCount)
                            else stringResource(Res.string.social_joined_count, activity.acceptedCount, activity.capacity),
                            statusColor = MaterialTheme.colorScheme.primary,
                            onClick = { onOpenActivity(activity.id) }
                        )
                    }
                }
                if (going.isNotEmpty()) {
                    item(key = "h-going") { SectionTitle(stringResource(Res.string.social_mine_going)) }
                    items(going, key = { "g-${it.activity.id}" }) { joined ->
                        SocialActivityRow(joined.activity, status = null, statusColor = TextSecondary, onClick = { onOpenActivity(joined.activity.id) }) {
                            joined.conversationId?.let { id ->
                                Surface(onClick = { onOpenChat(id) }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                                    Icon(Icons.Outlined.ChatBubbleOutline, stringResource(Res.string.social_write_message),
                                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(9.dp))
                                }
                            }
                        }
                    }
                }
                if (asked.isNotEmpty()) {
                    item(key = "h-asked") { SectionTitle(stringResource(Res.string.social_mine_requests)) }
                    items(asked, key = { "r-${it.activity.id}" }) { joined ->
                        val (label, color) = requestStatus(joined.status)
                        SocialActivityRow(joined.activity, label, color, onClick = { onOpenActivity(joined.activity.id) })
                    }
                }
                if (past.isNotEmpty()) {
                    item(key = "past") {
                        TextButton(onClick = { showPast = !showPast }) {
                            Text(stringResource(if (showPast) Res.string.social_hide_past else Res.string.social_show_past), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (showPast) items(past, key = { "p-${it.first.id}" }) { (activity, _) ->
                        SocialActivityRow(activity, activityStatus(activity.status), TextSecondary, onClick = { onOpenActivity(activity.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp))
}

@Composable
internal fun requestStatus(status: String): Pair<String, Color> = when (status) {
    "pending" -> stringResource(Res.string.social_waiting_answer) to ClosetOfferAccent
    "accepted" -> stringResource(Res.string.social_accepted) to MaterialTheme.colorScheme.primary
    "withdrawn" -> stringResource(Res.string.social_withdrawn) to TextSecondary
    else -> stringResource(Res.string.social_no_spot) to TextSecondary
}

@Composable
internal fun activityStatus(status: String): String = when (status) {
    "cancelled" -> stringResource(Res.string.social_status_cancelled)
    "removed" -> stringResource(Res.string.social_status_removed)
    else -> stringResource(Res.string.social_status_ended)
}
