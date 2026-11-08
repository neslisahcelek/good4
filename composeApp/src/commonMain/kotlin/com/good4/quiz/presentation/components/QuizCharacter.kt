package com.good4.quiz.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import good4.composeapp.generated.resources.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.resources.StringResource

/**
 * Good4 live quiz characters. Every character is drawn from the same anatomy (big head, round
 * eyes, small body, ink outline, soft shading) so avatars and large previews stay consistent.
 * Drawing happens in a 100 × 128 unit box; [CharacterFraming.HEAD] crops to the head area.
 */
enum class QuizCharacter(val id: String, val nameRes: StringResource) {
    FOX("fox", Res.string.quiz_character_fox),
    PANDA("panda", Res.string.quiz_character_panda),
    ROBOT("robot", Res.string.quiz_character_robot),
    OWL("owl", Res.string.quiz_character_owl),
    CAT("cat", Res.string.quiz_character_cat),
    FROG("frog", Res.string.quiz_character_frog),
    DRAGON("dragon", Res.string.quiz_character_dragon),
    KOALA("koala", Res.string.quiz_character_koala),
    ALIEN("alien", Res.string.quiz_character_alien),
    MONSTER("monster", Res.string.quiz_character_monster);

    companion object {
        fun fromId(id: String?): QuizCharacter? = entries.firstOrNull { it.id == id }
    }
}

enum class QuizAccessory(val id: String, val nameRes: StringResource) {
    NONE("none", Res.string.quiz_accessory_none),
    CROWN("crown", Res.string.quiz_accessory_crown),
    SUNGLASSES("sunglasses", Res.string.quiz_accessory_sunglasses),
    PARTY_HAT("party_hat", Res.string.quiz_accessory_party_hat),
    HEADPHONES("headphones", Res.string.quiz_accessory_headphones),
    GRAD_CAP("grad_cap", Res.string.quiz_accessory_grad_cap);

    companion object {
        fun fromId(id: String?): QuizAccessory = entries.firstOrNull { it.id == id } ?: NONE
    }
}

/** What a player shows up as: a character plus one optional accessory. */
data class QuizAvatar(
    val character: QuizCharacter,
    val accessory: QuizAccessory = QuizAccessory.NONE
) {
    val key: String get() = "${character.id}:${accessory.id}"

    companion object {
        fun fromKey(key: String?): QuizAvatar? {
            val parts = key?.split(":") ?: return null
            val character = QuizCharacter.fromId(parts.getOrNull(0)) ?: return null
            return QuizAvatar(character, QuizAccessory.fromId(parts.getOrNull(1)))
        }

        val Saver: Saver<QuizAvatar, String> = Saver(
            save = { it.key },
            restore = { fromKey(it) }
        )
    }
}

enum class CharacterExpression { NEUTRAL, HAPPY, SAD, SURPRISED, WINK, DETERMINED }
enum class CharacterPose { REST, CHEER, WAVE }
enum class CharacterFraming { HEAD, FULL }

private val Ink = Color(0xFF1E1B2E)
private val MouthInside = Color(0xFF5A1E2B)
private val Tongue = Color(0xFFFF7A8A)
private val Blush = Color(0xFFFF8FA3)
private const val OUTLINE = 2.6f

private data class CharacterLook(
    val main: Color,
    val belly: Color,
    val detail: Color,
    val accent: Color,
    val eyeY: Float = 52f,
    val eyeDx: Float = 13.5f,
    val eyeRx: Float = 8.5f,
    val eyeRy: Float = 9.5f,
    val mouthY: Float = 69f,
    val mouthColor: Color = Ink,
    val hasMouth: Boolean = true,
    val blush: Boolean = true,
    val armColor: Color? = null
)

