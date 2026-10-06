package com.good4.social

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.SocialPoster
import com.good4.core.presentation.SocialPosterLine
import com.good4.core.presentation.SocialSportLine
import com.good4.core.presentation.SocialSportPoster
import kotlin.math.max

/**
 * The activity's poster: a line drawing for its type (court, track, steam,
 * film strip …) with the day and time in the corner. Sport posters are dark
 * green with lime lines, social ones cream with green lines.
 */
@Composable
fun SocialActivityPoster(
    type: String,
    kind: String,
    modifier: Modifier = Modifier,
    dayLabel: String? = null,
    timeLabel: String? = null,
    timeSize: TextUnit = 24.sp,
    textPadding: Dp = 10.dp,
    /** Wide bands (the detail header) keep the drawing whole at the right; cards fill with it. */
    alignEnd: Boolean = false,
    /** The band runs under the status bar and the back button; the drawing starts below this much. */
    artTopInset: Dp = 0.dp,
    game: String? = null
) {
    val sport = kind == "sport"
    val paper = if (sport) SocialSportPoster else SocialPoster
    val text = if (sport) Color.White else SocialPosterLine
    // Canvas does not clip; a drawing larger than the poster must not paint over the screen.
    Box(modifier.clipToBounds()) {
        Canvas(Modifier.fillMaxSize()) { drawPoster(if (type == "board-games" && game == "okey") "okey" else type, sport, alignEnd, artTopInset.toPx()) }
        if (timeLabel != null) {
            // A soft fade of the poster colour keeps the time readable over court lines.
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().fillMaxHeight(0.45f)
                    .background(Brush.verticalGradient(listOf(paper.copy(alpha = 0f), paper.copy(alpha = 0.9f))))
            )
            Column(Modifier.align(Alignment.BottomStart).padding(textPadding)) {
                dayLabel?.let { Text(it, color = text.copy(alpha = 0.85f), fontSize = (timeSize.value * 0.46f).sp) }
                Text(timeLabel, color = text, fontSize = timeSize, fontWeight = FontWeight.SemiBold, lineHeight = timeSize)
            }
        }
    }
}

/** Draws in a 100 × 125 artboard: scaled to cover and centred, or fitted to the height at the right edge. */
private fun DrawScope.drawPoster(type: String, sport: Boolean, alignEnd: Boolean, topInset: Float) {
    drawRect(if (sport) SocialSportPoster else SocialPoster)
    val unit = if (alignEnd) (size.height - topInset) / 125f else max(size.width / 100f, size.height / 125f)
    val left = if (alignEnd) size.width - 100f * unit else (size.width - 100f * unit) / 2f
    val top = if (alignEnd) topInset else (size.height - 125f * unit) / 2f
    translate(left, top) {
        scale(unit, unit, pivot = Offset.Zero) {
            val painter = if (sport) PosterPainter(this, SocialSportLine.copy(alpha = 0.55f), SocialSportLine, SocialSportPoster)
            else PosterPainter(this, SocialPosterLine.copy(alpha = 0.5f), SocialPosterLine, SocialPoster)
            painter.drawMotif(type)
        }
    }
}

private val NET = Color.White

/** Drawing helpers in artboard units; the motif colours travel with the painter. */
private class PosterPainter(scope: DrawScope, val line: Color, val accent: Color, val paper: Color) : DrawScope by scope {
    fun stroke(width: Float = 1.4f, dash: FloatArray? = null) =
        Stroke(width, pathEffect = dash?.let { PathEffect.dashPathEffect(it) })

