package com.good4.auth.presentation.login

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.good4.auth.presentation.components.AuthAccent
import com.good4.auth.presentation.components.AuthBackdrop
import com.good4.auth.presentation.components.AuthCard
import com.good4.auth.presentation.components.AuthDivider
import com.good4.auth.presentation.components.AuthFieldShape
import com.good4.auth.presentation.components.AuthFootnote
import com.good4.auth.presentation.components.AuthLogoBadge
import com.good4.auth.presentation.components.AuthPrimaryButton
import com.good4.auth.presentation.components.AuthSecondaryButton
import com.good4.auth.presentation.components.authTextFieldColors
import com.good4.auth.presentation.register.RegistrationLegalAcknowledgements
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorSnackbar
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend
import com.good4.core.util.keyboardPadding
import com.good4.core.util.singleClick
import com.good4.user.domain.UserRole
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.app_tagline
import good4.composeapp.generated.resources.dismiss
import good4.composeapp.generated.resources.email
import good4.composeapp.generated.resources.email_placeholder
import good4.composeapp.generated.resources.edu_email_helper
import good4.composeapp.generated.resources.edu_email_login
import good4.composeapp.generated.resources.edu_email_login_divider
import good4.composeapp.generated.resources.edu_email_register
import good4.composeapp.generated.resources.error_email_not_verified
import good4.composeapp.generated.resources.error_resend_wait_seconds
import good4.composeapp.generated.resources.forgot_password
import good4.composeapp.generated.resources.login
import good4.composeapp.generated.resources.login_welcome_title
import good4.composeapp.generated.resources.legal_registration_cancel
import good4.composeapp.generated.resources.legal_registration_continue
import good4.composeapp.generated.resources.legal_registration_intro
import good4.composeapp.generated.resources.no_account
import good4.composeapp.generated.resources.or
import good4.composeapp.generated.resources.password
import good4.composeapp.generated.resources.password_placeholder
import good4.composeapp.generated.resources.password_visibility_hide
import good4.composeapp.generated.resources.password_visibility_show
import good4.composeapp.generated.resources.register
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

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