private fun QuizCharacter.look(): CharacterLook = when (this) {
    QuizCharacter.FOX -> CharacterLook(Color(0xFFF07A2B), Color(0xFFFFF1DE), Color(0xFF7A3414), Color(0xFFFFF1DE), mouthY = 71f)
    QuizCharacter.PANDA -> CharacterLook(Color(0xFFFAFAF5), Color(0xFFEDEDE6), Color(0xFF26263A), Color(0xFF26263A), mouthY = 71f, armColor = Color(0xFF26263A))
    QuizCharacter.ROBOT -> CharacterLook(Color(0xFF8FB4D9), Color(0xFFC9DDF0), Color(0xFF1C2A3F), Color(0xFF4FF3D2), mouthY = 68f, mouthColor = Color(0xFF4FF3D2), blush = false)
    QuizCharacter.OWL -> CharacterLook(Color(0xFF8B5FBF), Color(0xFFEBDDFF), Color(0xFF5C3A86), Color(0xFFFFB224), eyeDx = 14.5f, eyeRx = 9.5f, eyeRy = 10.5f, hasMouth = false)
    QuizCharacter.CAT -> CharacterLook(Color(0xFFFFCB5C), Color(0xFFFFF4D6), Color(0xFFE8892E), Color(0xFFFF8FA3), mouthY = 69f)
    QuizCharacter.FROG -> CharacterLook(Color(0xFF5CC95A), Color(0xFFD9F7A8), Color(0xFF2F8A3A), Color(0xFFFF8FA3), eyeY = 27f, eyeDx = 16f, eyeRx = 8f, eyeRy = 8.5f, mouthY = 63f)
    QuizCharacter.DRAGON -> CharacterLook(Color(0xFF2BB5A3), Color(0xFFFFE08A), Color(0xFF167A6D), Color(0xFFFFF4D6), mouthY = 72f)
    QuizCharacter.KOALA -> CharacterLook(Color(0xFFA9B4C2), Color(0xFFE6EBF1), Color(0xFF3B3F52), Color(0xFFFFC2CF), mouthY = 77f)
    QuizCharacter.ALIEN -> CharacterLook(Color(0xFF5EE0E6), Color(0xFFCFF8FA), Color(0xFF1E9AA6), Color(0xFFFF6FB5), eyeY = 53f, eyeDx = 15f, eyeRx = 10f, eyeRy = 11.5f, mouthY = 72f)
    QuizCharacter.MONSTER -> CharacterLook(Color(0xFFFF6FAE), Color(0xFFFFD3E6), Color(0xFFD9408A), Color(0xFFFFF0C7), mouthY = 70f)
}

private fun Color.shade(f: Float = 0.22f) = androidx.compose.ui.graphics.lerp(this, Color.Black, f)

// ---- primitive helpers (all in character units) ----

private fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, fill: Color, outline: Boolean = true) {
    drawOval(fill, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2))
    if (outline) drawOval(Ink, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), style = Stroke(OUTLINE))
}

private fun DrawScope.shape(path: Path, fill: Color, outline: Boolean = true) {
    drawPath(path, fill)
    if (outline) drawPath(path, Ink, style = Stroke(OUTLINE, join = StrokeJoin.Round))
}

private fun tri(a: Offset, b: Offset, c: Offset) = Path().apply {
    moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close()
}

private fun DrawScope.limb(from: Offset, to: Offset, color: Color, width: Float = 8f) {
    drawLine(Ink, from, to, strokeWidth = width + OUTLINE * 2, cap = StrokeCap.Round)
    drawLine(color, from, to, strokeWidth = width, cap = StrokeCap.Round)
}

private fun DrawScope.curve(from: Offset, control: Offset, to: Offset, color: Color = Ink, width: Float = 2.8f) {
    drawPath(
        Path().apply { moveTo(from.x, from.y); quadraticTo(control.x, control.y, to.x, to.y) },
        color,
        style = Stroke(width, cap = StrokeCap.Round)
    )
}

private const val HEAD_CX = 50f
private const val HEAD_CY = 52f
private const val HEAD_RX = 37f
private const val HEAD_RY = 34f

private fun headPath(character: QuizCharacter): Path = Path().apply {
    when (character) {
        QuizCharacter.ROBOT -> addRoundRect(
            androidx.compose.ui.geometry.RoundRect(14f, 21f, 86f, 85f, CornerRadius(20f, 20f))
        )
        QuizCharacter.ALIEN -> addOval(androidx.compose.ui.geometry.Rect(12f, 15f, 88f, 87f))
        else -> addOval(
            androidx.compose.ui.geometry.Rect(HEAD_CX - HEAD_RX, HEAD_CY - HEAD_RY, HEAD_CX + HEAD_RX, HEAD_CY + HEAD_RY)
        )
    }
}