    fun box(x: Float, y: Float, w: Float, h: Float, radius: Float = 0f, color: Color = line, width: Float = 1.4f) =
        drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(radius), style = stroke(width))

    fun fillBox(x: Float, y: Float, w: Float, h: Float, radius: Float = 0f, color: Color = accent) =
        drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(radius))

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color = line, width: Float = 1.4f, dash: FloatArray? = null) =
        drawLine(color, Offset(x1, y1), Offset(x2, y2), width, pathEffect = dash?.let { PathEffect.dashPathEffect(it) })

    fun ring(x: Float, y: Float, r: Float, color: Color = line, width: Float = 1.4f) =
        drawCircle(color, r, Offset(x, y), style = stroke(width))

    fun dot(x: Float, y: Float, r: Float, color: Color = accent) = drawCircle(color, r, Offset(x, y))

    fun outline(path: Path, color: Color = line, width: Float = 1.4f, dash: FloatArray? = null) =
        drawPath(path, color, style = stroke(width, dash))

    fun dotGrid(color: Color) {
        for (x in 0..8) for (y in 0..10) drawCircle(color, 1.4f, Offset(6f + x * 12f, 6f + y * 12f))
    }

    fun drawMotif(type: String) {
        // Rotated blocks are DrawScope DSL lambdas, so they reach the painter explicitly.
        val p = this
        when (type) {
            "football" -> {
                box(12f, 8f, 76f, 109f); line(12f, 62.5f, 88f, 62.5f); ring(50f, 62.5f, 13f)
                box(30f, 8f, 40f, 18f); box(30f, 99f, 40f, 18f); dot(50f, 62.5f, 3.5f)
            }
            "basketball" -> {
                box(12f, 8f, 76f, 109f); box(36f, 8f, 28f, 38f); ring(50f, 46f, 11f)
                line(18f, 8f, 18f, 34f); line(82f, 8f, 82f, 34f)
                drawArc(line, 0f, 180f, false, Offset(18f, 2f), Size(64f, 64f), style = stroke())
                ring(50f, 17f, 4f, accent, 2f); dot(68f, 92f, 8f)
            }
            "volleyball" -> {
                box(14f, 10f, 72f, 105f); line(14f, 40f, 86f, 40f); line(14f, 85f, 86f, 85f)
                line(6f, 62.5f, 94f, 62.5f, NET, 3f, floatArrayOf(2f, 2f)); dot(34f, 28f, 8f)
            }
            "tennis" -> {
                box(14f, 10f, 72f, 105f)
                line(24f, 10f, 24f, 115f); line(76f, 10f, 76f, 115f)
                line(24f, 40f, 76f, 40f); line(24f, 85f, 76f, 85f); line(50f, 40f, 50f, 85f)
                line(8f, 62.5f, 92f, 62.5f, NET, 2f); dot(66f, 30f, 6f)
            }
            "table-tennis" -> {
                box(18f, 22f, 64f, 82f); line(50f, 22f, 50f, 104f); line(10f, 63f, 90f, 63f, NET, 2f)
                dot(64f, 40f, 3.5f); ring(34f, 86f, 11f, accent, 2.4f); line(34f, 97f, 34f, 110f, accent, 3.4f)
            }
            "badminton" -> {
                line(16f, 8f, 16f, 117f); line(84f, 8f, 84f, 117f); line(10f, 62.5f, 90f, 62.5f, NET, 2f)
                val feathers = Path().apply { moveTo(54f, 40f); lineTo(74f, 14f); lineTo(84f, 24f); close() }
                drawPath(feathers, accent); dot(54f, 40f, 6f)
            }
            "running" -> {
                box(12f, 8f, 76f, 109f, 38f); box(21f, 17f, 58f, 91f, 29f); box(30f, 26f, 40f, 73f, 20f)
                line(12f, 62.5f, 30f, 62.5f, NET, 2.5f)
            }
            "hiking" -> {
                listOf(
                    "M50 8 C82 8 96 40 92 64 C88 92 66 106 42 102 C14 98 4 70 10 46 C14 26 30 8 50 8Z",
                    "M50 20 C70 20 82 40 78 58 C74 78 58 88 44 84 C28 80 20 62 26 46 C30 32 38 20 50 20Z",
                    "M50 32 C64 32 70 46 66 58 C62 70 52 74 44 72 C34 68 32 56 35 48 C38 40 42 32 50 32Z",
                    "M50 44 C58 44 60 52 58 58 C56 63 50 64 46 62 C42 60 42 54 44 50 C45 47 47 44 50 44Z"
                ).forEach { outline(svgPath(it)) }
                drawPath(Path().apply { moveTo(51f, 48f); lineTo(56f, 57f); lineTo(46f, 57f); close() }, accent)
            }
            "cycling" -> {
                listOf(0f, 20f, 40f).forEachIndexed { index, dy ->
                    val route = Path().apply {
                        moveTo(-5f, 34f + dy); cubicTo(25f, 14f + dy, 45f, 59f + dy, 70f, 39f + dy)
                        cubicTo(95f, 19f + dy, 100f, 24f + dy, 110f, 44f + dy)
                    }
                    outline(route, width = 1.6f, dash = if (index == 2) floatArrayOf(3f, 3f) else null)
                }
                dot(70f, 39f, 5f)
            }
            "swimming" -> {
                line(0f, 18f, 100f, 18f)
                listOf(25f, 50f, 75f).forEach { line(it, 18f, it, 125f, dash = floatArrayOf(5f, 3f)) }
                val wave = Path().apply {
                    moveTo(26f, 66f); quadraticTo(32f, 58f, 38f, 66f); quadraticTo(44f, 74f, 50f, 66f)
                    quadraticTo(56f, 58f, 62f, 66f); quadraticTo(68f, 74f, 74f, 66f)
                }
                outline(wave, accent, 3f)
            }
            "sup-kayak" -> {
                listOf(44f, 62f, 80f, 98f).forEach { y ->
                    val wave = Path().apply {
                        moveTo(-5f, y)
                        var x = -5f
                        var up = true
                        while (x < 105f) {
                            quadraticTo(x + 12.5f, if (up) y - 9f else y + 9f, x + 25f, y); x += 25f; up = !up
                        }
                    }
                    outline(wave)
                }
                rotate(-12f, Offset(56f, 32f)) { drawOval(p.accent, Offset(34f, 27.5f), Size(44f, 9f)) }
            }
            "skating" -> {
                outline(Path().apply { moveTo(10f, 22f); quadraticTo(10f, 92f, 50f, 92f); quadraticTo(90f, 92f, 90f, 22f) })
                outline(Path().apply { moveTo(20f, 22f); quadraticTo(20f, 82f, 50f, 82f); quadraticTo(80f, 82f, 80f, 22f) })
                line(0f, 22f, 20f, 22f); line(80f, 22f, 100f, 22f)
                rotate(-38f, Offset(71f, 66f)) { p.fillBox(58f, 63.5f, 26f, 5f, 2.5f) }
            }
            "fitness" -> {
                line(0f, 96f, 100f, 96f); line(0f, 104f, 100f, 104f)
                box(14f, 40f, 8f, 34f); box(24f, 45f, 6f, 24f); box(78f, 40f, 8f, 34f); box(70f, 45f, 6f, 24f)
                line(6f, 57f, 94f, 57f, accent, 3f)
            }
            "yoga-pilates" -> {
                listOf(38f, 26f, 14f).forEach { r ->
                    drawArc(line, 180f, 180f, false, Offset(50f - r, 82f - r), Size(2 * r, 2 * r), style = stroke())
                }
                box(18f, 92f, 64f, 10f, 5f); dot(50f, 82f, 4.5f)
            }
            "coffee" -> {
                listOf(16f, 26f, 36f, 46f).forEach { ring(60f, 46f, it) }; dot(60f, 46f, 7f)
            }
            "food" -> {
                ring(50f, 52f, 30f); ring(50f, 52f, 20f)
                line(12f, 30f, 12f, 84f); line(8f, 30f, 8f, 42f); line(16f, 30f, 16f, 42f)
                outline(Path().apply { moveTo(88f, 30f); quadraticTo(94f, 42f, 88f, 54f); lineTo(88f, 84f) })
                dot(50f, 52f, 5f)
            }
            "meetup" -> {
                ring(38f, 52f, 26f); ring(62f, 52f, 26f); ring(38f, 52f, 36f); ring(62f, 52f, 36f)
                val lens = Path().apply {
                    op(Path().apply { addOval(Rect(Offset(38f, 52f), 26f)) }, Path().apply { addOval(Rect(Offset(62f, 52f), 26f)) },
                        PathOperation.Intersect)
                }
                drawPath(lens, accent.copy(alpha = 0.85f))
            }
            "picnic" -> {
                rotate(-12f, Offset(50f, 55f)) {
                    val tint = p.accent.copy(alpha = 0.14f)
                    for (row in 0..3) for (column in 0..3) {
                        if ((row + column) % 2 == 0) p.fillBox(15f + column * 17.5f, 20f + row * 17.5f, 17.5f, 17.5f, color = tint)
                    }
                    p.box(15f, 20f, 70f, 70f)
                }
                dot(74f, 100f, 6f)
            }
            "movie" -> {
                box(20f, -2f, 60f, 130f); line(32f, -2f, 32f, 128f); line(68f, -2f, 68f, 128f)
                line(32f, 38f, 68f, 38f); line(32f, 82f, 68f, 82f)
                line(26f, 0f, 26f, 125f, width = 4f, dash = floatArrayOf(4f, 6f))
                line(74f, 0f, 74f, 125f, width = 4f, dash = floatArrayOf(4f, 6f))
                drawPath(Path().apply { moveTo(45f, 52f); lineTo(58f, 60f); lineTo(45f, 68f); close() }, accent)
            }
            "concert" -> {
                val faded = accent.copy(alpha = 0.3f)
                listOf(14f to 54f, 26f to 34f, 38f to 46f, 62f to 40f, 74f to 58f).forEach { (x, y) ->
                    fillBox(x, y, 7f, 94f - y, 3.5f, faded)
                }
                fillBox(50f, 22f, 7f, 72f, 3.5f)
            }
            "okey" -> {
                // A rack of okey tiles and the die used to choose the opening tile.
                box(9f, 59f, 82f, 18f, 3f, width = 2f)
                for (index in 0..3) {
                    val x = 16f + index * 18f
                    fillBox(x, 25f, 15f, 38f, 3f, paper)
                    box(x, 25f, 15f, 38f, 3f, width = 1.5f)
                    val ink = if (index % 2 == 0) accent else Color(0xFFD06B37)
                    line(x + 7.5f, 34f, x + 7.5f, 44f, ink, 2f)
                    dot(x + 7.5f, 53f, 2f, ink)
                }
                rotate(12f, Offset(73f, 84f)) {
                    p.fillBox(59f, 70f, 28f, 28f, 5f)
                    for (x in listOf(66f, 80f)) for (y in listOf(77f, 84f, 91f)) p.dot(x, y, 2f, p.paper)
                }
            }
            "board-games" -> {
                val tint = accent.copy(alpha = 0.14f)
                for (row in 0..3) for (column in 0..3) {
                    if ((row + column) % 2 == 0) fillBox(18f + column * 16f, 16f + row * 16f, 16f, 16f, color = tint)
                }
                box(18f, 16f, 64f, 64f)
                // The die sits on the board's corner, clear of the time in the bottom left.
                rotate(14f, Offset(80f, 18f)) {
                    p.fillBox(70f, 8f, 20f, 20f, 5f)
                    listOf(75f to 13f, 80f to 18f, 85f to 23f).forEach { (x, y) -> p.dot(x, y, 1.8f, p.paper) }
                }
            }
            "video-games" -> {
                val grid = accent.copy(alpha = 0.2f)
                for (x in 0..9) for (y in 0..12) box(1f + x * 10f, 1f + y * 10f, 8f, 8f, color = grid, width = 0.8f)
                val pad = Path().apply {
                    moveTo(20f, 50f); lineTo(32f, 50f); lineTo(32f, 38f); lineTo(44f, 38f); lineTo(44f, 50f); lineTo(56f, 50f)
                    lineTo(56f, 62f); lineTo(44f, 62f); lineTo(44f, 74f); lineTo(32f, 74f); lineTo(32f, 62f); lineTo(20f, 62f); close()
                }
                outline(pad, accent.copy(alpha = 0.65f), 1.8f)
                dot(72f, 50f, 5f); dot(82f, 60f, 5f, accent.copy(alpha = 0.5f))
            }
            "music" -> {
                listOf(36f, 46f, 56f, 66f, 76f).forEach { line(0f, it, 100f, it) }
                listOf(Triple(28f, 71f, 32.5f), Triple(50f, 56f, 54.5f), Triple(72f, 41f, 76.5f)).forEach { (x, y, stem) ->
                    rotate(-20f, Offset(x, y)) { drawOval(p.accent, Offset(x - 5f, y - 4f), Size(10f, 8f)) }
                    line(stem, y - 1f, stem, y - 27f, accent, 1.6f)
                }
            }
            "trip" -> {
                outline(Path().apply { moveTo(14f, 104f); cubicTo(34f, 84f, 18f, 64f, 44f, 58f); cubicTo(70f, 52f, 72f, 40f, 66f, 30f) },
                    width = 1.8f, dash = floatArrayOf(4f, 4f))
                ring(14f, 104f, 4f)
                outline(Path().apply { moveTo(0f, 20f); quadraticTo(30f, 30f, 50f, 14f); quadraticTo(70f, -2f, 100f, 18f) })
                outline(Path().apply { moveTo(0f, 116f); quadraticTo(40f, 100f, 70f, 112f); quadraticTo(100f, 124f, 100f, 104f) })
                dot(66f, 15f, 9f)
                drawPath(Path().apply { moveTo(58f, 19f); lineTo(66f, 32f); lineTo(74f, 19f); close() }, accent)
                dot(66f, 15f, 3.5f, paper)
            }
            "language-exchange" -> {
                box(10f, 22f, 54f, 36f, 14f)
                outline(Path().apply { moveTo(22f, 58f); lineTo(18f, 68f); lineTo(30f, 58f) })
                fillBox(36f, 54f, 54f, 36f, 14f)
                drawPath(Path().apply { moveTo(78f, 90f); lineTo(82f, 100f); lineTo(70f, 90f); close() }, accent)
                listOf(52f, 63f, 74f).forEach { dot(it, 72f, 2.6f, paper) }
            }
            "study" -> {
                var y = 24f
                while (y <= 108f) { line(0f, y, 100f, y); y += 12f }
                line(20f, 0f, 20f, 125f); fillBox(28f, 54f, 44f, 7f, 2f, accent.copy(alpha = 0.75f))
            }
            "volunteering" -> {
                ring(50f, 56f, 30f)
                listOf(50f to 26f, 71.2f to 34.8f, 80f to 56f, 71.2f to 77.2f, 50f to 86f, 28.8f to 77.2f, 20f to 56f, 28.8f to 34.8f)
                    .forEach { (x, y) -> ring(x, y, 5f, accent.copy(alpha = 0.6f), 1.6f) }
                drawPath(Path().apply {
                    moveTo(50f, 66f); cubicTo(38f, 58f, 40f, 48f, 46f, 48f); cubicTo(48f, 48f, 50f, 50f, 50f, 52f)
                    cubicTo(50f, 50f, 52f, 48f, 54f, 48f); cubicTo(60f, 48f, 62f, 58f, 50f, 66f); close()
                }, accent)
            }
            else -> {
                dotGrid(accent.copy(alpha = 0.4f)); dot(54f, 54f, 6f)
            }
        }
    }
}

/** Absolute M/C/Z path data only, as used by the contour lines above. */
private fun svgPath(data: String): Path {
    val path = Path()
    val tokens = Regex("[MCZ]|-?\\d+(?:\\.\\d+)?").findAll(data).map { it.value }.toList()
    var index = 0
    fun number() = tokens[index++].toFloat()
    while (index < tokens.size) {
        when (tokens[index++]) {
            "M" -> path.moveTo(number(), number())
            "C" -> {
                do path.cubicTo(number(), number(), number(), number(), number(), number())
                while (index < tokens.size && tokens[index].first().let { it.isDigit() || it == '-' })
            }
            "Z" -> path.close()
        }
    }
    return path
}
