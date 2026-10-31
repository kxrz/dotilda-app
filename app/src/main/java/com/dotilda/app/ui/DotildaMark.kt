package com.dotilda.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dotilda's expressions. On e-ink she never moves smoothly: she changes pose in one step,
 * which costs a single redraw, like turning a page.
 */
enum class Pose {
    Neutral,

    /** Glasses slid down her nose: she is asking. */
    Peering,

    /** Glancing aside over her glasses: a little mischief, for "another question". */
    Curious,

    /** A small smile: something was written. */
    Pleased,

    /** One eye closed behind her glasses: only when poked. */
    Wink,

    /** Round eyes, a little "o": only when poked. */
    Surprised,

    /** Eyes closed, a nap behind the glasses: only when poked. */
    Sleepy,
}

/** What a poke shows, one after the other: a hidden game, one step per tap, never animated. */
private val PokePoses = listOf(Pose.Wink, Pose.Surprised, Pose.Curious, Pose.Sleepy, Pose.Pleased)

/** Dotilda herself: an ink dot with a bun and round glasses. Readable down to 16 dp. */
@Composable
fun DotildaMark(modifier: Modifier = Modifier, size: Dp = 48.dp, pose: Pose = Pose.Neutral) {
    Canvas(modifier.size(size)) {
        val w = this.size.minDimension
        val head = Offset(w * 0.5f, w * 0.6f)
        drawCircle(Color.Black, radius = w * 0.13f, center = Offset(w * 0.5f, w * 0.14f))
        drawCircle(Color.Black, radius = w * 0.36f, center = head)

        val lens = w * if (pose == Pose.Surprised) 0.115f else 0.1f
        val gap = w * 0.125f
        val line = w * 0.04f
        val drop = if (pose == Pose.Peering) w * 0.08f else 0f
        // Shifted sideways rather than tilted: tilted lenses read as "∞" at small sizes.
        val glance = if (pose == Pose.Curious) Offset(w * 0.06f, -w * 0.03f) else Offset.Zero
        val left = Offset(head.x - gap, head.y + drop) + glance
        val right = Offset(head.x + gap, head.y + drop) + glance
        drawCircle(Color.White, lens, left, style = Stroke(width = line))
        drawCircle(Color.White, lens, right, style = Stroke(width = line))
        drawLine(Color.White, left + Offset(lens, 0f), right - Offset(lens, 0f), strokeWidth = line)

        // Closed eyes: a small curve inside the lens, like an eyelid.
        fun closedEye(center: Offset) = drawArc(
            Color.White,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - lens * 0.6f, center.y - lens * 0.55f),
            size = Size(lens * 1.2f, lens * 0.8f),
            style = Stroke(width = line, cap = StrokeCap.Round),
        )
        when (pose) {
            Pose.Wink -> closedEye(left)
            Pose.Sleepy -> {
                closedEye(left)
                closedEye(right)
            }
            Pose.Surprised -> drawCircle(Color.White, w * 0.035f, Offset(head.x, head.y + w * 0.17f), style = Stroke(width = line))
            else -> Unit
        }

        if (pose == Pose.Pleased || pose == Pose.Wink) {
            val smile = w * 0.09f
            drawArc(
                Color.White,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(head.x - smile, head.y + w * 0.06f),
                size = Size(smile * 2, smile * 1.4f),
                style = Stroke(width = line, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Dotilda, who answers a poke: each tap shows another face, in one step. [onPoke] gets the
 * number of taps so far, for the places where she also has something to say about it.
 */
@Composable
fun PlayfulDotilda(size: Dp, pose: Pose, modifier: Modifier = Modifier, onPoke: (Int) -> Unit = {}) {
    var pokes by remember { mutableIntStateOf(0) }
    val shown = if (pokes == 0) pose else PokePoses[(pokes - 1) % PokePoses.size]
    DotildaMark(
        modifier
            .testTag("dotilda")
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                pokes++
                onPoke(pokes)
            },
        size = size,
        pose = shown,
    )
}
