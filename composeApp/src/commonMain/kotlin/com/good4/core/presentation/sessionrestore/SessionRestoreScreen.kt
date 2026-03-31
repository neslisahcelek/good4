package com.good4.core.presentation.sessionrestore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.TextPrimary
import com.good4.navigation.Route
import com.good4.user.domain.UserRole
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.loading
import good4.composeapp.generated.resources.logout
import good4.composeapp.generated.resources.session_restore_retry
import good4.composeapp.generated.resources.session_restore_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun SessionRestoreScreenRoot(
    viewModel: SessionRestoreViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: (UserRole) -> Unit,
    onNavigateToEmailVerification: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.targetRoute, state.userRole) {
        when (state.targetRoute) {
            Route.Login -> onNavigateToLogin()
            Route.EmailVerification -> onNavigateToEmailVerification()
            Route.AdminHome, Route.BusinessHome, Route.StudentHome, Route.SupporterHome -> {
                state.userRole?.let(onNavigateToHome)
            }

            null -> Unit
            else -> Unit
        }
    }

    SessionRestoreScreen(
        state = state,
        onRetry = viewModel::onRetry,
        onLogout = viewModel::onLogout
    )
}

@Composable
private fun SessionRestoreScreen(
    state: SessionRestoreState,
    onRetry: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(color = TextPrimary)
            Text(
                text = stringResource(Res.string.loading),
                color = TextPrimary,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            Text(
                text = stringResource(Res.string.session_restore_title),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            val error = state.errorMessage
            if (error != null) {
                Text(
                    text = error.asString(),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
            }
            Button(onClick = onRetry) {
                Text(text = stringResource(Res.string.session_restore_retry))
            }
            Button(
                onClick = onLogout,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(text = stringResource(Res.string.logout))
            }
        }
    }
}
