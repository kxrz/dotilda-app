package com.dotilda.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Dotilda herself: an ink dot with a bun and round glasses. Readable down to 16 dp. */
@Composable
fun DotildaMark(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.minDimension
        val head = Offset(w * 0.5f, w * 0.6f)
        drawCircle(Color.Black, radius = w * 0.13f, center = Offset(w * 0.5f, w * 0.14f))
        drawCircle(Color.Black, radius = w * 0.36f, center = head)

        val lens = w * 0.1f
        val gap = w * 0.125f
        val stroke = Stroke(width = w * 0.04f)
        drawCircle(Color.White, lens, Offset(head.x - gap, head.y), style = stroke)
        drawCircle(Color.White, lens, Offset(head.x + gap, head.y), style = stroke)
        drawLine(
            Color.White,
            Offset(head.x - gap + lens, head.y),
            Offset(head.x + gap - lens, head.y),
            strokeWidth = stroke.width,
        )
    }
}
