package com.good4.auth.presentation.verify_email

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.util.singleClick
import com.good4.user.domain.UserRole
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_resend_wait_seconds
import good4.composeapp.generated.resources.logout
import good4.composeapp.generated.resources.verify_email_check
import good4.composeapp.generated.resources.verify_email_description
import good4.composeapp.generated.resources.verify_email_resend
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
fun EmailVerificationScreen(
    modifier: Modifier = Modifier,
    state: EmailVerificationState,
    onAction: (EmailVerificationAction) -> Unit
) {
    val onCheckClick = remember { singleClick { onAction(EmailVerificationAction.OnCheckClick) } }
    val onResendClick = remember { singleClick { onAction(EmailVerificationAction.OnResendClick) } }
    val onLogoutClick = remember { singleClick { onAction(EmailVerificationAction.OnLogoutClick) } }

    Good4Scaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.verify_email_title)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.verify_email_description),
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            state.errorMessage?.let { error ->
                LaunchedEffect(error) {
                    delay(3.seconds)
                    onAction(EmailVerificationAction.OnClearError)
                }
                Text(
                    text = error.asString(),
                    color = ErrorRed,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            state.infoMessage?.let { info ->
                LaunchedEffect(info) {
                    delay(3.seconds)
                    onAction(EmailVerificationAction.OnClearInfo)
                }
                Text(
                    text = info.asString(),
                    color = DeepGreen,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCheckClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepGreen,
                    disabledContainerColor = DeepGreen.copy(alpha = 0.5f)
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = SurfaceDefault,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.verify_email_check),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onResendClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isLoading && state.canResendEmail,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    disabledContainerColor = TextPrimary.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = stringResource(Res.string.verify_email_resend),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

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

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    text = stringResource(Res.string.logout),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
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
