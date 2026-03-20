package com.good4.admin.presentation.editstudentcredit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.admin_users_email
import good4.composeapp.generated.resources.admin_users_edit_credit_description
import good4.composeapp.generated.resources.admin_users_override_dialog_confirm
import good4.composeapp.generated.resources.admin_users_override_dialog_placeholder
import good4.composeapp.generated.resources.admin_users_title
import good4.composeapp.generated.resources.admin_users_user_id
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentCreditScreen(
    modifier: Modifier = Modifier,
    viewModel: EditStudentCreditViewModel = koinViewModel(),
    onMenuClick: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
        state.successMessage?.let { snackbarHostState.showSnackbar(it) }
        if (state.errorMessage != null || state.successMessage != null) {
            viewModel.clearMessages()
        }
    }

    Good4NestedScaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.admin_users_title),
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Filled.Menu, contentDescription = null)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    text = stringResource(Res.string.admin_users_edit_credit_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.userIdInput,
                    onValueChange = viewModel::onUserIdInputChange,
                    label = { Text(stringResource(Res.string.admin_users_user_id)) },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.emailInput,
                    onValueChange = viewModel::onEmailInputChange,
                    label = { Text(stringResource(Res.string.admin_users_email)) },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.weeklyCreditInput,
                    onValueChange = viewModel::onWeeklyCreditInputChange,
                    label = { Text(stringResource(Res.string.admin_users_override_dialog_placeholder)) },
                    singleLine = true
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = viewModel::applyOverride,
                    enabled = !state.isUpdating
                ) {
                    Text(stringResource(Res.string.admin_users_override_dialog_confirm))
                }
                if (state.isUpdating) {
                    CircularProgressIndicator(color = TextPrimary)
                }
            }
        }
    }
}

@Preview
@Composable
fun EditStudentCreditScreenPreview() {
    MaterialTheme {
        EditStudentCreditScreen()
    }
}
