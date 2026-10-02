package com.good4.schedule.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.Good4Theme
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.StandardButtonHeight
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.schedule_close_selection
import good4.composeapp.generated.resources.schedule_go_to_settings
import good4.composeapp.generated.resources.schedule_missing_all
import good4.composeapp.generated.resources.schedule_missing_class
import good4.composeapp.generated.resources.schedule_missing_department
import good4.composeapp.generated.resources.schedule_missing_department_class
import good4.composeapp.generated.resources.schedule_missing_faculty
import good4.composeapp.generated.resources.schedule_missing_faculty_class
import good4.composeapp.generated.resources.schedule_missing_faculty_department
import good4.composeapp.generated.resources.schedule_selection_return_hint
import good4.composeapp.generated.resources.schedule_selection_title
import good4.composeapp.generated.resources.schedule_select_action
import good4.composeapp.generated.resources.schedule_skip_selection
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AcademicSelectionBottomSheet(
    missingFields: List<AcademicProfileField>,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDefault,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        AcademicSelectionPromptContent(
            missingFields = missingFields,
            onOpenSettings = onOpenSettings,
            onDismiss = onDismiss,
            modifier = Modifier.verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(top = 4.dp, bottom = 24.dp)
        )
    }
}

@Composable
internal fun AcademicSelectionPromptContent(
    missingFields: List<AcademicProfileField>,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(Res.string.schedule_close_selection),
                    tint = TextSecondary
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.schedule_selection_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(academicSelectionMessageResource(missingFields)),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AcademicSelectionButton(
                label = stringResource(Res.string.schedule_go_to_settings),
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = stringResource(Res.string.schedule_selection_return_hint),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun AcademicProfileSelectionButton(onSelect: () -> Unit, modifier: Modifier = Modifier) {
    AcademicSelectionButton(
        label = stringResource(Res.string.schedule_select_action),
        onClick = onSelect,
        modifier = modifier
    )
}

private enum class AcademicSelectionButtonVariant { PRIMARY, TERTIARY }

@Composable
private fun AcademicSelectionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AcademicSelectionButtonVariant = AcademicSelectionButtonVariant.PRIMARY
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) .97f else 1f, label = "academicSelectionButtonScale")
    val colors = when (variant) {
        AcademicSelectionButtonVariant.PRIMARY -> ButtonDefaults.buttonColors()
        AcademicSelectionButtonVariant.TERTIARY -> ButtonDefaults.buttonColors(
            containerColor = SurfaceMuted,
            contentColor = TextPrimary
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier.height(StandardButtonHeight).graphicsLayer { scaleX = scale; scaleY = scale },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        colors = colors
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
    }
}

internal fun academicSelectionMessageResource(missingFields: List<AcademicProfileField>) =
    when (missingFields.toSet()) {
        setOf(AcademicProfileField.FACULTY) -> Res.string.schedule_missing_faculty
        setOf(AcademicProfileField.DEPARTMENT) -> Res.string.schedule_missing_department
        setOf(AcademicProfileField.CLASS_YEAR) -> Res.string.schedule_missing_class
        setOf(AcademicProfileField.FACULTY, AcademicProfileField.DEPARTMENT) -> Res.string.schedule_missing_faculty_department
        setOf(AcademicProfileField.FACULTY, AcademicProfileField.CLASS_YEAR) -> Res.string.schedule_missing_faculty_class
        setOf(AcademicProfileField.DEPARTMENT, AcademicProfileField.CLASS_YEAR) -> Res.string.schedule_missing_department_class
        else -> Res.string.schedule_missing_all
    }

@Preview
@Composable
private fun AcademicSelectionBottomSheetPreview() {
    Good4Theme {
        AcademicSelectionBottomSheet(
            missingFields = listOf(AcademicProfileField.CLASS_YEAR),
            onOpenSettings = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun AcademicSelectionPromptPreview() {
    Good4Theme {
        AcademicSelectionPromptContent(
            missingFields = AcademicProfileField.entries,
            onOpenSettings = {},
            onDismiss = {},
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Preview
@Composable
private fun AcademicProfileSelectionButtonPreview() {
    Good4Theme {
        AcademicProfileSelectionButton(onSelect = {}, modifier = Modifier.fillMaxWidth().padding(16.dp))
    }
}

@Preview
@Composable
private fun AcademicSelectionPromptDarkPreview() {
    Good4Theme(darkTheme = true) {
        Surface(color = SurfaceDefault) {
            AcademicSelectionPromptContent(
                missingFields = listOf(AcademicProfileField.CLASS_YEAR),
                onOpenSettings = {},
                onDismiss = {},
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}
