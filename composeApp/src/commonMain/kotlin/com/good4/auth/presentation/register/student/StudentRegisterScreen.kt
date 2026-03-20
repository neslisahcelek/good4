package com.good4.auth.presentation.register.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
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
import config.LegalLinks
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.back
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
import good4.composeapp.generated.resources.privacy_policy
import good4.composeapp.generated.resources.register
import good4.composeapp.generated.resources.required_fields
import good4.composeapp.generated.resources.student_registration
import good4.composeapp.generated.resources.terms_accept_middle
import good4.composeapp.generated.resources.terms_accept_suffix
import good4.composeapp.generated.resources.terms_of_service
import good4.composeapp.generated.resources.university
import good4.composeapp.generated.resources.university_dropdown_empty
import good4.composeapp.generated.resources.university_placeholder
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
        universities = state.universities,
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
    universities: List<String>,
    educationLevels: List<String>,
    onAction: (StudentRegisterAction) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val onRegisterClick =
        remember { singleClick { onAction(StudentRegisterAction.OnRegisterClick) } }

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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next
                    ),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                SelectionDropdown(
                    selectedValue = state.university,
                    onValueChange = { onAction(StudentRegisterAction.OnUniversityChange(it)) },
                    options = universities,
                    label = stringResource(Res.string.university),
                    placeholder = stringResource(Res.string.university_placeholder),
                    emptyText = stringResource(Res.string.university_dropdown_empty),
                    modifier = Modifier.fillMaxWidth()
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

                Spacer(modifier = Modifier.height(16.dp))

                TermsCheckbox(
                    isChecked = state.isTermsAccepted,
                    onToggle = { onAction(StudentRegisterAction.OnToggleTermsAccepted) }
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
    SelectionDropdown(
        selectedValue = selectedValue,
        onValueChange = onValueChange,
        options = educationLevels,
        label = stringResource(Res.string.education_level),
        placeholder = stringResource(Res.string.education_level_placeholder),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDropdown(
    selectedValue: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    label: String,
    placeholder: String,
    emptyText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val hasOptions = options.isNotEmpty()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            enabled = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIcon,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null
                )
            },
            colors = textFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(
                    enabled = true,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showSheet = true }
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceDefault
        ) {
            Text(
                text = label,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
            )

            if (hasOptions) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = options,
                        key = { it }
                    ) { option ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = option,
                                    color = TextPrimary,
                                    fontWeight = if (option == selectedValue) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            trailingContent = {
                                if (option == selectedValue) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = DeepGreen
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                onValueChange(option)
                                showSheet = false
                            }
                        )
                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.15f))
                    }
                }
            } else {
                Text(
                    text = emptyText ?: placeholder,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
internal fun TermsCheckbox(
    isChecked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val termsText = stringResource(Res.string.terms_of_service)
    val middle = stringResource(Res.string.terms_accept_middle)
    val privacyText = stringResource(Res.string.privacy_policy)
    val suffix = stringResource(Res.string.terms_accept_suffix)

    val annotatedText = buildAnnotatedString {
        withLink(
            LinkAnnotation.Url(
                url = LegalLinks.TERMS,
                styles = TextLinkStyles(
                    style = SpanStyle(color = DeepGreen, textDecoration = TextDecoration.Underline)
                )
            )
        ) {
            append(termsText)
        }
        append(middle)
        withLink(
            LinkAnnotation.Url(
                url = LegalLinks.PRIVACY,
                styles = TextLinkStyles(
                    style = SpanStyle(color = DeepGreen, textDecoration = TextDecoration.Underline)
                )
            )
        ) {
            append(privacyText)
        }
        append(suffix)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = DeepGreen)
        )
        Text(
            text = annotatedText,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp,
                color = TextSecondary
            ),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 8.dp)
        )
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
            universities = emptyList(),
            educationLevels = educationLevels,
            onAction = {}
        )
    }
}
