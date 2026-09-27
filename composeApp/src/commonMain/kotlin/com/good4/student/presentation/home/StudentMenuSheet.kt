package com.good4.student.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.LaunchedEffect
import com.good4.student.home.HomeShortcut
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.SurfaceCanvasWarm
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.PrimaryGreen as BrandGreen
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.feedback.FeedbackUiState
import com.good4.feedback.FeedbackViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

private data class StudentMenuItem(
    val title: String,
    val icon: ImageVector,
    val accent: Color,
    val opensHomeEditor: Boolean = false,
    val opensNumbers: Boolean = false,
    val opensFeedback: Boolean = false,
    val url: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudentMenuSheet(
    onDismiss: () -> Unit,
    onEditHome: () -> Unit = {},
    initialShortcut: HomeShortcut? = null,
    feedbackViewModel: FeedbackViewModel = koinViewModel()
) {
    var numbersOpen by rememberSaveable(initialShortcut) { mutableStateOf(initialShortcut == HomeShortcut.PHONE_NUMBERS) }
    var feedbackOpen by rememberSaveable(initialShortcut) { mutableStateOf(initialShortcut == HomeShortcut.FEEDBACK) }
    LaunchedEffect(initialShortcut) {
        if (initialShortcut == HomeShortcut.FEEDBACK) feedbackViewModel.startNew()
    }
    val feedbackState by feedbackViewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val menuItems = listOf(HomeShortcut.TOP_UP, HomeShortcut.TENNIS, HomeShortcut.PHONE_NUMBERS, HomeShortcut.FEEDBACK).map { shortcut ->
        val appearance = shortcut.appearance()
        StudentMenuItem(
            title = appearance.title,
            icon = appearance.icon,
            accent = if (appearance.accent == BrandGreen) MaterialTheme.colorScheme.primary else appearance.accent,
            url = shortcut.externalUrl,
            opensNumbers = shortcut == HomeShortcut.PHONE_NUMBERS,
            opensFeedback = shortcut == HomeShortcut.FEEDBACK
        )
    } + StudentMenuItem("Sayfanı Düzenle", Icons.Outlined.Edit, MaterialTheme.colorScheme.primary, opensHomeEditor = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceCanvasWarm,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .padding(top = 10.dp)
        ) {
            if (feedbackOpen) {
                MenuSheetHeader(
                    title = "Geri Bildirim",
                    subtitle = "Görüşlerini doğrudan Good4 ekibiyle paylaş",
                    onBack = { feedbackOpen = false }
                )
                FeedbackForm(
                    state = feedbackState,
                    onSubjectChange = feedbackViewModel::onSubjectChange,
                    onMessageChange = feedbackViewModel::onMessageChange,
                    onSubmit = feedbackViewModel::submit,
                    onDone = {
                        feedbackOpen = false
                        feedbackViewModel.startNew()
                    }
                )
            } else if (numbersOpen) {
                MenuSheetHeader(
                    title = "Numaralar",
                    subtitle = "Dokunarak doğrudan arayabilirsin",
                    onBack = { numbersOpen = false }
                )
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PhoneContactCard(
                        title = "Kampüs Güvenlik İhbar Hattı",
                        number = "0242 310 22 22",
                        accent = Color(0xFFF2A66F),
                        onClick = { uriHandler.openUri("tel:+902423102222") }
                    )
                    PhoneContactCard(
                        title = "SKS / Mediko-Sosyal",
                        number = "0242 310 21 51",
                        accent = Color(0xFF68CCDC),
                        onClick = { uriHandler.openUri("tel:+902423102151") }
                    )
                }
            } else {
                MenuSheetHeader(
                    title = "Menü",
                    subtitle = "Kampüs araçları ve hızlı bağlantılar",
                    onClose = onDismiss
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    menuItems.forEach { item ->
                        val onItemClick: (() -> Unit)? = when {
                            item.opensHomeEditor -> ({ onDismiss(); onEditHome() })
                            item.opensNumbers -> ({ numbersOpen = true })
                            item.opensFeedback -> ({
                                feedbackViewModel.startNew()
                                feedbackOpen = true
                            })
                            item.url != null -> {
                                val url = item.url
                                ({
                                    onDismiss()
                                    uriHandler.openUri(url)
                                })
                            }
                            else -> null
                        }
                        StudentMenuCard(
                            item = item,
                            onClick = onItemClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackForm(
    state: FeedbackUiState,
    onSubjectChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.isSubmitted) {
            Spacer(Modifier.height(22.dp))
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally).size(52.dp)
            )
            Text(
                text = "Teşekkürler",
                modifier = Modifier.fillMaxWidth(),
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Geri bildirimin Good4 yönetim paneline ulaştı.",
                modifier = Modifier.fillMaxWidth(),
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tamam", fontWeight = FontWeight.SemiBold)
            }
            return@Column
        }

        Text(
            text = "Konu",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = state.subject,
            onValueChange = onSubjectChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Örn. Ana sayfa önerisi") },
            supportingText = { Text("${state.subject.length}/120") },
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = feedbackFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )
        Text(
            text = "Geri bildirimin",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Deneyimini, önerini veya karşılaştığın sorunu yazabilirsin.") },
            supportingText = { Text("${state.message.length}/2000") },
            minLines = 5,
            maxLines = 8,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            colors = feedbackFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )
        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = state.canSubmit,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = if (state.isSubmitting) 0.85f else 0.42f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Gönder", fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            text = "Hesap bilgin yalnızca geri bildirimi takip edebilmek için kaydedilir.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun feedbackFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = BorderMuted
)

@Composable
private fun MenuSheetHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Menüye dön",
                    tint = TextPrimary
                )
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary
            )
        }
        if (onClose != null) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Menüyü kapat", tint = TextSecondary)
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
private fun StudentMenuCard(
    item: StudentMenuItem,
    onClick: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
        ) {
            Text(
                text = item.title,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp, end = 54.dp),
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(58.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 3.dp, y = 3.dp)
                        .size(48.dp)
                        .graphicsLayer(rotationZ = -5f)
                        .background(
                            color = TextPrimary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp)
                        )
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .graphicsLayer(rotationZ = -5f),
                    shape = RoundedCornerShape(14.dp),
                    color = item.accent.copy(alpha = 0.24f),
                    border = BorderStroke(1.dp, item.accent.copy(alpha = 0.88f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = item.accent,
                            modifier = Modifier.size(27.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneContactCard(
    title: String,
    number: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(15.dp),
                color = accent.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Text(number, fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            Text("Ara", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