/**
 * Draws one character. [wave] (0..1) drives the waving arm; [blink] closes open eyes for a frame.
 */
fun DrawScope.drawQuizCharacter(
    avatar: QuizAvatar,
    expression: CharacterExpression = CharacterExpression.NEUTRAL,
    pose: CharacterPose = CharacterPose.REST,
    framing: CharacterFraming = CharacterFraming.FULL,
    blink: Boolean = false,
    wave: Float = 0f
) {
    val unitsW: Float
    val unitsH: Float
    val originX: Float
    val originY: Float
    if (framing == CharacterFraming.HEAD) {
        unitsW = 96f; unitsH = 96f; originX = 2f; originY = -4f
    } else {
        unitsW = 100f; unitsH = 128f; originX = 0f; originY = -4f
    }
    val s = minOf(size.width / unitsW, size.height / unitsH)
    val dx = (size.width - unitsW * s) / 2f
    val dy = (size.height - unitsH * s) / 2f

    withTransform({
        translate(dx - originX * s, dy - originY * s)
        scale(s, s, Offset.Zero)
    }) {
        val character = avatar.character
        val look = character.look()
        if (framing == CharacterFraming.FULL) {
            drawBehindBody(character, look)
            drawBody(character, look, pose, wave)
        }
        drawBehindHead(character, look)
        val head = headPath(character)
        shape(head, look.main)
        clipPath(head) {
            // soft shading: darker chin, light top-left sheen
            drawOval(look.main.shade(0.16f), Offset(4f, 66f), Size(92f, 40f))
            drawOval(Color.White.copy(alpha = 0.32f), Offset(24f, 24f), Size(22f, 12f))
        }
        drawPath(head, Ink, style = Stroke(OUTLINE))
        drawFace(character, look)
        drawEyes(character, look, expression, blink)
        drawMouth(character, look, expression)
        drawFaceTop(character, look)
        drawAccessory(avatar.accessory, character)
    }
}

private fun DrawScope.drawBehindBody(character: QuizCharacter, look: CharacterLook) {
    when (character) {
        QuizCharacter.FOX -> rotate(38f, Offset(80f, 104f)) {
            oval(80f, 104f, 10f, 22f, look.main)
            oval(80f, 86f, 7f, 7f, look.belly)
        }
        QuizCharacter.CAT -> curve(Offset(70f, 112f), Offset(98f, 108f), Offset(88f, 82f), look.detail, 7f)
        QuizCharacter.DRAGON -> {
            shape(tri(Offset(68f, 92f), Offset(96f, 76f), Offset(84f, 104f)), look.detail)
            shape(tri(Offset(32f, 92f), Offset(4f, 76f), Offset(16f, 104f)), look.detail)
            curve(Offset(64f, 116f), Offset(92f, 124f), Offset(94f, 106f), look.main, 7f)
        }
        QuizCharacter.MONSTER -> curve(Offset(68f, 114f), Offset(94f, 118f), Offset(90f, 98f), look.detail, 6f)
        else -> Unit
    }
}

private fun DrawScope.drawBody(character: QuizCharacter, look: CharacterLook, pose: CharacterPose, wave: Float) {
    val feetColor = if (character == QuizCharacter.PANDA) look.detail else look.main.shade(0.12f)
    oval(39f, 120f, 9f, 5f, feetColor)
    oval(61f, 120f, 9f, 5f, feetColor)
    if (character == QuizCharacter.ROBOT) {
        shape(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(28f, 82f, 72f, 118f, CornerRadius(10f, 10f))) }, look.main)
        drawRoundRect(look.detail, Offset(38f, 92f), Size(24f, 14f), CornerRadius(4f, 4f))
        drawCircle(look.accent, 2.5f, Offset(44f, 99f))
        drawCircle(Color(0xFFFF6FB5), 2.5f, Offset(50f, 99f))
        drawCircle(Color(0xFFFFD24A), 2.5f, Offset(56f, 99f))
    } else {
        oval(50f, 102f, 24f, 18f, look.main)
        oval(50f, 105f, 14f, 11f, look.belly, outline = false)
        if (character == QuizCharacter.OWL) {
            curve(Offset(45f, 100f), Offset(47f, 103f), Offset(49f, 100f), look.detail, 1.6f)
            curve(Offset(51f, 106f), Offset(53f, 109f), Offset(55f, 106f), look.detail, 1.6f)
        }
    }
    val arm = look.armColor ?: look.main
    val armWidth = if (character == QuizCharacter.OWL) 11f else 8f
    when (pose) {
        CharacterPose.REST -> {
            limb(Offset(30f, 94f), Offset(25f, 109f), arm, armWidth)
            limb(Offset(70f, 94f), Offset(75f, 109f), arm, armWidth)
        }
        CharacterPose.CHEER -> {
            limb(Offset(31f, 92f), Offset(15f, 73f), arm, armWidth)
            limb(Offset(69f, 92f), Offset(85f, 73f), arm, armWidth)
        }
        CharacterPose.WAVE -> {
            limb(Offset(30f, 94f), Offset(25f, 109f), arm, armWidth)
            val angle = (-55f + 22f * sin(wave * 2f * PI.toFloat())) * PI.toFloat() / 180f
            val len = 20f
            limb(Offset(69f, 92f), Offset(69f + cos(angle) * len, 92f + sin(angle) * len), arm, armWidth)
        }
    }
}

