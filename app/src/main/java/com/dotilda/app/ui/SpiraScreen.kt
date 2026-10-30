package com.dotilda.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.dotilda.app.SpiraTimer
import com.dotilda.app.data.Prefs
import com.dotilda.app.tap
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SpiraMode { Breathe, Timer, Stopwatch }

private const val BREATHS = 6
private const val INHALE_MS = 4_000L
private const val EXHALE_MS = 6_000L

/**
 * spira, "breathe": a breathing minute before writing, a timer and a stopwatch, which the
 * Kompakt lacks. Everything moves in steps, never smoothly.
 */
@Composable
fun SpiraScreen(prefs: Prefs, start: SpiraMode, onWrite: (() -> Unit)?, onBack: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(start) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(stringResource(R.string.spira), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                SpiraMode.Breathe to R.string.spira_breathe,
                SpiraMode.Timer to R.string.spira_timer,
                SpiraMode.Stopwatch to R.string.spira_stopwatch,
            ).forEach { (m, label) ->
                TextMMD(
                    stringResource(label),
                    modifier = Modifier.clickable { mode = m }.padding(vertical = 8.dp),
                    fontWeight = if (m == mode) FontWeight.Bold else null,
                    textDecoration = if (m == mode) TextDecoration.Underline else null,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        val rest = Modifier.weight(1f).fillMaxWidth()
        when (mode) {
            SpiraMode.Breathe -> Breathe(onWrite, rest)
            SpiraMode.Timer -> Timer(prefs, rest)
            SpiraMode.Stopwatch -> Stopwatch(rest)
        }
        Spacer(Modifier.height(12.dp))
        ActionRow(back = stringResource(R.string.back), onBack = onBack)
    }
}

private enum class Breath { Ready, In, Out, Done }

/** One minute: 6 breaths of 4 s in and 6 s out. The circle has three fixed sizes, with a tap at each change. */
@Composable
private fun Breathe(onWrite: (() -> Unit)?, modifier: Modifier) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf(Breath.Ready) }
    var count by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        for (i in 1..BREATHS) {
            count = i
            phase = Breath.In
            context.tap()
            delay(INHALE_MS)
            phase = Breath.Out
            context.tap()
            delay(EXHALE_MS)
        }
        phase = Breath.Done
        context.tap()
        running = false
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val fraction = when (phase) {
                Breath.In -> 1f
                Breath.Out -> 0.45f
                else -> 0.7f
            }
            Canvas(Modifier.size(200.dp)) {
                val r = size.minDimension / 2 * fraction
                if (phase == Breath.In) drawCircle(Color.Black, r) else drawCircle(Color.Black, r, style = Stroke(width = 6f))
            }
        }
        val text = when (phase) {
            Breath.Ready -> stringResource(R.string.breathe_ready)
            Breath.In -> stringResource(R.string.breathe_in, count, BREATHS)
            Breath.Out -> stringResource(R.string.breathe_out, count, BREATHS)
            Breath.Done -> stringResource(R.string.breathe_done)
        }
        TextMMD(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        when {
            phase == Breath.Done && onWrite != null -> ButtonMMD(onClick = onWrite, modifier = Modifier.fillMaxWidth()) {
                TextMMD(stringResource(R.string.breathe_write))
            }
            !running -> ButtonMMD(
                onClick = {
                    phase = Breath.Ready
                    running = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextMMD(stringResource(if (phase == Breath.Done) R.string.breathe_again else R.string.breathe_start))
            }
        }
    }
}

private val clock = DateTimeFormatter.ofPattern("HH:mm")

/** Big +1 / +5 minute buttons; rings through an alarm, so the screen can sleep. Updates once a minute. */
@Composable
private fun Timer(prefs: Prefs, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val end by prefs.timerEnd.collectAsState(initial = null)
    var minutes by rememberSaveable { mutableIntStateOf(5) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val running = end?.let { it > now } == true

    LaunchedEffect(end) {
        while (true) {
            now = System.currentTimeMillis()
            val e = end ?: break
            if (e <= now) break
            delay(minOf(60_000L - now % 60_000L, e - now))
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        if (running) {
            val left = ((end!! - now + 59_999) / 60_000).toInt()
            TextMMD(stringResource(R.string.timer_left, left), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            val at = Instant.ofEpochMilli(end!!).atZone(ZoneId.systemDefault()).format(clock)
            TextMMD(stringResource(R.string.timer_rings_at, at), style = MaterialTheme.typography.bodyMedium)
        } else {
            TextMMD(stringResource(R.string.timer_minutes, minutes), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("−1" to -1, "+1" to 1, "+5" to 5).forEachIndexed { i, (label, delta) ->
                    if (i > 0) Spacer(Modifier.width(8.dp))
                    OutlinedButtonMMD(onClick = { minutes = (minutes + delta).coerceIn(1, 180) }, modifier = Modifier.weight(1f)) {
                        TextMMD(label)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        ButtonMMD(
            onClick = {
                scope.launch {
                    if (running) {
                        SpiraTimer.cancel(context)
                        prefs.setTimerEnd(null)
                    } else {
                        val at = System.currentTimeMillis() + minutes * 60_000L
                        SpiraTimer.start(context, at)
                        prefs.setTimerEnd(at)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextMMD(stringResource(if (running) R.string.timer_cancel else R.string.timer_start))
        }
    }
}

/** Start, stop, laps. Refreshes each second in fast mode, each minute otherwise, to spare the battery. */
@Composable
private fun Stopwatch(modifier: Modifier) {
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    var banked by rememberSaveable { mutableLongStateOf(0L) }
    var fast by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val laps = remember { mutableStateListOf<Long>() }
    val running = startedAt != 0L
    val elapsed = banked + if (running) now - startedAt else 0L

    LaunchedEffect(running, fast) {
        while (running) {
            now = System.currentTimeMillis()
            val step = if (fast) 1_000L else 60_000L
            delay(step - (now - startedAt) % step)
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        TextMMD(format(elapsed, fast || !running), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        laps.takeLast(4).forEachIndexed { i, lap ->
            TextMMD(
                stringResource(R.string.stopwatch_lap, laps.size - minOf(4, laps.size) + i + 1, format(lap, true)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(stringResource(R.string.stopwatch_fast), modifier = Modifier.weight(1f))
            SwitchMMD(checked = fast, onCheckedChange = { fast = it })
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            OutlinedButtonMMD(
                onClick = {
                    if (running) {
                        laps += System.currentTimeMillis() - startedAt + banked
                    } else {
                        banked = 0L
                        laps.clear()
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                TextMMD(stringResource(if (running) R.string.stopwatch_lap_button else R.string.stopwatch_reset))
            }
            Spacer(Modifier.width(12.dp))
            ButtonMMD(
                onClick = {
                    val t = System.currentTimeMillis()
                    if (running) {
                        banked += t - startedAt
                        startedAt = 0L
                    } else {
                        startedAt = t
                    }
                    now = t
                },
                modifier = Modifier.weight(1f),
            ) {
                TextMMD(stringResource(if (running) R.string.stopwatch_stop else R.string.stopwatch_start))
            }
        }
    }
}

/** "12:05" with seconds, or "12 min" when only minutes are shown. */
private fun format(ms: Long, seconds: Boolean): String {
    val total = ms / 1000
    return if (seconds) "%d:%02d".format(total / 60, total % 60) else "${total / 60} min"
}
