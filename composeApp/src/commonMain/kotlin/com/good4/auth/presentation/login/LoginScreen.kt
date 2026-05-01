package com.good4.auth.presentation.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
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
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.StandardButtonHeight
import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import com.good4.core.util.singleClick
import com.good4.user.domain.UserRole
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.app_name
import good4.composeapp.generated.resources.app_tagline
import good4.composeapp.generated.resources.email
import good4.composeapp.generated.resources.email_placeholder
import good4.composeapp.generated.resources.error_resend_wait_seconds
import good4.composeapp.generated.resources.forgot_password
import good4.composeapp.generated.resources.login
import good4.composeapp.generated.resources.no_account
import good4.composeapp.generated.resources.or
import good4.composeapp.generated.resources.password
import good4.composeapp.generated.resources.password_placeholder
import good4.composeapp.generated.resources.password_visibility_hide
import good4.composeapp.generated.resources.password_visibility_show
import good4.composeapp.generated.resources.register
import good4.composeapp.generated.resources.splash_logo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.seconds

@Composable
fun LoginScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel,
    onLoginSuccess: (UserRole) -> Unit,
    onNavigateToRegisterOptions: () -> Unit,
    onNavigateToEmailVerification: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isLoginSuccess, state.userRole) {
        state.userRole?.let { role ->
            if (state.isLoginSuccess) {
                onLoginSuccess(role)
            }
        }
    }

    LaunchedEffect(state.isEmailVerificationRequired) {
        if (state.isEmailVerificationRequired) {
            onNavigateToEmailVerification()
        }
    }

    LoginScreen(
        modifier = modifier,
        state = state,
        onAction = { action ->
            when (action) {
                is LoginAction.OnStudentRegisterClick -> onNavigateToRegisterOptions()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    state: LoginState,
    onAction: (LoginAction) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val onLoginClick = remember { singleClick { onAction(LoginAction.OnLoginClick) } }
    val onForgotPasswordClick =
        remember { singleClick { onAction(LoginAction.OnForgotPasswordClick) } }

    Good4Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppBackground)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Image(
                    painter = painterResource(Res.drawable.splash_logo),
                    contentDescription = stringResource(Res.string.app_name),
                    modifier = Modifier
                        .size(96.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(Res.string.app_tagline),
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onAction(LoginAction.OnEmailChange(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            // iOS Password AutoFill primarily matches login identifiers as "username".
                            contentType = ContentType.Username
                        },
                    label = { Text(stringResource(Res.string.email)) },
                    placeholder = { Text(stringResource(Res.string.email_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        focusedLabelColor = TextPrimary,
                        cursorColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.password,
                    onValueChange = { onAction(LoginAction.OnPasswordChange(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Password },
                    label = { Text(stringResource(Res.string.password)) },
                    placeholder = { Text(stringResource(Res.string.password_placeholder)) },
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
                            onAction(LoginAction.OnLoginClick)
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { onAction(LoginAction.OnTogglePasswordVisibility) }) {
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        focusedLabelColor = TextPrimary,
                        cursorColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                TextButton(
                    onClick = onForgotPasswordClick,
                    modifier = Modifier.align(Alignment.End),
                    enabled = !state.isLoading && state.canSendPasswordReset
                ) {
                    Text(
                        text = stringResource(Res.string.forgot_password),
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }

                if (!state.canSendPasswordReset && state.passwordResetCooldownSeconds > 0) {
                    Text(
                        text = stringResource(
                            Res.string.error_resend_wait_seconds,
                            state.passwordResetCooldownSeconds
                        ),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                state.errorMessage?.let { error ->
                    LaunchedEffect(error) {
                        delay(3.seconds)
                        onAction(LoginAction.OnClearError)
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

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(StandardButtonHeight),
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextPrimary,
                        disabledContainerColor = TextPrimary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                            color = SurfaceDefault,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.login),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(TextSecondary.copy(alpha = 0.3f))
                    )
                    Text(
                        text = stringResource(Res.string.or),
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(TextSecondary.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.no_account),
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onAction(LoginAction.OnStudentRegisterClick) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(StandardButtonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.register),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            state = LoginState(),
            onAction = {}
        )
    }
}
