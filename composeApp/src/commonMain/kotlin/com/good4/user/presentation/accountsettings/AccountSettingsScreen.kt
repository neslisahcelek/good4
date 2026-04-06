package com.good4.user.presentation.accountsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.DeleteAccountConfirmDialog
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.ProfileDeleteAccountButton
import com.good4.core.presentation.components.ProfileSectionCard
import com.good4.core.presentation.components.StandardButtonHeight
import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.account_settings_account_management_title
import good4.composeapp.generated.resources.account_settings_business_name_label
import good4.composeapp.generated.resources.account_settings_name_label
import good4.composeapp.generated.resources.account_settings_phone_label
import good4.composeapp.generated.resources.account_settings_profile_section_title
import good4.composeapp.generated.resources.account_settings_reset_password_button
import good4.composeapp.generated.resources.account_settings_reset_password_sent_hint
import good4.composeapp.generated.resources.account_settings_save_button
import good4.composeapp.generated.resources.account_settings_security_section_title
import good4.composeapp.generated.resources.account_settings_title
import good4.composeapp.generated.resources.education_level_1
import good4.composeapp.generated.resources.education_level_2
import good4.composeapp.generated.resources.education_level_3
import good4.composeapp.generated.resources.education_level_4
import good4.composeapp.generated.resources.education_level_5
import good4.composeapp.generated.resources.education_level_6
import good4.composeapp.generated.resources.education_level_masters
import good4.composeapp.generated.resources.education_level_phd
import good4.composeapp.generated.resources.education_level_placeholder
import good4.composeapp.generated.resources.profile_education_level_label
import good4.composeapp.generated.resources.profile_major_label
import good4.composeapp.generated.resources.profile_university_label
import good4.composeapp.generated.resources.university_dropdown_empty
import good4.composeapp.generated.resources.university_placeholder
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    mode: AccountSettingsMode,
    modifier: Modifier = Modifier,
    viewModel: AccountSettingsViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = state.errorMessage?.asString()
    val infoText = state.infoMessage?.asString()

    val educationLevelOptions = listOf(
        stringResource(Res.string.education_level_1),
        stringResource(Res.string.education_level_2),
        stringResource(Res.string.education_level_3),
        stringResource(Res.string.education_level_4),
        stringResource(Res.string.education_level_5),
        stringResource(Res.string.education_level_6),
        stringResource(Res.string.education_level_masters),
        stringResource(Res.string.education_level_phd)
    )

    LaunchedEffect(mode) {
        viewModel.refresh(mode)
    }

    LaunchedEffect(errorText, infoText) {
        val message = errorText ?: infoText
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.isLoggedOut, state.isAccountDeleted) {
        if (state.isLoggedOut || state.isAccountDeleted) {
            onLogout()
        }
    }

    if (state.isDeleteDialogVisible) {
        DeleteAccountConfirmDialog(
            isDeleting = state.isDeleting,
            onConfirm = viewModel::deleteAccount,
            onDismiss = viewModel::hideDeleteAccountDialog
        )
    }

    Good4NestedScaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.account_settings_title),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ProfileSectionCard {
                Text(
                    text = stringResource(Res.string.account_settings_profile_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (mode == AccountSettingsMode.BUSINESS) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.businessName,
                        onValueChange = viewModel::onBusinessNameChange,
                        label = { Text(stringResource(Res.string.account_settings_business_name_label)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        enabled = !state.isSaving && !state.isLoading
                    )
                } else {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.fullName,
                        onValueChange = viewModel::onFullNameChange,
                        label = { Text(stringResource(Res.string.account_settings_name_label)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        enabled = !state.isSaving && !state.isLoading
                    )
                }

                if (mode == AccountSettingsMode.STUDENT) {
                    EditableSelectionField(
                        value = state.university,
                        label = stringResource(Res.string.profile_university_label),
                        placeholder = stringResource(Res.string.university_placeholder),
                        emptyText = stringResource(Res.string.university_dropdown_empty),
                        options = state.universities,
                        enabled = !state.isSaving && !state.isLoading,
                        onValueSelect = viewModel::onUniversityChange
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.major,
                        onValueChange = viewModel::onMajorChange,
                        label = { Text(stringResource(Res.string.profile_major_label)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        enabled = !state.isSaving && !state.isLoading
                    )

                    EditableSelectionField(
                        value = state.educationLevel,
                        label = stringResource(Res.string.profile_education_level_label),
                        placeholder = stringResource(Res.string.education_level_placeholder),
                        options = educationLevelOptions,
                        enabled = !state.isSaving && !state.isLoading,
                        onValueSelect = viewModel::onEducationLevelChange
                    )
                }

                if (state.showPhoneField) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = if (mode == AccountSettingsMode.BUSINESS) {
                            state.businessPhone
                        } else {
                            state.phoneNumber
                        },
                        onValueChange = if (mode == AccountSettingsMode.BUSINESS) {
                            viewModel::onBusinessPhoneChange
                        } else {
                            viewModel::onPhoneNumberChange
                        },
                        label = { Text(stringResource(Res.string.account_settings_phone_label)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        enabled = !state.isSaving && !state.isLoading
                    )
                }

                Button(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(min = 220.dp)
                        .height(StandardButtonHeight),
                    enabled = !state.isSaving && !state.isLoading,
                    onClick = { viewModel.saveChanges(mode) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = SurfaceDefault
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                            strokeWidth = 2.dp,
                            color = SurfaceDefault
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(Res.string.account_settings_save_button))
                    }
                }
            }

            ProfileSectionCard {
                Text(
                    text = stringResource(Res.string.account_settings_security_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(min = 220.dp)
                        .height(StandardButtonHeight),
                    enabled = !state.isSendingPasswordReset &&
                            state.email.isNotBlank() &&
                            !state.isLoading,
                    onClick = viewModel::sendPasswordResetEmail,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = SurfaceDefault
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isSendingPasswordReset) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                            strokeWidth = 2.dp,
                            color = SurfaceDefault
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(Res.string.account_settings_reset_password_button))
                    }
                }

                if (state.isPasswordResetEmailSent) {
                    Text(
                        text = stringResource(Res.string.account_settings_reset_password_sent_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            ProfileSectionCard(verticalSpacing = 6.dp) {
                Text(
                    text = stringResource(Res.string.account_settings_account_management_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                ProfileDeleteAccountButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = viewModel::showDeleteAccountDialog
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableSelectionField(
    value: String,
    label: String,
    placeholder: String,
    options: List<String>,
    enabled: Boolean,
    onValueSelect: (String) -> Unit,
    emptyText: String? = null
) {
    var isSheetVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null
                    )
                }
            },
            singleLine = true
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isSheetVisible = true }
        )
    }

    if (isSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { isSheetVisible = false },
            sheetState = sheetState,
            containerColor = SurfaceDefault
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )

            if (options.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(items = options, key = { it }) { option ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = option,
                                    fontWeight = if (option == value) FontWeight.SemiBold else FontWeight.Normal,
                                    color = TextPrimary
                                )
                            },
                            trailingContent = {
                                if (option == value) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = PrimaryGreen
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                onValueSelect(option)
                                isSheetVisible = false
                            }
                        )
                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.15f))
                    }
                }
            } else {
                Text(
                    text = emptyText ?: placeholder,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun AccountSettingsScreenPreview() {
    MaterialTheme {
        AccountSettingsScreen(
            mode = AccountSettingsMode.STUDENT,
            onBackClick = {},
            onLogout = {}
        )
    }
}
