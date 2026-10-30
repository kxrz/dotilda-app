package com.dotilda.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.dotilda.app.data.Journal
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.delay

/** The day's dot, filled once, and Dotilda's brief word. */
@Composable
fun DoneScreen(journal: Journal, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    val month = remember { YearMonth.from(today) }
    val entry by remember { journal.entryOn(today) }.collectAsState(initial = null)
    val monthEntries by remember { journal.entriesOf(month) }.collectAsState(initial = emptyList())
    val constellations by remember { journal.constellations }.collectAsState(initial = emptyList())
    // A journey finished with today's answer: Dotilda names its constellation.
    val finished = constellations.lastOrNull()?.takeIf { it.dates.lastOrNull() == today.toString() }

    // One discrete step, never a loop: Dotilda pushes her glasses up and smiles.
    var pose by remember { mutableStateOf(Pose.Peering) }
    LaunchedEffect(Unit) {
        delay(700)
        pose = Pose.Pleased
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        DotildaMark(size = 56.dp, pose = pose)
        Spacer(Modifier.height(20.dp))
        if (finished != null) {
            Stars(finished.dates.size)
            Spacer(Modifier.height(28.dp))
            val count = stringArrayResource(R.array.number_words).getOrNull(finished.dates.size) ?: finished.dates.size.toString()
            TextMMD(
                stringResource(R.string.constellation_named, count, count.lowercase(), finished.name),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )
        } else {
            entry?.let { DayDot(it.dotShape(), size = 96.dp) }
            Spacer(Modifier.height(28.dp))
            TextMMD(
                stringResource(R.string.noted),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        Spacer(Modifier.height(32.dp))
        MonthLine(month, monthEntries, today)
        Spacer(Modifier.weight(1f))
        ButtonMMD(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            TextMMD(stringResource(R.string.back_to_today))
        }
    }
}

/** A finished journey's days as a little row of stars, free of any line. */
@Composable
private fun Stars(count: Int) {
    Canvas(Modifier.fillMaxWidth().height(48.dp)) {
        val cell = size.width / count
        val radius = minOf(cell * 0.18f, 7.dp.toPx())
        for (i in 0 until count) {
            val center = Offset(cell * (i + 0.5f), size.height / 2 + if (i % 2 == 0) -radius else radius)
            drawDot(DotShape.Full, center, radius)
            drawStarRays(center, radius)
        }
    }
}
