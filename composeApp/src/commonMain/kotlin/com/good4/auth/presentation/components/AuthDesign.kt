package com.good4.auth.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.app_name
import good4.composeapp.generated.resources.splash_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal val AuthFieldShape = RoundedCornerShape(14.dp)
internal val AuthButtonHeight = 52.dp
private val AuthCardShape = RoundedCornerShape(24.dp)

/** Brand green of the current theme (lighter in dark mode for contrast). */
internal val AuthAccent: Color @Composable get() = MaterialTheme.colorScheme.primary
internal val AuthOnAccent: Color @Composable get() = MaterialTheme.colorScheme.onPrimary

@Composable
internal fun authTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AuthAccent,
    focusedLabelColor = AuthAccent,
    cursorColor = AuthAccent,
    unfocusedBorderColor = BorderMuted,
    focusedContainerColor = SurfaceDefault,
    unfocusedContainerColor = SurfaceDefault
)

/** Soft brand gradient with two blurred-looking circles behind auth screens. */
@Composable
internal fun AuthBackdrop(
    modifier: Modifier = Modifier,
    belowTopBar: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val accent = AuthAccent
    val lime = DeepGreen
    val background = AppBackground
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .drawBehind {
                // Under an opaque top bar the gradient must start from the plain
                // background, otherwise a hard edge shows below the bar.
                drawRect(
                    brush = if (belowTopBar) {
                        Brush.verticalGradient(
                            0f to background,
                            0.25f to accent.copy(alpha = 0.06f),
                            1f to background
                        )
                    } else {
                        Brush.verticalGradient(
                            0f to accent.copy(alpha = 0.16f),
                            0.45f to accent.copy(alpha = 0.04f),
                            1f to background
                        )
                    }
                )
                drawCircle(
                    color = accent.copy(alpha = if (belowTopBar) 0.07f else 0.10f),
                    radius = size.width * if (belowTopBar) 0.3f else 0.42f,
                    center = Offset(size.width * 0.98f, size.height * if (belowTopBar) 0.16f else 0.02f)
                )
                drawCircle(
                    color = lime.copy(alpha = 0.12f),
                    radius = size.width * 0.28f,
                    center = Offset(-size.width * 0.05f, size.height * 0.22f)
                )
            },
        content = content
    )
}

@Composable
internal fun AuthLogoBadge(modifier: Modifier = Modifier, size: Dp = 92.dp) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size * 0.3f),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Image(
            painter = painterResource(Res.drawable.splash_logo),
            contentDescription = stringResource(Res.string.app_name),
            modifier = Modifier.padding(size * 0.14f)
        )
    }
}

@Composable
internal fun AuthCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AuthCardShape,
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.5f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
internal fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(AuthButtonHeight),
        enabled = enabled && !loading,
        shape = AuthFieldShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthAccent,
            contentColor = AuthOnAccent,
            disabledContainerColor = AuthAccent.copy(alpha = if (loading) 0.85f else 0.4f),
            disabledContentColor = AuthOnAccent.copy(alpha = 0.8f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = AuthOnAccent, strokeWidth = 2.dp)
        } else {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun AuthSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leading: (@Composable () -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(AuthButtonHeight),
        enabled = enabled && !loading,
        shape = AuthFieldShape,
        border = BorderStroke(1.dp, BorderMuted),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceDefault,
            contentColor = TextPrimary,
            disabledContainerColor = if (loading) SurfaceDefault else Color.Transparent
        )
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        } else if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = if (loading) AuthSigningInText else text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

/** Shown on a sign-in button while the account is being signed in after the provider sheet closes. */
const val AuthSigningInText = "Giriş yapılıyor…"

/** Shared look for the platform Google buttons; the platforms only own the sign-in call. */
@Composable
fun GoogleButtonContent(text: String, enabled: Boolean, loading: Boolean = false, onClick: () -> Unit) {
    AuthSecondaryButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        loading = loading,
        leading = { GoogleGMark(Modifier.size(20.dp)) }
    )
}

/** Simplified four-colour Google "G" drawn with arcs, so no bitmap asset is needed. */
@Composable
internal fun GoogleGMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.22f
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        fun arc(color: Color, start: Float, sweep: Float) = drawArc(
            color = color,
            startAngle = start,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        arc(Color(0xFFEA4335), 200f, 118f)
        arc(Color(0xFFFBBC05), 140f, 60f)
        arc(Color(0xFF34A853), 45f, 95f)
        arc(Color(0xFF4285F4), 0f, 45f)
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(size.width / 2, size.height / 2 - stroke / 2),
            size = Size(size.width / 2 - inset + stroke / 2, stroke)
        )
    }
}

@Composable
internal fun AuthDivider(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(BorderMuted))
        Text(text, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp))
        Box(Modifier.weight(1f).height(1.dp).background(BorderMuted))
    }
}

/** Title block used at the top of the registration forms. */
@Composable
internal fun AuthFormHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AuthAccent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AuthAccent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary)
        }
    }
}

/** A labelled group of form fields on a card. */
@Composable
internal fun AuthSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Plain case on purpose: uppercase() is locale-invariant and would turn "i" into "I" in Turkish.
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        AuthCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

/** Large tappable option used on the "Kayıt Türü" screen. */
@Composable
internal fun AuthOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth().clip(AuthCardShape).clickable(onClick = onClick),
        shape = AuthCardShape,
        color = if (highlighted) AuthAccent.copy(alpha = 0.08f) else SurfaceDefault,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, if (highlighted) AuthAccent.copy(alpha = 0.55f) else BorderMuted),
        shadowElevation = if (highlighted) 0.dp else 1.dp
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(AuthAccent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AuthAccent, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(3.dp))
                Text(description, fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

@Composable
internal fun AuthFootnote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
internal fun AuthErrorBanner(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ErrorRed.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = ErrorRed, fontSize = 14.sp, lineHeight = 19.sp)
    }
}