private fun DrawScope.drawBehindHead(character: QuizCharacter, look: CharacterLook) {
    when (character) {
        QuizCharacter.FOX -> {
            shape(tri(Offset(16f, 36f), Offset(24f, 4f), Offset(44f, 22f)), look.main)
            shape(tri(Offset(84f, 36f), Offset(76f, 4f), Offset(56f, 22f)), look.main)
            drawPath(tri(Offset(22f, 30f), Offset(26f, 12f), Offset(37f, 22f)), look.detail)
            drawPath(tri(Offset(78f, 30f), Offset(74f, 12f), Offset(63f, 22f)), look.detail)
        }
        QuizCharacter.PANDA -> {
            oval(21f, 25f, 11f, 11f, look.detail)
            oval(79f, 25f, 11f, 11f, look.detail)
        }
        QuizCharacter.ROBOT -> {
            drawLine(Ink, Offset(50f, 21f), Offset(50f, 7f), strokeWidth = 3.4f)
            oval(50f, 6f, 5.5f, 5.5f, look.accent)
            oval(10f, 53f, 5f, 9f, look.detail)
            oval(90f, 53f, 5f, 9f, look.detail)
        }
        QuizCharacter.OWL -> {
            val left = Path().apply { moveTo(22f, 30f); quadraticTo(8f, 18f, 4f, 6f); quadraticTo(22f, 12f, 36f, 22f); close() }
            val right = Path().apply { moveTo(78f, 30f); quadraticTo(92f, 18f, 96f, 6f); quadraticTo(78f, 12f, 64f, 22f); close() }
            shape(left, look.detail)
            shape(right, look.detail)
        }
        QuizCharacter.CAT -> {
            shape(tri(Offset(17f, 38f), Offset(22f, 8f), Offset(44f, 22f)), look.main)
            shape(tri(Offset(83f, 38f), Offset(78f, 8f), Offset(56f, 22f)), look.main)
            drawPath(tri(Offset(23f, 31f), Offset(25f, 15f), Offset(37f, 23f)), look.accent)
            drawPath(tri(Offset(77f, 31f), Offset(75f, 15f), Offset(63f, 23f)), look.accent)
        }
        QuizCharacter.FROG -> {
            oval(50f - look.eyeDx, look.eyeY, 13f, 12f, look.main)
            oval(50f + look.eyeDx, look.eyeY, 13f, 12f, look.main)
        }
        QuizCharacter.DRAGON -> {
            val leftHorn = Path().apply { moveTo(24f, 28f); quadraticTo(10f, 14f, 16f, 2f); quadraticTo(26f, 14f, 36f, 21f); close() }
            val rightHorn = Path().apply { moveTo(76f, 28f); quadraticTo(90f, 14f, 84f, 2f); quadraticTo(74f, 14f, 64f, 21f); close() }
            shape(leftHorn, look.accent)
            shape(rightHorn, look.accent)
            shape(tri(Offset(38f, 21f), Offset(43f, 9f), Offset(48f, 19f)), look.detail)
            shape(tri(Offset(45f, 19f), Offset(50f, 5f), Offset(55f, 19f)), look.detail)
            shape(tri(Offset(52f, 19f), Offset(57f, 9f), Offset(62f, 21f)), look.detail)
        }
        QuizCharacter.KOALA -> {
            oval(17f, 32f, 15f, 15f, look.main)
            oval(83f, 32f, 15f, 15f, look.main)
            oval(18f, 33f, 9f, 9f, look.accent, outline = false)
            oval(82f, 33f, 9f, 9f, look.accent, outline = false)
        }
        QuizCharacter.ALIEN -> {
            curve(Offset(38f, 18f), Offset(32f, 8f), Offset(24f, 4f), Ink, 3f)
            curve(Offset(62f, 18f), Offset(68f, 8f), Offset(76f, 4f), Ink, 3f)
            oval(24f, 4f, 5f, 5f, look.accent)
            oval(76f, 4f, 5f, 5f, look.accent)
        }
        QuizCharacter.MONSTER -> {
            shape(tri(Offset(26f, 26f), Offset(18f, 4f), Offset(38f, 20f)), look.accent)
            shape(tri(Offset(74f, 26f), Offset(82f, 4f), Offset(62f, 20f)), look.accent)
        }
    }
}

