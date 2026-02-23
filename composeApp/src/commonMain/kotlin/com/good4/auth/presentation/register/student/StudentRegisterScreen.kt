package com.good4.auth.presentation.register.student

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.util.singleClick
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.back
import good4.composeapp.generated.resources.create_student_account
import good4.composeapp.generated.resources.education_level
import good4.composeapp.generated.resources.education_level_1
import good4.composeapp.generated.resources.education_level_2
import good4.composeapp.generated.resources.education_level_3
import good4.composeapp.generated.resources.education_level_4
import good4.composeapp.generated.resources.education_level_5
import good4.composeapp.generated.resources.education_level_6
import good4.composeapp.generated.resources.education_level_masters
import good4.composeapp.generated.resources.education_level_phd
import good4.composeapp.generated.resources.education_level_placeholder
import good4.composeapp.generated.resources.email_required
import good4.composeapp.generated.resources.full_name
import good4.composeapp.generated.resources.major
import good4.composeapp.generated.resources.password_confirm
import good4.composeapp.generated.resources.password_required
import good4.composeapp.generated.resources.password_visibility_hide
import good4.composeapp.generated.resources.password_visibility_show
import good4.composeapp.generated.resources.register
import good4.composeapp.generated.resources.required_fields
import good4.composeapp.generated.resources.student_emoji
import good4.composeapp.generated.resources.student_registration
import good4.composeapp.generated.resources.university
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun StudentRegisterScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: StudentRegisterViewModel,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isRegisterSuccess) {
        if (state.isRegisterSuccess) {
            onRegisterSuccess()
        }
    }

    val educationLevels = state.educationLevels.map { stringResource(it) }

    StudentRegisterScreen(
        modifier = modifier,
        state = state,
        educationLevels = educationLevels,
        onAction = { action ->
            when (action) {
                is StudentRegisterAction.OnBackClick -> onBackClick()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentRegisterScreen(
    modifier: Modifier = Modifier,
    state: StudentRegisterState,
    educationLevels: List<String>,
    onAction: (StudentRegisterAction) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val onRegisterClick = remember { singleClick { onAction(StudentRegisterAction.OnRegisterClick) } }

    Good4Scaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.student_registration),
                navigationIcon = {
                    IconButton(onClick = { onAction(StudentRegisterAction.OnBackClick) }) {
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
                    text = stringResource(Res.string.student_emoji),
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.create_student_account),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = state.fullName,
                    onValueChange = { onAction(StudentRegisterAction.OnFullNameChange(it)) },
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
                    onValueChange = { onAction(StudentRegisterAction.OnEmailChange(it)) },
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
                    value = state.university,
                    onValueChange = { onAction(StudentRegisterAction.OnUniversityChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.university)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.major,
                    onValueChange = { onAction(StudentRegisterAction.OnMajorChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.major)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                EducationLevelDropdown(
                    selectedValue = state.educationLevel,
                    onValueChange = { onAction(StudentRegisterAction.OnEducationLevelChange(it)) },
                    educationLevels = educationLevels,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.password,
                    onValueChange = { onAction(StudentRegisterAction.OnPasswordChange(it)) },
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
                    IconButton(onClick = { onAction(StudentRegisterAction.OnTogglePasswordVisibility) }) {
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
                    onValueChange = { onAction(StudentRegisterAction.OnConfirmPasswordChange(it)) },
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
                            onAction(StudentRegisterAction.OnRegisterClick)
                        }
                    ),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
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
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepGreen,
                        disabledContainerColor = DeepGreen.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EducationLevelDropdown(
    selectedValue: String,
    onValueChange: (String) -> Unit,
    educationLevels: List<String>,
    modifier: Modifier = Modifier
) {

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.education_level)) },
            placeholder = { Text(stringResource(Res.string.education_level_placeholder)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = textFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            educationLevels.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level) },
                    onClick = {
                        onValueChange(level)
                        expanded = false
                    }
                )
            }
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
fun StudentRegisterScreenPreview() {
    MaterialTheme {
        val educationLevels = listOf(
            stringResource(Res.string.education_level_1),
            stringResource(Res.string.education_level_2),
            stringResource(Res.string.education_level_3),
            stringResource(Res.string.education_level_4),
            stringResource(Res.string.education_level_5),
            stringResource(Res.string.education_level_6),
            stringResource(Res.string.education_level_masters),
            stringResource(Res.string.education_level_phd)
        )
        StudentRegisterScreen(
            state = StudentRegisterState(),
            educationLevels = educationLevels,
            onAction = {}
        )
    }
}




