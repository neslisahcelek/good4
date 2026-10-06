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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.campuscloset.CenteredState
import com.good4.campuscloset.formatRelativeTime
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SocialRequestsScreen(
    activityId: String,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: SocialRequestsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(activityId) { viewModel.load(activityId) }
    SocialRequestsContent(state, onBack, onOpenChat, viewModel::respond)
}

@Composable
internal fun SocialRequestsContent(
    state: SocialRequestsState,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onRespond: (String, Boolean) -> Unit
) {
    val list = state.list
    Good4Scaffold(
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.social_requests_title),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.social_back)) } }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(AppBackground).padding(padding)) {
            when {
                state.isLoading -> SocialCenteredProgress()
                list == null -> CenteredState(state.loadError?.asString() ?: stringResource(Res.string.social_yuklenemedi),
                    stringResource(Res.string.social_geri_don), onBack)
                else -> {
                    val waiting = list.requests.filter { it.status == "pending" }
                    val accepted = list.requests.filter { it.status == "accepted" }
                    LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize()) {
                        item(key = "summary") {
                            val activity = list.activity
                            Surface(shape = RoundedCornerShape(14.dp), color = SurfaceDefault,
                                border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(activity.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(Res.string.social_joined_count, activity.acceptedCount, activity.capacity),
                                        color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                }
                            }
                            state.error?.let { Text(it.asString(), color = ErrorRed, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)) }
                        }
                        item(key = "waiting-title") { Heading(stringResource(Res.string.social_requests_waiting, waiting.size)) }
                        if (waiting.isEmpty()) {
                            item(key = "waiting-empty") {
                                Text(stringResource(Res.string.social_requests_none), color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                        items(waiting, key = { "w-${it.requesterUid}" }) { request ->
                            RequestRow(request, busy = state.busyUid == request.requesterUid, anyBusy = state.busyUid != null) { accept ->
                                onRespond(request.requesterUid, accept)
                            }
                        }
                        if (accepted.isNotEmpty()) {
                            item(key = "accepted-title") { Heading(stringResource(Res.string.social_requests_accepted)) }
                            items(accepted, key = { "a-${it.requesterUid}" }) { request ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SocialAvatar(request.name, request.photoUrl, size = 40.dp)
                                    Text(request.name, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                    request.conversationId?.let { id ->
                                        Surface(onClick = { onOpenChat(id) }, shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                            Icon(Icons.Outlined.ChatBubbleOutline, stringResource(Res.string.social_write_message),
                                                tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(9.dp).size(18.dp))
                                        }
                                    }
                                }
                                HorizontalDivider(color = BorderMuted.copy(alpha = 0.35f))
                            }
                        }
                        item(key = "hint") {
                            Text(stringResource(Res.string.social_requests_hint), color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
                                modifier = Modifier.padding(top = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
}

@Composable
private fun RequestRow(request: SocialJoinRequest, busy: Boolean, anyBusy: Boolean, onRespond: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SocialAvatar(request.name, request.photoUrl, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(request.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (request.note.isNotBlank()) Text(request.note, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
            else Text(stringResource(Res.string.social_no_note), color = TextSecondary, fontSize = 13.sp)
            Text(formatRelativeTime(request.createdAt), color = TextSecondary, fontSize = 11.sp)
        }
        if (busy) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(onClick = { onRespond(false) }, enabled = !anyBusy, shape = CircleShape, color = SurfaceDefault,
                    border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f))) {
                    Icon(Icons.Outlined.Close, stringResource(Res.string.social_reddet), tint = TextSecondary, modifier = Modifier.padding(9.dp).size(18.dp))
                }
                Surface(onClick = { onRespond(true) }, enabled = !anyBusy, shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Outlined.Check, stringResource(Res.string.social_kabul_et), tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(9.dp).size(18.dp))
                }
            }
        }
    }
    HorizontalDivider(color = BorderMuted.copy(alpha = 0.35f))
}
