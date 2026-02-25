package com.good4.auth.presentation.register.business

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.auth.presentation.register.student.TermsCheckbox
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.address
import good4.composeapp.generated.resources.back
import good4.composeapp.generated.resources.business_emoji
import good4.composeapp.generated.resources.business_information
import good4.composeapp.generated.resources.business_name
import good4.composeapp.generated.resources.business_phone
import good4.composeapp.generated.resources.business_registration
import good4.composeapp.generated.resources.city
import good4.composeapp.generated.resources.create_business_account
import good4.composeapp.generated.resources.district
import good4.composeapp.generated.resources.email_required
import good4.composeapp.generated.resources.full_name
import good4.composeapp.generated.resources.password_confirm
import good4.composeapp.generated.resources.password_required
import good4.composeapp.generated.resources.password_visibility_hide
import good4.composeapp.generated.resources.password_visibility_show
import good4.composeapp.generated.resources.personal_information
import good4.composeapp.generated.resources.phone
import good4.composeapp.generated.resources.register
import good4.composeapp.generated.resources.required_fields
import good4.composeapp.generated.resources.security
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BusinessRegisterScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: BusinessRegisterViewModel,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isRegisterSuccess) {
        if (state.isRegisterSuccess) {
            onRegisterSuccess()
        }
    }

    BusinessRegisterScreen(
        modifier = modifier,
        state = state,
        onAction = { action ->
            when (action) {
                is BusinessRegisterAction.OnBackClick -> onBackClick()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessRegisterScreen(
    modifier: Modifier = Modifier,
    state: BusinessRegisterState,
    onAction: (BusinessRegisterAction) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Good4Scaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.business_registration),
                navigationIcon = {
                    IconButton(onClick = { onAction(BusinessRegisterAction.OnBackClick) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.business_emoji),
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.create_business_account),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle(stringResource(Res.string.personal_information))

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.fullName,
                    onValueChange = { onAction(BusinessRegisterAction.OnFullNameChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.full_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onAction(BusinessRegisterAction.OnEmailChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.email_required)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.phoneNumber,
                    onValueChange = { onAction(BusinessRegisterAction.OnPhoneNumberChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.phone)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(color = BorderMuted)

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle(stringResource(Res.string.business_information))

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.businessName,
                    onValueChange = { onAction(BusinessRegisterAction.OnBusinessNameChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.business_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.businessPhone,
                    onValueChange = { onAction(BusinessRegisterAction.OnBusinessPhoneChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.business_phone)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.address,
                    onValueChange = { onAction(BusinessRegisterAction.OnAddressChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.address)) },
                    singleLine = false,
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.city,
                    onValueChange = { onAction(BusinessRegisterAction.OnCityChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.city)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.district,
                    onValueChange = { onAction(BusinessRegisterAction.OnDistrictChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.district)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(color = BorderMuted)

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle(stringResource(Res.string.security))

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.password,
                    onValueChange = { onAction(BusinessRegisterAction.OnPasswordChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.password_required)) },
                    singleLine = true,
                    visualTransformation = if (state.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    trailingIcon = {
                        IconButton(onClick = { onAction(BusinessRegisterAction.OnTogglePasswordVisibility) }) {
                            val contentDescription = if (state.isPasswordVisible) {
                                stringResource(Res.string.password_visibility_hide)
                            } else {
                                stringResource(Res.string.password_visibility_show)
                            }
                            Icon(
                                imageVector = if (state.isPasswordVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = contentDescription
                            )
                        }
                    },
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = { onAction(BusinessRegisterAction.OnConfirmPasswordChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.password_confirm)) },
                    singleLine = true,
                    visualTransformation = if (state.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onAction(BusinessRegisterAction.OnRegisterClick)
                        }
                    ),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                TermsCheckbox(
                    isChecked = state.isTermsAccepted,
                    onToggle = { onAction(BusinessRegisterAction.OnToggleTermsAccepted) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                state.errorMessage?.let { error ->
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

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onAction(BusinessRegisterAction.OnRegisterClick) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PistachioGreen,
                        contentColor = TextPrimary,
                        disabledContainerColor = PistachioGreen.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.register),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.required_fields),
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TextPrimary,
    focusedLabelColor = TextPrimary,
    cursorColor = TextPrimary
)

@Preview
@Composable
fun BusinessRegisterScreenPreview() {
    MaterialTheme {
        BusinessRegisterScreen(
            state = BusinessRegisterState(),
            onAction = {}
        )
    }
}