@OptIn(ExperimentalMaterial3Api::class)
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
    val onCompleteLegalRegistrationClick =
        remember { singleClick { onAction(LoginAction.OnCompleteLegalRegistration) } }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEmailNotVerifiedError =
        (state.errorMessage as? com.good4.core.presentation.UiText.StringResourceId)?.id ==
            Res.string.error_email_not_verified
    val googleOnlyLogin = AppEnvironment.firebaseBackend == FirebaseBackend.V2
    var showEduLogin by rememberSaveable { mutableStateOf(false) }

    Good4Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        // The backdrop is drawn edge to edge so the gradient continues under the status bar.
        AuthBackdrop(
            modifier = Modifier
                // Scaffold padding is applied inside; consume it so the keyboard gap is not added twice.
                .consumeWindowInsets(paddingValues)
                .keyboardPadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus(force = true) })
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(36.dp))

                AuthLogoBadge()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.login_welcome_title),
                    fontSize = 21.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.app_tagline),
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                AuthCard {
                    if (state.isLegalAcknowledgementRequired) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(Res.string.legal_registration_intro),
                                color = TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            RegistrationLegalAcknowledgements(
                                userAgreementAccepted = state.isUserAgreementAccepted,
                                kvkkNoticeAcknowledged = state.isKvkkNoticeAcknowledged,
                                onUserAgreementToggle = {
                                    onAction(LoginAction.OnToggleUserAgreementAccepted)
                                },
                                onKvkkNoticeToggle = {
                                    onAction(LoginAction.OnToggleKvkkNoticeAcknowledged)
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            AuthPrimaryButton(
                                text = stringResource(Res.string.legal_registration_continue),
                                onClick = onCompleteLegalRegistrationClick,
                                loading = state.isLoading
                            )
                            TextButton(
                                onClick = { onAction(LoginAction.OnCancelLegalRegistration) },
                                enabled = !state.isLoading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(Res.string.legal_registration_cancel),
                                    color = TextSecondary
                                )
                            }
                        }
                    } else if (googleOnlyLogin) {
                        GoogleSignInButton(
                            enabled = !state.isLoading,
                            loading = state.isLoading && state.federatedSignIn == FederatedSignIn.Google,
                            onToken = { token, accessToken -> onAction(LoginAction.OnGoogleToken(token, accessToken)) },
                            onError = { onAction(LoginAction.OnGoogleError(it)) }
                        )
                        AppleSignInButton(
                            enabled = !state.isLoading,
                            loading = state.isLoading && state.federatedSignIn == FederatedSignIn.Apple,
                            onCredential = { idToken, rawNonce ->
                                onAction(LoginAction.OnAppleCredential(idToken, rawNonce))
                            },
                            onError = { onAction(LoginAction.OnAppleError(it)) }
                        )

                        // V2 signs in with Google/Apple only; community managers use Google too and
                        // are assigned from the web panel. The edu e-mail form stays behind the flag.
                        if (config.ReleaseFeatures.eduEmailAuth) {
                            Spacer(modifier = Modifier.height(12.dp))

                            AnimatedVisibility(visible = !showEduLogin) {
                                AuthSecondaryButton(
                                    text = stringResource(Res.string.edu_email_login),
                                    onClick = { showEduLogin = true },
                                    enabled = !state.isLoading,
                                    leading = {
                                        Icon(
                                            imageVector = Icons.Outlined.School,
                                            contentDescription = null,
                                            tint = AuthAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }

                            AnimatedVisibility(visible = showEduLogin) {
                                Column {
                                    AuthDivider(
                                        text = stringResource(Res.string.edu_email_login_divider),
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    EmailPasswordForm(
                                        state = state,
                                        onAction = onAction,
                                        onLoginClick = onLoginClick,
                                        onForgotPasswordClick = onForgotPasswordClick
                                    )
                                }
                            }
                        }
                    } else {
                        EmailPasswordForm(
                            state = state,
                            onAction = onAction,
                            onLoginClick = onLoginClick,
                            onForgotPasswordClick = onForgotPasswordClick
                        )

                        AuthDivider(
                            text = stringResource(Res.string.or),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        GoogleSignInButton(
                            enabled = !state.isLoading,
                            loading = state.isLoading && state.federatedSignIn == FederatedSignIn.Google,
                            onToken = { token, accessToken -> onAction(LoginAction.OnGoogleToken(token, accessToken)) },
                            onError = { onAction(LoginAction.OnGoogleError(it)) }
                        )
                        AppleSignInButton(
                            enabled = !state.isLoading,
                            loading = state.isLoading && state.federatedSignIn == FederatedSignIn.Apple,
                            onCredential = { idToken, rawNonce ->
                                onAction(LoginAction.OnAppleCredential(idToken, rawNonce))
                            },
                            onError = { onAction(LoginAction.OnAppleError(it)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val showRegisterLink = !googleOnlyLogin || config.ReleaseFeatures.eduEmailAuth
                if (!state.isLegalAcknowledgementRequired && showRegisterLink) Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.no_account),
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    TextButton(
                        onClick = { onAction(LoginAction.OnStudentRegisterClick) },
                        enabled = !state.isLoading
                    ) {
                        Text(
                            text = stringResource(
                                if (googleOnlyLogin) Res.string.edu_email_register else Res.string.register
                            ),
                            color = AuthAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (googleOnlyLogin && config.ReleaseFeatures.eduEmailAuth && !state.isLegalAcknowledgementRequired) {
                    AuthFootnote(
                        text = stringResource(Res.string.edu_email_helper),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            ErrorSnackbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                errorMessage = state.errorMessage?.takeUnless { isEmailNotVerifiedError },
                onDismiss = { onAction(LoginAction.OnClearError) },
                addTopSafeArea = false
            )

            if (isEmailNotVerifiedError) {
                ModalBottomSheet(
                    onDismissRequest = { onAction(LoginAction.OnClearError) },
                    sheetState = bottomSheetState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.errorMessage?.asString().orEmpty(),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onAction(LoginAction.OnClearError) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.dismiss),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SurfaceDefault
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailPasswordForm(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(modifier = Modifier.fillMaxWidth()) {
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
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = TextSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next
            ),
            colors = authTextFieldColors(),
            shape = AuthFieldShape
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = { onAction(LoginAction.OnPasswordChange(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Password },
            label = { Text(stringResource(Res.string.password)) },
            placeholder = { Text(stringResource(Res.string.password_placeholder)) },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextSecondary) },
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
                        contentDescription = contentDescription,
                        tint = TextSecondary
                    )
                }
            },
            colors = authTextFieldColors(),
            shape = AuthFieldShape
        )

        TextButton(
            onClick = onForgotPasswordClick,
            modifier = Modifier.align(Alignment.End),
            enabled = !state.isLoading && state.canSendPasswordReset
        ) {
            Text(
                text = stringResource(Res.string.forgot_password),
                color = AuthAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
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

        state.infoMessage?.let { info ->
            Text(
                text = info.asString(),
                color = AuthAccent,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AuthPrimaryButton(
            text = stringResource(Res.string.login),
            onClick = onLoginClick,
            loading = state.isLoading
        )
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