private fun DrawScope.drawFace(character: QuizCharacter, look: CharacterLook) {
    val ey = look.eyeY
    when (character) {
        QuizCharacter.FOX -> {
            oval(37f, 66f, 15f, 11f, look.belly, outline = false)
            oval(63f, 66f, 15f, 11f, look.belly, outline = false)
            oval(50f, 70f, 12f, 9f, look.belly, outline = false)
        }
        QuizCharacter.PANDA -> {
            rotate(-24f, Offset(50f - look.eyeDx, ey + 1f)) { oval(50f - look.eyeDx, ey + 1f, 11f, 13f, look.detail, outline = false) }
            rotate(24f, Offset(50f + look.eyeDx, ey + 1f)) { oval(50f + look.eyeDx, ey + 1f, 11f, 13f, look.detail, outline = false) }
        }
        QuizCharacter.ROBOT -> {
            drawRoundRect(look.detail, Offset(22f, 36f), Size(56f, 42f), CornerRadius(12f, 12f))
            drawCircle(look.main.shade(0.3f), 1.8f, Offset(19f, 27f))
            drawCircle(look.main.shade(0.3f), 1.8f, Offset(81f, 27f))
        }
        QuizCharacter.OWL -> {
            oval(50f - look.eyeDx, ey, 14f, 14f, look.belly, outline = false)
            oval(50f + look.eyeDx, ey, 14f, 14f, look.belly, outline = false)
            drawPath(tri(Offset(40f, 30f), Offset(60f, 30f), Offset(50f, 42f)), look.detail)
        }
        QuizCharacter.CAT -> {
            curve(Offset(44f, 22f), Offset(45f, 28f), Offset(44f, 32f), look.detail, 3f)
            curve(Offset(50f, 19f), Offset(51f, 26f), Offset(50f, 31f), look.detail, 3f)
            curve(Offset(56f, 22f), Offset(55f, 28f), Offset(56f, 32f), look.detail, 3f)
        }
        QuizCharacter.FROG -> oval(50f, 66f, 22f, 14f, look.belly, outline = false)
        QuizCharacter.DRAGON -> oval(50f, 70f, 18f, 11f, look.belly, outline = false)
        QuizCharacter.KOALA -> oval(50f, 68f, 18f, 13f, look.belly, outline = false)
        QuizCharacter.ALIEN -> {
            drawCircle(look.detail.copy(alpha = 0.35f), 4f, Offset(30f, 30f))
            drawCircle(look.detail.copy(alpha = 0.35f), 2.6f, Offset(68f, 26f))
            drawCircle(look.detail.copy(alpha = 0.35f), 3f, Offset(74f, 72f))
        }
        QuizCharacter.MONSTER -> {
            drawCircle(look.detail, 4.5f, Offset(26f, 40f))
            drawCircle(look.detail, 3f, Offset(74f, 34f))
            drawCircle(look.detail, 3.5f, Offset(78f, 66f))
        }
    }
    if (look.blush) {
        oval(26f, ey + 13f, 5.5f, 3.2f, Blush.copy(alpha = 0.55f), outline = false)
        oval(74f, ey + 13f, 5.5f, 3.2f, Blush.copy(alpha = 0.55f), outline = false)
    }
}

