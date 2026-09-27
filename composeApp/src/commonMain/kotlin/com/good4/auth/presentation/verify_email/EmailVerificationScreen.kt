package com.good4.auth.presentation.verify_email

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.auth.presentation.components.AuthAccent
import com.good4.auth.presentation.components.AuthBackdrop
import com.good4.auth.presentation.components.AuthCard
import com.good4.auth.presentation.components.AuthErrorBanner
import com.good4.auth.presentation.components.AuthPrimaryButton
import com.good4.auth.presentation.components.AuthSecondaryButton
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.UiText
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.StandardButtonHeight
import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.util.singleClick
import com.good4.user.domain.UserRole
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.dismiss
import good4.composeapp.generated.resources.error_resend_wait_seconds
import good4.composeapp.generated.resources.error_email_not_verified
import good4.composeapp.generated.resources.logout
import good4.composeapp.generated.resources.verification_email_sent
import good4.composeapp.generated.resources.verify_email_check
import good4.composeapp.generated.resources.verify_email_description
import good4.composeapp.generated.resources.verify_email_resend
import good4.composeapp.generated.resources.verify_email_spam_note
import good4.composeapp.generated.resources.verify_email_title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.seconds

@Composable
fun EmailVerificationScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: EmailVerificationViewModel,
    onVerified: (UserRole) -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isVerified, state.userRole) {
        val role = state.userRole
        if (state.isVerified && role != null) {
            onVerified(role)
        }
    }

    EmailVerificationScreen(
        modifier = modifier,
        state = state,
        onAction = { action ->
            if (action is EmailVerificationAction.OnLogoutClick) {
                onLogout()
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EmailVerificationScreen(
    modifier: Modifier = Modifier,
    state: EmailVerificationState,
    onAction: (EmailVerificationAction) -> Unit
) {
    val onCheckClick = remember { singleClick { onAction(EmailVerificationAction.OnCheckClick) } }
    val onResendClick = remember { singleClick { onAction(EmailVerificationAction.OnResendClick) } }
    val onLogoutClick = remember { singleClick { onAction(EmailVerificationAction.OnLogoutClick) } }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isNotVerifiedErrorSheet =
        (state.errorMessage as? UiText.StringResourceId)?.id == Res.string.error_email_not_verified
    val isVerificationSentInfoSheet =
        (state.infoMessage as? UiText.StringResourceId)?.id == Res.string.verification_email_sent
    val isAnyLoading = state.isCheckingVerification || state.isResendingEmail || state.isLoggingOut

    Good4Scaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.verify_email_title)
            )
        }
    ) { paddingValues ->
        AuthBackdrop(modifier = Modifier.padding(paddingValues), belowTopBar = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(AuthAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.MarkEmailUnread,
                    contentDescription = null,
                    tint = AuthAccent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            AuthCard {
                Text(
                    text = stringResource(Res.string.verify_email_description),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.verify_email_spam_note),
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                state.errorMessage?.takeUnless { isNotVerifiedErrorSheet }?.let { error ->
                    LaunchedEffect(error) {
                        delay(3.seconds)
                        onAction(EmailVerificationAction.OnClearError)
                    }
                    AuthErrorBanner(text = error.asString(), modifier = Modifier.padding(top = 16.dp))
                }

                state.infoMessage?.takeUnless { isVerificationSentInfoSheet }?.let { info ->
                    LaunchedEffect(info) {
                        delay(3.seconds)
                        onAction(EmailVerificationAction.OnClearInfo)
                    }
                    Text(
                        text = info.asString(),
                        color = AuthAccent,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                AuthPrimaryButton(
                    text = stringResource(Res.string.verify_email_check),
                    onClick = onCheckClick,
                    enabled = !isAnyLoading,
                    loading = state.isCheckingVerification
                )

                Spacer(modifier = Modifier.height(12.dp))

                AuthSecondaryButton(
                    text = stringResource(Res.string.verify_email_resend),
                    onClick = onResendClick,
                    enabled = !isAnyLoading && state.canResendEmail,
                    leading = if (state.isResendingEmail) {
                        {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AuthAccent,
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        null
                    }
                )

                if (!state.canResendEmail && state.resendCooldownSeconds > 0) {
                    Text(
                        text = stringResource(
                            Res.string.error_resend_wait_seconds,
                            state.resendCooldownSeconds
                        ),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = onLogoutClick,
                enabled = !isAnyLoading
            ) {
                Text(
                    text = stringResource(Res.string.logout),
                    color = TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isNotVerifiedErrorSheet) {
                ModalBottomSheet(
                    onDismissRequest = { onAction(EmailVerificationAction.OnClearError) },
                    sheetState = bottomSheetState
                ) {
                    BottomSheetMessageContent(
                        message = state.errorMessage?.asString().orEmpty(),
                        buttonLabel = stringResource(Res.string.dismiss),
                        onButtonClick = { onAction(EmailVerificationAction.OnClearError) }
                    )
                }
            }

            if (isVerificationSentInfoSheet) {
                ModalBottomSheet(
                    onDismissRequest = { onAction(EmailVerificationAction.OnClearInfo) },
                    sheetState = bottomSheetState
                ) {
                    BottomSheetMessageContent(
                        message = state.infoMessage?.asString().orEmpty(),
                        buttonLabel = stringResource(Res.string.dismiss),
                        onButtonClick = { onAction(EmailVerificationAction.OnClearInfo) }
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun BottomSheetMessageContent(
    message: String,
    buttonLabel: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = TextPrimary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onButtonClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
        ) {
            Text(
                text = buttonLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SurfaceDefault
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview
@Composable
fun EmailVerificationScreenPreview() {
    MaterialTheme {
        EmailVerificationScreen(
            state = EmailVerificationState(),
            onAction = {}
        )
    }
}
