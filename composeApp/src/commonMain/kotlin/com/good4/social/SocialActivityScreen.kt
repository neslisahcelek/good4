package com.good4.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.campuscloset.CampusEmailVerificationCard
import com.good4.campuscloset.CampusEmailVerificationViewModel
import com.good4.campuscloset.CenteredState
import com.good4.campuscloset.MarketNotice
import com.good4.campuscloset.ReportDialog
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ClosetOfferAccent
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialActivityScreen(
    activityId: String,
    onBack: () -> Unit,
    onOpenRequests: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: SocialActivityViewModel = koinViewModel(),
    eduViewModel: CampusEmailVerificationViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val eduState by eduViewModel.state.collectAsStateWithLifecycle()
    var showVerification by rememberSaveable { mutableStateOf(false) }
    var showTerms by rememberSaveable { mutableStateOf(false) }
    // Set when the student pressed "Katılmak istiyorum" before verifying; the request goes out once they can.
    var joinAfterVerification by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(activityId) { viewModel.load(activityId) }
    LifecycleResumeEffect(activityId) {
        eduViewModel.onResume()
        onPauseOrDispose { eduViewModel.onPause() }
    }
    LaunchedEffect(eduState.verifiedEmail) {
        if (eduState.verifiedEmail != null && showVerification) {
            showVerification = false
            viewModel.load(activityId)
        }
    }
    val me = state.detail?.me
    LaunchedEffect(me?.eduVerified, me?.termsAccepted, joinAfterVerification) {
        if (!joinAfterVerification || me == null || !me.eduVerified) return@LaunchedEffect
        joinAfterVerification = false
        if (me.termsAccepted) viewModel.requestToJoin() else showTerms = true
    }

    SocialActivityContent(
        state = state,
        onBack = onBack,
        onNote = viewModel::setRequestNote,
        onJoin = {
            val current = state.detail?.me
            when {
                current == null -> Unit
                !current.eduVerified -> { joinAfterVerification = true; showVerification = true }
                !current.termsAccepted -> showTerms = true
                else -> viewModel.requestToJoin()
            }
        },
        onWithdraw = viewModel::withdraw,
        onOpenRequests = { onOpenRequests(activityId) },
        onOpenChat = onOpenChat,
        onCancel = viewModel::cancelActivity,
        onReport = viewModel::report
    )

    if (showVerification) {
        ModalBottomSheet(
            onDismissRequest = { showVerification = false; joinAfterVerification = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AppBackground
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
                CampusEmailVerificationCard(
                    state = eduState,
                    onEmailChange = eduViewModel::setEmail,
                    onSendLink = eduViewModel::sendLink,
                    onChangeEmail = eduViewModel::changeEmail,
                    onRetryLink = eduViewModel::retryLink,
                    title = stringResource(Res.string.social_verify_title),
                    readyText = stringResource(Res.string.social_verify_ready),
                    afterVerifyText = stringResource(Res.string.social_verify_after)
                )
            }
        }
    }
    if (showTerms && me != null) {
        ModalBottomSheet(
            onDismissRequest = { showTerms = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AppBackground
        ) {
            SocialTermsContent(
                accepting = state.busy, error = state.error?.asString(),
                shownName = me.shownName, maskedName = me.maskedName,
                onAccept = { showName -> viewModel.acceptTermsAndJoin(me.termsVersion, showName); showTerms = false },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

@Composable
internal fun SocialActivityContent(
    state: SocialActivityState,
    onBack: () -> Unit,
    onNote: (String) -> Unit,
    onJoin: () -> Unit,
    onWithdraw: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenChat: (String) -> Unit,
    onCancel: () -> Unit,
    onReport: (String, String) -> Unit
) {
    val detail = state.detail
    var menuOpen by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    Good4Scaffold(
        modifier = Modifier.imePadding(),
        bottomBar = {
            if (detail != null) {
                SocialBottomBar {
                    state.info?.let { MarketNotice(it) }
                    state.error?.let { Text(it.asString(), color = ErrorRed, fontSize = 13.sp, lineHeight = 18.sp) }
                    ActionBar(detail, state.requestNote, state.busy, onNote, onJoin, onWithdraw, onOpenRequests, onOpenChat)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(AppBackground).padding(bottom = padding.calculateBottomPadding())) {
            when {
                state.isLoading -> SocialCenteredProgress()
                detail == null -> CenteredState(state.loadError?.asString() ?: stringResource(Res.string.social_yuklenemedi),
                    stringResource(Res.string.social_geri_don), onBack)
                else -> DetailBody(detail)
            }
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
                RoundButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.social_back), onBack)
                Box(Modifier.weight(1f))
                if (detail != null && (!detail.activity.isMine || detail.activity.isUpcoming)) {
                    Box {
                        RoundButton(Icons.Outlined.MoreHoriz, stringResource(Res.string.social_other)) { menuOpen = true }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (detail.activity.isMine) {
                                DropdownMenuItem(text = { Text(stringResource(Res.string.social_cancel_activity), color = ErrorRed) },
                                    onClick = { menuOpen = false; confirmCancel = true })
                            } else {
                                DropdownMenuItem(text = { Text(stringResource(Res.string.social_report)) },
                                    onClick = { menuOpen = false; reporting = true })
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(Res.string.social_cancel_confirm_title)) },
            text = { Text(stringResource(Res.string.social_cancel_confirm_text)) },
            confirmButton = { TextButton(onClick = { confirmCancel = false; onCancel() }) { Text(stringResource(Res.string.social_cancel_activity), color = ErrorRed) } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text(stringResource(Res.string.social_keep)) } }
        )
    }
    if (reporting) {
        ReportDialog(stringResource(Res.string.social_report_activity), onDismiss = { reporting = false },
            onSubmit = { reason, note -> reporting = false; onReport(reason, note) }, reasons = SOCIAL_REPORT_REASONS)
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = SurfaceDefault, border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)), shadowElevation = 1.dp) {
        Icon(icon, description, tint = TextPrimary, modifier = Modifier.padding(9.dp).size(20.dp))
    }
}

@Composable
private fun DetailBody(detail: SocialActivityDetail) {
    val activity = detail.activity
    val instant = activity.startsAtInstant
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SocialActivityPoster(
            activity.type, activity.kind, Modifier.fillMaxWidth().height(260.dp + statusBar),
            dayLabel = instant?.let { fullWhenLabel(it).substringBefore(" · ") }, timeLabel = instant?.let(::clockLabel),
            timeSize = 34.sp, textPadding = 18.dp, alignEnd = true,
            // Status bar plus the back / more button row (36dp + 12dp margins), so the drawing never sits under them.
            artTopInset = statusBar + 60.dp, game = activity.game
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text(activity.title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
                Text(listOfNotNull(
                    stringResource(if (activity.isSport) Res.string.social_kind_sport else Res.string.social_kind_social),
                    typeLabel(activity.type), gameLabel(activity.game)
                ).joinToString(" · "),
                    color = TextSecondary, fontSize = 13.sp)
            }
            Column {
                levelLabel(activity.level)?.takeIf { activity.isSport }?.let { InfoRow(Icons.Outlined.BarChart, stringResource(Res.string.social_level_value, it)) }
                InfoRow(Icons.Outlined.Group, if (activity.isMine) stringResource(Res.string.social_joined_count, activity.acceptedCount, activity.capacity)
                    else stringResource(Res.string.social_looking_for, activity.spotsLeft))
                if (!activity.isUpcoming || activity.status == "full") {
                    InfoRow(Icons.Outlined.Schedule, if (activity.status == "full") stringResource(Res.string.social_full) else activityStatus(activity.status))
                }
            }
            activity.note?.takeIf { it.isNotBlank() }?.let { Text(it, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp) }
            if (!activity.isMine) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SocialAvatar(activity.organizerName, activity.organizerPhotoUrl, size = 42.dp)
                    Column {
                        Text(activity.organizerName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Text(" ${stringResource(Res.string.social_verified_student, activity.universityName)}", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            MarketNotice(stringResource(Res.string.social_safety_note), color = TextSecondary)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text, color = TextPrimary, fontSize = 15.sp)
    }
    HorizontalDivider(color = BorderMuted.copy(alpha = 0.35f))
}

/** One action at a time, chosen by where the student stands with this activity. */
@Composable
private fun ActionBar(
    detail: SocialActivityDetail,
    note: String,
    busy: Boolean,
    onNote: (String) -> Unit,
    onJoin: () -> Unit,
    onWithdraw: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val activity = detail.activity
    val request = detail.myRequest
    when {
        activity.isMine && activity.isUpcoming -> SocialPrimaryButton(
            if (activity.pendingCount > 0) stringResource(Res.string.social_see_requests_count, activity.pendingCount)
            else stringResource(Res.string.social_see_requests), onOpenRequests
        )
        activity.isMine -> StatusLine(activityStatus(activity.status))
        request?.status == "accepted" -> {
            request.conversationId?.let { SocialPrimaryButton(stringResource(Res.string.social_write_message), { onOpenChat(it) }) }
            if (activity.isUpcoming) {
                TextButton(onClick = onWithdraw, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.social_leave), color = TextSecondary)
                }
            }
        }
        request?.status == "pending" -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.social_waiting_answer), color = ClosetOfferAccent, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f))
            TextButton(onClick = onWithdraw, enabled = !busy) { Text(stringResource(Res.string.social_withdraw_request), color = TextSecondary) }
        }
        request?.status == "closed" || activity.status == "full" -> StatusLine(stringResource(Res.string.social_no_spot))
        !activity.isUpcoming -> StatusLine(activityStatus(activity.status))
        detail.me.suspendedUntil != null -> StatusLine(stringResource(Res.string.social_error_suspended))
        else -> {
            OutlinedTextField(
                value = note, onValueChange = onNote, maxLines = 3,
                placeholder = { Text(stringResource(Res.string.social_request_note_hint), fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderMuted.copy(alpha = 0.55f))
            )
            SocialPrimaryButton(stringResource(Res.string.social_join), onJoin, loading = busy)
        }
    }
}

@Composable
private fun StatusLine(text: String) {
    Text(text, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
}
