package com.good4.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.campuscloset.ClosetSectionHeading
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.UiText
import com.good4.core.presentation.components.ImagePickerLabels
import com.good4.core.presentation.components.ProductImagePicker
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** How the student appears to others: the photo, and whether their name is shown or masked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SocialProfileSheet(
    me: SocialMe,
    saving: Boolean,
    error: UiText?,
    onShowName: (Boolean) -> Unit,
    onPhoto: (ByteArray) -> Unit,
    onRemovePhoto: () -> Unit,
    onPhotoError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var picking by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss, containerColor = AppBackground,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ClosetSectionHeading(stringResource(Res.string.social_profile_title), Icons.Outlined.AddPhotoAlternate, stringResource(Res.string.social_profile_subtitle))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SocialAvatar(if (me.namesShown) me.shownName else me.maskedName, me.photoUrl, size = 80.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { picking = !picking }, enabled = !saving) {
                        Text(stringResource(if (me.photoUrl == null) Res.string.social_photo_choose else Res.string.social_photo_change), fontWeight = FontWeight.Medium)
                    }
                    if (me.photoUrl != null) {
                        TextButton(onClick = onRemovePhoto, enabled = !saving) { Text(stringResource(Res.string.social_photo_remove), color = ErrorRed) }
                    }
                }
            }
            if (picking) {
                ProductImagePicker(
                    currentRemoteImageUrl = "", pendingImageBytes = null, isUploading = saving,
                    onPendingImageChange = { bytes -> bytes?.let { onPhoto(it); picking = false } },
                    onError = { onPhotoError(it); picking = false },
                    labels = ImagePickerLabels(
                        stringResource(Res.string.social_photo_gallery), stringResource(Res.string.social_photo_camera),
                        stringResource(Res.string.social_photo_opening), stringResource(Res.string.social_photo_preparing),
                        stringResource(Res.string.social_photo_uploading), stringResource(Res.string.social_photo_selected),
                        stringResource(Res.string.social_photo_saved), stringResource(Res.string.social_photo_prepare_error),
                        stringResource(Res.string.social_photo_gallery_error), stringResource(Res.string.social_photo_camera_error)
                    )
                )
            }
            Text(stringResource(Res.string.social_photo_note), color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            SocialNameChoice(me.namesShown, me.shownName, me.maskedName, onChange = onShowName, enabled = !saving)
            error?.let { Text(it.asString(), color = ErrorRed, fontSize = 13.sp) }
        }
    }
}
