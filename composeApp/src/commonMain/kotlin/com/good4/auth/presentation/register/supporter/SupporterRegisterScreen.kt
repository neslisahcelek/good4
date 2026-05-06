package com.good4.auth.presentation.register.supporter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.auth.presentation.register.RegisterScreenContentContainer
import com.good4.auth.presentation.register.student.TermsCheckbox
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.StandardButtonHeight
import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import com.good4.core.util.singleClick
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.back
import good4.composeapp.generated.resources.email_required
import good4.composeapp.generated.resources.full_name
import good4.composeapp.generated.resources.password_confirm
import good4.composeapp.generated.resources.password_required
import good4.composeapp.generated.resources.password_visibility_hide
import good4.composeapp.generated.resources.password_visibility_show
import good4.composeapp.generated.resources.register
import good4.composeapp.generated.resources.required_fields
import good4.composeapp.generated.resources.supporter_registration
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SupporterRegisterScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: SupporterRegisterViewModel,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isRegisterSuccess) {
        if (state.isRegisterSuccess) onRegisterSuccess()
    }

    SupporterRegisterScreen(
        modifier = modifier,
        state = state,
        onAction = { action ->
            when (action) {
                is SupporterRegisterAction.OnBackClick -> onBackClick()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
fun SupporterRegisterScreen(
    modifier: Modifier = Modifier,
    state: SupporterRegisterState,
    onAction: (SupporterRegisterAction) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val onRegisterClick = remember { singleClick { onAction(SupporterRegisterAction.OnRegisterClick) } }

    Good4Scaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.supporter_registration),
                navigationIcon = {
                    IconButton(onClick = { onAction(SupporterRegisterAction.OnBackClick) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        RegisterScreenContentContainer(
            modifier = modifier,
            paddingValues = paddingValues
        ) {
            SupporterRegisterFormFields(
                state = state,
                onAction = onAction,
                onDone = {
                    focusManager.clearFocus()
                    onAction(SupporterRegisterAction.OnRegisterClick)
                }
            )

                Spacer(modifier = Modifier.height(16.dp))

                TermsCheckbox(
                    isChecked = state.isTermsAccepted,
                    onToggle = { onAction(SupporterRegisterAction.OnToggleTermsAccepted) }
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
                RegisterSubmitButton(
                    isLoading = state.isLoading,
                    onClick = onRegisterClick
                )

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

@Composable
private fun SupporterRegisterFormFields(
    modifier: Modifier = Modifier,
    state: SupporterRegisterState,
    onAction: (SupporterRegisterAction) -> Unit,
    onDone: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = state.fullName,
            onValueChange = { onAction(SupporterRegisterAction.OnFullNameChange(it)) },
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
            onValueChange = { onAction(SupporterRegisterAction.OnEmailChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.email_required)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next
            ),
            colors = textFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = { onAction(SupporterRegisterAction.OnPasswordChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.password_required)) },
            singleLine = true,
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            trailingIcon = { PasswordVisibilityToggle(state.isPasswordVisible, onAction) },
            colors = textFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = { onAction(SupporterRegisterAction.OnConfirmPasswordChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.password_confirm)) },
            singleLine = true,
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            colors = textFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun PasswordVisibilityToggle(
    isVisible: Boolean,
    onAction: (SupporterRegisterAction) -> Unit
) {
    IconButton(onClick = { onAction(SupporterRegisterAction.OnTogglePasswordVisibility) }) {
        Icon(
            imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = stringResource(
                if (isVisible) Res.string.password_visibility_hide else Res.string.password_visibility_show
            )
        )
    }
}

@Composable
private fun RegisterSubmitButton(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(StandardButtonHeight),
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepGreen,
            disabledContainerColor = DeepGreen.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                color = SurfaceDefault,
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
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TextPrimary,
    focusedLabelColor = TextPrimary,
    cursorColor = TextPrimary
)

@Preview
@Composable
fun SupporterRegisterScreenPreview() {
    MaterialTheme {
        SupporterRegisterScreen(
            state = SupporterRegisterState(),
            onAction = {}
        )
    }
}