private fun DrawScope.drawEyes(character: QuizCharacter, look: CharacterLook, expression: CharacterExpression, blink: Boolean) {
    val ey = look.eyeY
    val leftX = 50f - look.eyeDx
    val rightX = 50f + look.eyeDx
    fun openEye(cx: Float, rxScale: Float = 1f, pupilScale: Float = 1f, lookY: Float = 1f) {
        val rx = look.eyeRx * rxScale
        val ry = look.eyeRy * rxScale
        oval(cx, ey, rx, ry, Color.White)
        val pr = rx * 0.56f * pupilScale
        val pupilColor = if (character == QuizCharacter.ROBOT) look.detail else Ink
        drawCircle(pupilColor, pr, Offset(cx + 0.6f, ey + lookY))
        drawCircle(Color.White, pr * 0.36f, Offset(cx + 0.6f - pr * 0.38f, ey + lookY - pr * 0.42f))
    }
    // Closed-eye lines must stay visible on dark eye areas (panda patches, robot screen).
    val lineColor = when (character) {
        QuizCharacter.ROBOT -> look.accent
        QuizCharacter.PANDA -> Color.White
        else -> Ink
    }
    fun happyEye(cx: Float) = curve(Offset(cx - look.eyeRx, ey + 2f), Offset(cx, ey - look.eyeRy * 1.2f), Offset(cx + look.eyeRx, ey + 2f), lineColor, 3.2f)
    fun closedEye(cx: Float) = curve(Offset(cx - look.eyeRx, ey), Offset(cx, ey + 3f), Offset(cx + look.eyeRx, ey), lineColor, 3f)

    when (expression) {
        CharacterExpression.HAPPY -> { happyEye(leftX); happyEye(rightX) }
        CharacterExpression.WINK -> {
            if (blink) closedEye(leftX) else openEye(leftX)
            happyEye(rightX)
        }
        CharacterExpression.SURPRISED -> { openEye(leftX, 1.14f, 0.62f, 0f); openEye(rightX, 1.14f, 0.62f, 0f) }
        CharacterExpression.SAD -> {
            openEye(leftX, lookY = 2.6f); openEye(rightX, lookY = 2.6f)
            val browY = ey - look.eyeRy - 4f
            curve(Offset(leftX - 8f, browY + 1f), Offset(leftX, browY - 1f), Offset(leftX + 7f, browY - 5f), Ink, 2.8f)
            curve(Offset(rightX + 8f, browY + 1f), Offset(rightX, browY - 1f), Offset(rightX - 7f, browY - 5f), Ink, 2.8f)
            drawPath(
                Path().apply {
                    moveTo(leftX - 6f, ey + 10f)
                    quadraticTo(leftX - 10f, ey + 17f, leftX - 6f, ey + 19f)
                    quadraticTo(leftX - 2f, ey + 17f, leftX - 6f, ey + 10f)
                },
                Color(0xFF5BC0FF)
            )
        }
        CharacterExpression.DETERMINED -> {
            if (blink) { closedEye(leftX); closedEye(rightX) } else { openEye(leftX); openEye(rightX) }
            val browY = ey - look.eyeRy - 3f
            drawLine(Ink, Offset(leftX - 8f, browY - 4f), Offset(leftX + 7f, browY + 1f), 3f, StrokeCap.Round)
            drawLine(Ink, Offset(rightX + 8f, browY - 4f), Offset(rightX - 7f, browY + 1f), 3f, StrokeCap.Round)
        }
        CharacterExpression.NEUTRAL -> {
            if (blink) { closedEye(leftX); closedEye(rightX) } else { openEye(leftX); openEye(rightX) }
        }
    }
}

