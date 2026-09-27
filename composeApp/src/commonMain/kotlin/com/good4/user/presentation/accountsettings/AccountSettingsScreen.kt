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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.DarkMode
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.LocalThemeController
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
import config.LegalLinks
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.account_settings_account_management_title
import good4.composeapp.generated.resources.account_settings_business_name_label
import good4.composeapp.generated.resources.account_settings_name_label
import good4.composeapp.generated.resources.account_settings_phone_label
import good4.composeapp.generated.resources.account_settings_profile_section_title
import good4.composeapp.generated.resources.account_settings_reset_password_button
import good4.composeapp.generated.resources.account_settings_save_button
import good4.composeapp.generated.resources.account_settings_security_section_title
import good4.composeapp.generated.resources.account_settings_title
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
    academicSelectionPrompt: Boolean = false,
    onAcademicSelectionSaved: () -> Unit = {},
    onBackClick: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = state.errorMessage?.asString()
    val infoText = state.infoMessage?.asString()
    val themeController = LocalThemeController.current
    val uriHandler = LocalUriHandler.current

    val universityOptions = listOf("Akdeniz Üniversitesi")
    val facultyOptions = listOf(
        "İktisadi ve İdari Bilimler Fakültesi",
        "Mühendislik Fakültesi",
        "Mimarlık Fakültesi",
        "Uygulamalı Bilimler Fakültesi",
        "Eğitim Fakültesi",
        "Edebiyat Fakültesi",
        "Fen Fakültesi",
        "Ziraat Fakültesi",
        "Hemşirelik Fakültesi",
        "Sağlık Bilimleri Fakültesi",
        "Turizm Fakültesi",
        "İletişim Fakültesi",
        "Hukuk Fakültesi",
        "Spor Bilimleri Fakültesi",
        "Güzel Sanatlar Fakültesi",
        "Su Ürünleri Fakültesi",
        "İlahiyat Fakültesi",
        "Diş Hekimliği Fakültesi"
    )
    val iibfDepartmentOptions = listOf(
        "İşletme",
        "İktisat",
        "Ekonometri",
        "Maliye",
        "Çalışma Ekonomisi ve Endüstri İlişkileri",
        "Siyaset Bilimi ve Kamu Yönetimi",
        "Uluslararası İlişkiler"
    )
    val departmentOptions = when (state.faculty) {
        "Mühendislik Fakültesi" -> listOf(
            "Bilgisayar Mühendisliği",
            "Yapay Zeka ve Veri Mühendisliği",
            "Elektrik-Elektronik Mühendisliği",
            "Çevre Mühendisliği",
            "Gıda Mühendisliği",
            "İnşaat Mühendisliği",
            "Jeoloji Mühendisliği",
            "Makine Mühendisliği"
        )
        "Mimarlık Fakültesi" -> listOf("Mimarlık", "İç Mimarlık")
        "Fen Fakültesi" -> listOf(
            "Biyoloji",
            "Fizik",
            "Kimya",
            "Matematik",
            "Uzay Bilimleri ve Teknolojileri"
        )
        "Ziraat Fakültesi" -> listOf(
            "Bahçe Bitkileri",
            "Bitki Koruma",
            "Tarım Ekonomisi",
            "Tarım Makinaları ve Teknolojileri Mühendisliği",
            "Tarımsal Biyoteknoloji",
            "Tarımsal Yapılar ve Sulama",
            "Tarla Bitkileri",
            "Toprak Bilimi ve Bitki Besleme",
            "Zootekni"
        )
        "Uygulamalı Bilimler Fakültesi" -> listOf(
            "Finans ve Bankacılık",
            "Pazarlama",
            "Sigortacılık",
            "Uluslararası Ticaret ve Lojistik",
            "Yönetim Bilişim Sistemleri"
        )
        "Eğitim Fakültesi" -> listOf(
            "Fen Bilgisi Eğitimi",
            "İngilizce Öğretmenliği",
            "İlköğretim Matematik Eğitimi",
            "Okul Öncesi Eğitimi",
            "Özel Eğitim Öğretmenliği",
            "Rehberlik ve Psikolojik Danışmanlık",
            "Sınıf Öğretmenliği",
            "Sosyal Bilgiler Öğretmenliği",
            "Türkçe Öğretmenliği"
        )
        "Edebiyat Fakültesi" -> listOf(
            "Alman Dili ve Edebiyatı",
            "Arkeoloji",
            "Coğrafya",
            "Eski Yunan Dili ve Edebiyatı",
            "Latin Dili ve Edebiyatı",
            "Felsefe",
            "İngiliz Dili ve Edebiyatı (Örgün)",
            "İngiliz Dili ve Edebiyatı (İkinci Öğretim)",
            "Rus Dili ve Edebiyatı",
            "Psikoloji",
            "Sanat Tarihi",
            "Sosyoloji",
            "Tarih",
            "Türk Dili ve Edebiyatı (Örgün)",
            "Türk Dili ve Edebiyatı (İkinci Öğretim)"
        )
        "Hemşirelik Fakültesi" -> listOf("Hemşirelik")
        "Sağlık Bilimleri Fakültesi" -> listOf("Beslenme ve Diyetetik")
        "Turizm Fakültesi" -> listOf(
            "Turizm İşletmeciliği",
            "Tourism Management",
            "Gastronomi ve Mutfak Sanatları",
            "Turizm Rehberliği",
            "Rekreasyon Yönetimi",
            "Turizm ve Gastronomi Yönetimi Programları"
        )
        "İletişim Fakültesi" -> listOf(
            "Gazetecilik",
            "Halkla İlişkiler ve Tanıtım",
            "Radyo Televizyon ve Sinema",
            "Reklamcılık"
        )
        "Hukuk Fakültesi" -> listOf("Hukuk")
        "Su Ürünleri Fakültesi" -> listOf("Su Ürünleri Mühendisliği")
        "İlahiyat Fakültesi" -> listOf("İlahiyat")
        "Diş Hekimliği Fakültesi" -> listOf("Diş Hekimliği")
        "Spor Bilimleri Fakültesi" -> listOf(
            "Beden Eğitimi ve Spor",
            "Spor Yöneticiliği",
            "Rekreasyon",
            "Antrenörlük Eğitimi"
        )
        "Güzel Sanatlar Fakültesi" -> listOf(
            "Resim",
            "Heykel",
            "Grafik",
            "Seramik",
            "Müzik",
            "Fotoğraf",
            "Sinema-TV",
            "Geleneksel Türk Sanatları",
            "Tekstil ve Moda Tasarımı"
        )
        else -> iibfDepartmentOptions
    }
    val classYearOptions = if (state.faculty == "Diş Hekimliği Fakültesi") {
        listOf("1. Sınıf", "2. Sınıf", "3. Sınıf", "4. Sınıf", "5. Sınıf")
    } else {
        listOf("1. Sınıf", "2. Sınıf", "3. Sınıf", "4. Sınıf")
    }

    LaunchedEffect(mode) {
        viewModel.refresh(mode)
    }

    LaunchedEffect(state.profileSaveCount) {
        val hasAcademicSelection = listOf(state.faculty, state.major, state.classYear).all(String::isNotBlank)
        if (academicSelectionPrompt && state.profileSaveCount > 0 && hasAcademicSelection) {
            onAcademicSelectionSaved()
        }
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
        val focusManager = LocalFocusManager.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                // Tapping outside a field closes the keyboard, as on the register screen.
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus(force = true) })
                }
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ProfileSectionCard(verticalSpacing = 14.dp) {
                AccountSettingsSectionHeader(
                    icon = Icons.Outlined.DarkMode,
                    title = "Görünüm",
                    subtitle = "Uygulama temasını kişiselleştir."
                )
                HorizontalDivider(color = BorderMuted.copy(alpha = 0.45f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gece modu", color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text(
                            "Düşük ışıkta daha rahat bir görünüm kullan.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = themeController.isDark,
                        onCheckedChange = themeController.setDark
                    )
                }
            }

            ProfileSectionCard(verticalSpacing = 14.dp) {
                AccountSettingsSectionHeader(
                    icon = Icons.Filled.Person,
                    title = if (state.isCommunityManager) {
                        "Topluluk Bilgileri"
                    } else {
                        stringResource(Res.string.account_settings_profile_section_title)
                    },
                    subtitle = if (state.isCommunityManager) {
                        "Topluluğunun görünen bilgilerini düzenle."
                    } else {
                        "Üniversite ve bölüm bilgilerini güncel tut."
                    }
                )
                HorizontalDivider(color = BorderMuted.copy(alpha = 0.45f))

                if (state.isCommunityManager) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.communityName,
                        onValueChange = viewModel::onCommunityNameChange,
                        label = { Text("Topluluk adı") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        enabled = !state.isSaving && !state.isLoading
                    )
                } else if (mode == AccountSettingsMode.BUSINESS) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.businessName,
                        onValueChange = viewModel::onBusinessNameChange,
                        label = { Text(stringResource(Res.string.account_settings_business_name_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        enabled = !state.isSaving && !state.isLoading
                    )
                } else {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.fullName,
                        onValueChange = viewModel::onFullNameChange,
                        label = { Text(stringResource(Res.string.account_settings_name_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        enabled = !state.isSaving && !state.isLoading
                    )
                }

                if (mode == AccountSettingsMode.STUDENT) {
                    EditableSelectionField(
                        value = state.university,
                        label = stringResource(Res.string.profile_university_label),
                        placeholder = stringResource(Res.string.university_placeholder),
                        emptyText = stringResource(Res.string.university_dropdown_empty),
                        options = universityOptions,
                        enabled = !state.isSaving && !state.isLoading,
                        onValueSelect = viewModel::onUniversityChange
                    )

                    if (!state.isCommunityManager) {
                        if (academicSelectionPrompt) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = "Ders programını görmek için fakülte, bölüm ve sınıfını seçip kaydet.",
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        EditableSelectionField(
                            value = state.faculty,
                            label = "Fakülte",
                            placeholder = "Fakülte seçin",
                            options = facultyOptions,
                            enabled = !state.isSaving && !state.isLoading,
                            onValueSelect = viewModel::onFacultyChange
                        )

                        EditableSelectionField(
                            value = state.major,
                            label = "Bölüm",
                            placeholder = "Bölüm seçin",
                            options = departmentOptions,
                            enabled = !state.isSaving && !state.isLoading,
                            onValueSelect = viewModel::onMajorChange
                        )

                        EditableSelectionField(
                            value = state.classYear,
                            label = "Sınıf",
                            placeholder = "Sınıf seçin",
                            options = classYearOptions,
                            enabled = !state.isSaving && !state.isLoading,
                            onValueSelect = viewModel::onClassYearChange
                        )

                    }
                }

                if (state.showPhoneField && !state.isCommunityManager) {
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
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        enabled = !state.isSaving && !state.isLoading
                    )
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(StandardButtonHeight),
                    enabled = !state.isSaving && !state.isLoading,
                    onClick = { viewModel.saveChanges(mode) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
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


            // Google and Apple accounts have no password to reset.
            if (state.hasPasswordSignIn) ProfileSectionCard(verticalSpacing = 14.dp) {
                AccountSettingsSectionHeader(
                    icon = Icons.Filled.Email,
                    title = stringResource(Res.string.account_settings_security_section_title),
                    subtitle = "Şifre yenileme bağlantısını e-posta ile al."
                )
                HorizontalDivider(color = BorderMuted.copy(alpha = 0.45f))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(StandardButtonHeight),
                    enabled = state.canResendPasswordReset &&
                            !state.isSendingPasswordReset &&
                            state.email.isNotBlank() &&
                            !state.isLoading,
                    onClick = viewModel::sendPasswordResetEmail,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isSendingPasswordReset) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (state.passwordResetCooldownSeconds > 0) {
                            Text(
                                text = stringResource(
                                    Res.string.account_settings_reset_password_button
                                ) + " (${state.passwordResetCooldownSeconds}s)"
                            )
                        } else {
                            Text(text = stringResource(Res.string.account_settings_reset_password_button))
                        }
                    }
                }
            }

            ProfileSectionCard(verticalSpacing = 14.dp) {
                AccountSettingsSectionHeader(
                    icon = Icons.Filled.Info,
                    title = "Yasal belgeler",
                    subtitle = "Sözleşme ve gizlilik bilgilerini görüntüle."
                )
                HorizontalDivider(color = BorderMuted.copy(alpha = 0.45f))
                TextButton(onClick = { uriHandler.openUri(LegalLinks.TERMS) }) {
                    Text("Üyelik ve Kullanım Sözleşmesi")
                }
                TextButton(onClick = { uriHandler.openUri(LegalLinks.PRIVACY_POLICY) }) {
                    Text("Gizlilik Politikası")
                }
                TextButton(onClick = { uriHandler.openUri(LegalLinks.PRIVACY) }) {
                    Text("KVKK Aydınlatma Metni")
                }
            }

            ProfileSectionCard(verticalSpacing = 14.dp) {
                AccountSettingsSectionHeader(
                    icon = Icons.Filled.DeleteOutline,
                    title = stringResource(Res.string.account_settings_account_management_title),
                    subtitle = "Hesabını kalıcı olarak kapat.",
                    iconTint = MaterialTheme.colorScheme.error,
                    iconContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                )
                ProfileDeleteAccountButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = viewModel::showDeleteAccountDialog
                )
            }
        }
    }
}

@Composable
private fun AccountSettingsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconContainerColor: Color = PistachioGreen.copy(alpha = 0.24f)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconContainerColor, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
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
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Seçenekleri göster",
                    tint = TextSecondary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
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
                                        tint = MaterialTheme.colorScheme.primary
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
