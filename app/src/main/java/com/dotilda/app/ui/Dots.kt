package com.dotilda.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.dotilda.app.data.SkyStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dotilda.app.data.Answer
import com.dotilda.app.data.Entry
import com.dotilda.app.data.QuestionType
import com.dotilda.app.data.answer
import java.time.LocalDate
import java.time.YearMonth

private val Ink = Color.Black
private val Faint = Color(0xFF9E9E9E)

/** How a day looks in the galaxy. */
sealed interface DotShape {
    data object Full : DotShape

    /**
     * Inner weather as a moon phase, read like the real sky: 0 (sunny) a full moon,
     * 1 gibbous, 2 a half moon, 3 a crescent, 4 (stormy) a new moon, only its outline.
     */
    data class Moon(val level: Int) : DotShape

    /** Mirror: a dot inside a ring, looking back at itself. */
    data object Double : DotShape

    /** Photo of the day: a small square, like a print. */
    data object Print : DotShape

    /** Letter to the future: a diamond, hollow while sealed, inked once its day has come. */
    data class Diamond(val sealed: Boolean) : DotShape
}

fun Entry.dotShape(today: LocalDate = LocalDate.now()): DotShape = when {
    type == QuestionType.MIRROR -> DotShape.Double
    else -> when (val a = answer()) {
        is Answer.Weather -> DotShape.Moon(a.level)
        is Answer.Photo -> DotShape.Print
        is Answer.Letter -> DotShape.Diamond(sealed = a.sealedUntil > today.toString())
        else -> DotShape.Full
    }
}

/** The look of the dots and of the Galaxy, chosen in Settings. */
val LocalSkyStyle = staticCompositionLocalOf { SkyStyle() }

/**
 * In the night sky layout, how far a dot sits from its place, as a share of its cell: a fixed
 * offset per date, so the sky never shifts from one look to the next.
 */
fun nightOffset(date: String): Offset {
    val h = date.hashCode()
    return Offset(((h and 0xFF) / 255f - 0.5f) * 0.5f, ((h shr 8 and 0xFF) / 255f - 0.5f) * 0.5f)
}

fun DrawScope.drawDot(shape: DotShape, center: Offset, radius: Float) {
    val outline = Stroke(width = maxOf(1f, radius * 0.22f))
    val disc = Path().apply { addOval(Rect(center, radius)) }
    when (shape) {
        DotShape.Full -> drawCircle(Ink, radius, center)
        DotShape.Double -> {
            drawCircle(Ink, radius - outline.width / 2, center, style = outline)
            drawCircle(Ink, radius * 0.42f, center)
        }
        DotShape.Print -> {
            val side = radius * 1.7f
            drawRoundRect(
                Ink,
                topLeft = Offset(center.x - side / 2, center.y - side / 2),
                size = androidx.compose.ui.geometry.Size(side, side),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.25f),
            )
        }
        is DotShape.Diamond -> {
            val r = radius * 1.15f
            val diamond = Path().apply {
                moveTo(center.x, center.y - r)
                lineTo(center.x + r, center.y)
                lineTo(center.x, center.y + r)
                lineTo(center.x - r, center.y)
                close()
            }
            if (shape.sealed) drawPath(diamond, Ink, style = outline) else drawPath(diamond, Ink)
        }
        is DotShape.Moon -> {
            when (shape.level) {
                0 -> drawPath(disc, Ink)
                // The lit part is the disc minus a "shadow" disc, further away for a fuller moon.
                1 -> drawPath(disc - shadow(center, radius, 1.45f), Ink)
                2 -> clipRect(right = center.x) { drawPath(disc, Ink) }
                3 -> drawPath(disc - shadow(center, radius, 0.55f), Ink)
                else -> Unit
            }
            drawCircle(Ink, radius - outline.width / 2, center, style = outline)
        }
    }
}

private fun shadow(center: Offset, radius: Float, shift: Float) =
    Path().apply { addOval(Rect(Offset(center.x + radius * shift, center.y), radius)) }

private operator fun Path.minus(other: Path): Path = Path().also { it.op(this, other, PathOperation.Difference) }

/**
 * A day of a finished journey: four short rays around its dot, so the constellation shows as
 * stars. Never a line between them: the dots stay free.
 */
fun DrawScope.drawStarRays(center: Offset, radius: Float) {
    val inner = radius * 1.35f
    val outer = radius * 2f
    val width = maxOf(1f, radius * 0.3f)
    for ((dx, dy) in listOf(0f to -1f, 1f to 0f, 0f to 1f, -1f to 0f)) {
        drawLine(Ink, center + Offset(dx * inner, dy * inner), center + Offset(dx * outer, dy * outer), strokeWidth = width)
    }
}

/** A day with no entry: a tiny grey dot, a breath in the grid, never a hole. */
fun DrawScope.drawRestDot(center: Offset, radius: Float) {
    drawCircle(Faint, radius * 0.35f, center)
}

/** "Today" is marked under its dot by a small caret, so no dot shape has to mean it. */
fun DrawScope.drawTodayMark(center: Offset, radius: Float, below: Float) {
    val y = center.y + below
    val w = maxOf(radius * 0.9f, 3f)
    drawPath(
        Path().apply {
            moveTo(center.x, y)
            lineTo(center.x - w, y + w * 1.2f)
            lineTo(center.x + w, y + w * 1.2f)
            close()
        },
        Ink,
    )
}

@Composable
fun DayDot(shape: DotShape, modifier: Modifier = Modifier, size: Dp = 72.dp) {
    Canvas(modifier.size(size)) {
        drawDot(shape, center, this.size.minDimension / 2)
    }
}

/** The month on one line, one dot per day. Days to come are not drawn. */
@Composable
fun MonthLine(month: YearMonth, entries: List<Entry>, today: LocalDate, modifier: Modifier = Modifier) {
    val shapes = entries.groupBy { it.date }.mapValues { it.value.first().dotShape(today) }
    val lineFill = LocalSkyStyle.current.dots.line
    Canvas(modifier.fillMaxWidth().height(22.dp)) {
        val cell = size.width / 31
        val radius = cell * lineFill
        val y = radius + 1f
        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            if (date.isAfter(today)) break
            val center = Offset(cell * (day - 0.5f), y)
            val shape = shapes[date.toString()]
            if (shape != null) drawDot(shape, center, radius) else drawRestDot(center, radius)
            if (date == today) drawTodayMark(center, radius, below = radius + 3.dp.toPx())
        }
    }
}