private fun DrawScope.drawMouth(character: QuizCharacter, look: CharacterLook, expression: CharacterExpression) {
    if (!look.hasMouth) return
    val my = look.mouthY
    val wide = if (character == QuizCharacter.FROG) 1.5f else 1f
    when (expression) {
        CharacterExpression.HAPPY -> {
            val w = 11f * wide
            drawArc(MouthInside, 0f, 180f, true, Offset(50f - w, my - 8f), Size(w * 2, 18f))
            drawArc(Tongue, 25f, 130f, true, Offset(50f - w * 0.5f, my + 1f), Size(w, 9f))
            drawArc(Ink, 0f, 180f, true, Offset(50f - w, my - 8f), Size(w * 2, 18f), style = Stroke(OUTLINE))
        }
        CharacterExpression.SAD -> curve(Offset(50f - 7f * wide, my + 3f), Offset(50f, my - 4f), Offset(50f + 7f * wide, my + 3f), look.mouthColor, 2.8f)
        CharacterExpression.SURPRISED -> oval(50f, my + 1f, 4.5f, 5.5f, MouthInside)
        CharacterExpression.WINK -> {
            curve(Offset(50f - 9f * wide, my - 2f), Offset(50f, my + 7f), Offset(50f + 9f * wide, my - 3f), look.mouthColor, 2.8f)
            drawArc(Tongue, 0f, 180f, true, Offset(52f, my), Size(7f, 7f))
        }
        CharacterExpression.DETERMINED -> curve(Offset(50f - 8f * wide, my - 1f), Offset(50f + 2f, my + 5f), Offset(50f + 9f * wide, my - 3f), look.mouthColor, 2.8f)
        CharacterExpression.NEUTRAL -> curve(Offset(50f - 7f * wide, my - 2f), Offset(50f, my + 5f), Offset(50f + 7f * wide, my - 2f), look.mouthColor, 2.8f)
    }
    if (character == QuizCharacter.MONSTER && expression != CharacterExpression.SURPRISED) {
        shape(tri(Offset(43f, my + 1f), Offset(47f, my + 1f), Offset(45f, my + 6f)), Color.White)
        shape(tri(Offset(53f, my + 1f), Offset(57f, my + 1f), Offset(55f, my + 6f)), Color.White)
    }
}

private fun DrawScope.drawFaceTop(character: QuizCharacter, look: CharacterLook) {
    when (character) {
        QuizCharacter.FOX, QuizCharacter.PANDA -> oval(50f, 63f, 4.5f, 3.2f, Ink, outline = false)
        QuizCharacter.CAT -> {
            drawPath(tri(Offset(46.5f, 61f), Offset(53.5f, 61f), Offset(50f, 65f)), look.accent)
            drawLine(Ink, Offset(30f, 64f), Offset(16f, 61f), 1.6f, StrokeCap.Round)
            drawLine(Ink, Offset(30f, 68f), Offset(16f, 69f), 1.6f, StrokeCap.Round)
            drawLine(Ink, Offset(70f, 64f), Offset(84f, 61f), 1.6f, StrokeCap.Round)
            drawLine(Ink, Offset(70f, 68f), Offset(84f, 69f), 1.6f, StrokeCap.Round)
        }
        QuizCharacter.OWL -> shape(tri(Offset(44f, 61f), Offset(56f, 61f), Offset(50f, 71f)), look.accent)
        QuizCharacter.DRAGON -> {
            drawCircle(look.detail, 1.8f, Offset(45f, 66f))
            drawCircle(look.detail, 1.8f, Offset(55f, 66f))
        }
        QuizCharacter.KOALA -> oval(50f, 65f, 8f, 9.5f, look.detail)
        else -> Unit
    }
}

