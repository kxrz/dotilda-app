package com.dotilda.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD

/** Where the bubble's little tail points: up to Dotilda above it, or left to Dotilda beside it. */
enum class Tail { Up, Left }

/**
 * What Dotilda says, in a light speech bubble: a fine ink outline, round corners and a small
 * tail towards her. Drawn once, like everything else, and white inside.
 * [tailAt] is the tail's distance from the bubble's start (Up) or top (Left); null centres it.
 */
@Composable
fun SpeechBubble(
    modifier: Modifier = Modifier,
    tail: Tail = Tail.Up,
    tailAt: Dp? = 16.dp,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable () -> Unit,
) {
    val tailSize = 7.dp
    Box(
        modifier
            .padding(top = if (tail == Tail.Up) tailSize else 0.dp, start = if (tail == Tail.Left) tailSize else 0.dp)
            .drawBehind {
                val line = 1.5.dp.toPx()
                val corner = CornerRadius(14.dp.toPx())
                val t = tailSize.toPx()
                drawRoundRect(Color.White, cornerRadius = corner)
                drawRoundRect(
                    Color.Black,
                    topLeft = Offset(line / 2, line / 2),
                    size = Size(size.width - line, size.height - line),
                    cornerRadius = corner,
                    style = Stroke(line),
                )
                val at = tailAt?.toPx() ?: (if (tail == Tail.Up) size.width else size.height) / 2
                // Points of the tail: its two feet on the outline, and its tip outside.
                val (a, tip, b) = if (tail == Tail.Up) {
                    Triple(Offset(at - t, line / 2), Offset(at, -t), Offset(at + t, line / 2))
                } else {
                    Triple(Offset(line / 2, at - t), Offset(-t, at), Offset(line / 2, at + t))
                }
                // White over the outline between the feet, then the two sides of the tail.
                val inward = if (tail == Tail.Up) Offset(0f, line) else Offset(line, 0f)
                drawPath(Path().apply { moveTo(a.x + inward.x, a.y + inward.y); lineTo(tip.x, tip.y); lineTo(b.x + inward.x, b.y + inward.y); close() }, Color.White)
                drawPath(
                    Path().apply { moveTo(a.x, a.y); lineTo(tip.x, tip.y); lineTo(b.x, b.y) },
                    Color.Black,
                    style = Stroke(line, join = StrokeJoin.Round),
                )
            }
            .padding(padding),
    ) {
        content()
    }
}

/** "Dotilda asks", then her question in a bubble whose tail points up at her. */
@Composable
fun DotildaQuestion(text: String, style: TextStyle, pose: Pose = Pose.Peering, modifier: Modifier = Modifier) {
    Column(modifier) {
        DotildaAsks(pose = pose)
        Spacer(Modifier.height(4.dp))
        SpeechBubble(tail = Tail.Up, tailAt = 16.dp) { TextMMD(text, style = style) }
    }
}

/** Dotilda (small, on the left) with a short aside in a bubble beside her. */
@Composable
fun DotildaBeside(pose: Pose, markSize: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        PlayfulDotilda(size = markSize, pose = pose)
        Spacer(Modifier.width(6.dp))
        SpeechBubble(tail = Tail.Left, tailAt = null, padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), content = content)
    }
}
