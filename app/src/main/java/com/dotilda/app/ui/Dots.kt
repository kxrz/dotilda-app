package com.dotilda.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dotilda.app.data.Answer
import com.dotilda.app.data.Entry
import com.dotilda.app.data.answer
import java.time.LocalDate
import java.time.YearMonth

private val Ink = Color.Black
private val Faint = Color(0xFF9E9E9E)

/** How a day looks in the galaxy. */
sealed interface DotShape {
    data object Full : DotShape

    /** Inner weather as a moon phase: 0 (sunny) is a full dot, 4 (stormy) a bare ring. */
    data class Moon(val level: Int) : DotShape
}

fun Entry.dotShape(): DotShape = when (val a = answer()) {
    is Answer.Weather -> DotShape.Moon(a.level)
    else -> DotShape.Full
}

fun DrawScope.drawDot(shape: DotShape, center: Offset, radius: Float) {
    when (shape) {
        DotShape.Full -> drawCircle(Ink, radius, center)
        is DotShape.Moon -> {
            val lit = 1f - shape.level / 4f
            val circle = Path().apply { addOval(Rect(center, radius)) }
            clipPath(circle) {
                drawRect(Ink, topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(2 * radius * lit, 2 * radius))
            }
            drawCircle(Ink, radius - radius * 0.1f, center, style = Stroke(width = radius * 0.2f))
        }
    }
}

/** A day with no entry: a tiny grey dot, a breath in the grid, never a hole. */
fun DrawScope.drawRestDot(center: Offset, radius: Float) {
    drawCircle(Faint, radius * 0.3f, center)
}

/** Today, still open: an empty ring. */
fun DrawScope.drawOpenDot(center: Offset, radius: Float) {
    drawCircle(Ink, radius - radius * 0.1f, center, style = Stroke(width = radius * 0.2f))
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
    val shapes = entries.groupBy { it.date }.mapValues { it.value.first().dotShape() }
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val cell = size.width / 31
        val radius = minOf(cell, size.height) * 0.4f
        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            if (date.isAfter(today)) break
            val center = Offset(cell * (day - 0.5f), size.height / 2)
            val shape = shapes[date.toString()]
            when {
                shape != null -> drawDot(shape, center, radius)
                date == today -> drawOpenDot(center, radius)
                else -> drawRestDot(center, radius)
            }
        }
    }
}