/** Accessories sit on the head outline and never cover the eyes or mouth (sunglasses aside). */
private fun DrawScope.drawAccessory(accessory: QuizAccessory, character: QuizCharacter) {
    val topY = when (character) {
        QuizCharacter.ROBOT -> 21f
        QuizCharacter.ALIEN -> 15f
        QuizCharacter.FROG -> 16f
        else -> HEAD_CY - HEAD_RY
    }
    val look = character.look()
    when (accessory) {
        QuizAccessory.NONE -> Unit
        QuizAccessory.CROWN -> {
            val base = topY + 6f
            val path = Path().apply {
                moveTo(34f, base); lineTo(32f, base - 15f); lineTo(41f, base - 8f); lineTo(50f, base - 19f)
                lineTo(59f, base - 8f); lineTo(68f, base - 15f); lineTo(66f, base); close()
            }
            shape(path, Color(0xFFFFC93C))
            drawCircle(Color(0xFFFF4D6D), 2.4f, Offset(50f, base - 6f))
            drawCircle(Color(0xFF4FC3F7), 1.8f, Offset(40f, base - 4f))
            drawCircle(Color(0xFF4FC3F7), 1.8f, Offset(60f, base - 4f))
        }
        QuizAccessory.SUNGLASSES -> {
            val ey = look.eyeY
            val lx = 50f - look.eyeDx
            val rx = 50f + look.eyeDx
            val w = look.eyeRx + 4f
            val lens = Color(0xFF15131F)
            shape(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(lx - w, ey - 7f, lx + w, ey + 7f, CornerRadius(6f, 6f))) }, lens)
            shape(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(rx - w, ey - 7f, rx + w, ey + 7f, CornerRadius(6f, 6f))) }, lens)
            drawLine(Ink, Offset(lx + w, ey - 3f), Offset(rx - w, ey - 3f), 2.6f)
            drawLine(Color.White.copy(alpha = 0.55f), Offset(lx - w + 4f, ey - 3f), Offset(lx - w + 9f, ey - 3f), 2f, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.55f), Offset(rx - w + 4f, ey - 3f), Offset(rx - w + 9f, ey - 3f), 2f, StrokeCap.Round)
        }
        QuizAccessory.PARTY_HAT -> rotate(18f, Offset(62f, topY + 4f)) {
            val path = tri(Offset(51f, topY + 6f), Offset(73f, topY + 6f), Offset(62f, topY - 22f))
            shape(path, Color(0xFF7C5CFF))
            clipPath(path) {
                drawLine(Color(0xFFFFD24A), Offset(50f, topY - 2f), Offset(76f, topY - 8f), 3f)
                drawLine(Color(0xFFFF6FB5), Offset(50f, topY - 12f), Offset(76f, topY - 18f), 3f)
            }
            drawPath(path, Ink, style = Stroke(OUTLINE, join = StrokeJoin.Round))
            oval(62f, topY - 23f, 4f, 4f, Color(0xFFFFD24A))
        }
        QuizAccessory.HEADPHONES -> {
            val band = Path().apply {
                moveTo(15f, 50f)
                cubicTo(14f, topY - 14f, 86f, topY - 14f, 85f, 50f)
            }
            drawPath(band, Ink, style = Stroke(9f, cap = StrokeCap.Round))
            drawPath(band, Color(0xFFFF4D6D), style = Stroke(5f, cap = StrokeCap.Round))
            shape(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(7f, 42f, 21f, 66f, CornerRadius(6f, 6f))) }, Color(0xFFFF4D6D))
            shape(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(79f, 42f, 93f, 66f, CornerRadius(6f, 6f))) }, Color(0xFFFF4D6D))
        }
        QuizAccessory.GRAD_CAP -> {
            val y = topY + 2f
            shape(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(37f, y - 6f, 63f, y + 5f, CornerRadius(3f, 3f))) }, Color(0xFF26263A))
            shape(
                Path().apply { moveTo(50f, y - 17f); lineTo(76f, y - 9f); lineTo(50f, y - 1f); lineTo(24f, y - 9f); close() },
                Color(0xFF26263A)
            )
            drawLine(Color(0xFFFFC93C), Offset(50f, y - 9f), Offset(70f, y - 4f), 2f, StrokeCap.Round)
            drawLine(Color(0xFFFFC93C), Offset(70f, y - 4f), Offset(70f, y + 8f), 2f, StrokeCap.Round)
            drawCircle(Color(0xFFFFC93C), 2.6f, Offset(70f, y + 9f))
        }
    }
}

/** Static character drawing; see [AnimatedQuizCharacter] for the moving version. */
@Composable
fun QuizCharacterArt(
    avatar: QuizAvatar,
    modifier: Modifier = Modifier,
    expression: CharacterExpression = CharacterExpression.NEUTRAL,
    pose: CharacterPose = CharacterPose.REST,
    framing: CharacterFraming = CharacterFraming.FULL,
    blink: Boolean = false,
    wave: Float = 0f
) {
    val ratio = if (framing == CharacterFraming.HEAD) 1f else 100f / 128f
    Canvas(modifier = modifier.aspectRatio(ratio)) {
        drawQuizCharacter(avatar, expression, pose, framing, blink, wave)
    }
}
