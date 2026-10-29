package com.dotilda.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Inner weather pictos, 0 = sunny … 4 = stormy. Static black shapes, no animation. */
@Composable
fun WeatherIcon(level: Int, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.minDimension
        val line = w * 0.06f
        when (level) {
            0 -> sun(Offset(w * 0.5f, w * 0.5f), w * 0.2f, line)
            1 -> {
                sun(Offset(w * 0.36f, w * 0.36f), w * 0.15f, line)
                cloud(w, dy = w * 0.12f)
            }
            2 -> cloud(w, dy = 0f)
            3 -> {
                cloud(w, dy = -w * 0.12f)
                for (x in listOf(0.32f, 0.5f, 0.68f)) {
                    drawLine(Color.Black, Offset(w * x, w * 0.66f), Offset(w * (x - 0.06f), w * 0.86f), line, StrokeCap.Round)
                }
            }
            else -> {
                cloud(w, dy = -w * 0.12f)
                val bolt = Path().apply {
                    moveTo(w * 0.56f, w * 0.6f)
                    lineTo(w * 0.42f, w * 0.78f)
                    lineTo(w * 0.54f, w * 0.78f)
                    lineTo(w * 0.44f, w * 0.96f)
                }
                drawPath(bolt, Color.Black, style = Stroke(width = line, cap = StrokeCap.Round))
            }
        }
    }
}

private fun DrawScope.sun(center: Offset, radius: Float, line: Float) {
    drawCircle(Color.Black, radius, center, style = Stroke(width = line))
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
        drawLine(Color.Black, center + dir * (radius * 1.4f), center + dir * (radius * 1.9f), line, StrokeCap.Round)
    }
}

/** A filled cloud: three overlapping circles on a rounded base. */
private fun DrawScope.cloud(w: Float, dy: Float) {
    val base = w * 0.62f + dy
    drawCircle(Color.Black, w * 0.13f, Offset(w * 0.33f, base - w * 0.08f))
    drawCircle(Color.Black, w * 0.18f, Offset(w * 0.52f, base - w * 0.15f))
    drawCircle(Color.Black, w * 0.12f, Offset(w * 0.7f, base - w * 0.07f))
    drawRoundRect(
        Color.Black,
        topLeft = Offset(w * 0.2f, base - w * 0.1f),
        size = Size(w * 0.62f, w * 0.14f),
        cornerRadius = CornerRadius(w * 0.07f),
    )
}
