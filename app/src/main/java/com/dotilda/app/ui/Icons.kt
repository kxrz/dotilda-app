package com.dotilda.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The places Dotilda's icons lead to. */
enum class Place { Notebook, Galaxy, Journeys, Spira, Settings }

/**
 * An icon in ink, drawn like Dotilda herself (fine strokes, no fill but dots), in a 48 dp
 * touch target. [label] is what screen readers say, since there is no text.
 */
@Composable
fun PlaceIcon(place: Place, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(
        modifier
            .size(48.dp)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val w = this.size.minDimension
            val line = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (place) {
                Place.Notebook -> notebook(w, line)
                Place.Galaxy -> galaxy(w)
                Place.Journeys -> journeys(w, line)
                Place.Spira -> spira(w, line)
                Place.Settings -> settings(w, line)
            }
        }
    }
}

/** An open book: two pages from the spine, a few lines of writing. */
private fun DrawScope.notebook(w: Float, line: Stroke) {
    val c = w / 2
    val book = Path().apply {
        moveTo(c, w * 0.26f)
        lineTo(w * 0.06f, w * 0.18f)
        lineTo(w * 0.06f, w * 0.8f)
        lineTo(c, w * 0.88f)
        lineTo(w * 0.94f, w * 0.8f)
        lineTo(w * 0.94f, w * 0.18f)
        close()
        moveTo(c, w * 0.26f)
        lineTo(c, w * 0.88f)
    }
    drawPath(book, Color.Black, style = line)
    for (y in listOf(0.4f, 0.54f, 0.68f)) {
        drawLine(Color.Black, Offset(w * 0.18f, w * y), Offset(w * 0.38f, w * (y + 0.025f)), line.width * 0.7f, StrokeCap.Round)
        drawLine(Color.Black, Offset(w * 0.62f, w * (y + 0.025f)), Offset(w * 0.82f, w * y), line.width * 0.7f, StrokeCap.Round)
    }
}

/** A handful of dots of every size, the sky of the year. */
private fun DrawScope.galaxy(w: Float) {
    listOf(
        Triple(0.22f, 0.28f, 0.1f), Triple(0.6f, 0.2f, 0.06f), Triple(0.82f, 0.46f, 0.09f),
        Triple(0.4f, 0.55f, 0.13f), Triple(0.18f, 0.8f, 0.06f), Triple(0.7f, 0.8f, 0.07f),
    ).forEach { (x, y, r) -> drawCircle(Color.Black, w * r, Offset(w * x, w * y)) }
}

/** A star at the end of a little path of dots: a journey and its constellation. */
private fun DrawScope.journeys(w: Float, line: Stroke) {
    listOf(0.14f to 0.86f, 0.3f to 0.7f, 0.46f to 0.56f).forEach { (x, y) -> drawCircle(Color.Black, w * 0.06f, Offset(w * x, w * y)) }
    val star = Offset(w * 0.72f, w * 0.3f)
    drawCircle(Color.Black, w * 0.1f, star)
    for ((dx, dy) in listOf(0f to -1f, 1f to 0f, 0f to 1f, -1f to 0f)) {
        drawLine(Color.Black, star + Offset(dx, dy) * (w * 0.16f), star + Offset(dx, dy) * (w * 0.27f), line.width, StrokeCap.Round)
    }
}

/** Breathing: a ring around a smaller ring, around a dot. */
private fun DrawScope.spira(w: Float, line: Stroke) {
    val c = Offset(w / 2, w / 2)
    drawCircle(Color.Black, w * 0.44f, c, style = line)
    drawCircle(Color.Black, w * 0.26f, c, style = line)
    drawCircle(Color.Black, w * 0.08f, c)
}

/** Three sliders, set at different places. */
private fun DrawScope.settings(w: Float, line: Stroke) {
    listOf(0.22f to 0.66f, 0.5f to 0.3f, 0.78f to 0.55f).forEach { (y, knob) ->
        drawLine(Color.Black, Offset(w * 0.08f, w * y), Offset(w * 0.92f, w * y), line.width, StrokeCap.Round)
        drawCircle(Color.White, w * 0.1f, Offset(w * knob, w * y))
        drawCircle(Color.Black, w * 0.1f, Offset(w * knob, w * y), style = line)
    }
}
