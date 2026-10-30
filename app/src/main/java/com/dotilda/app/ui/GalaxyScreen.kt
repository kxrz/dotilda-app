package com.dotilda.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotilda.app.R
import com.dotilda.app.data.Entry
import com.dotilda.app.data.Journal
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

private val LABEL_WIDTH = 18.dp

/** A year on one page: 12 rows (months) of up to 31 dots (days). The volume buttons turn the years. */
@Composable
fun GalaxyScreen(journal: Journal, onOpenDay: (LocalDate) -> Unit, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var firstYear by remember { mutableIntStateOf(today.year) }
    LaunchedEffect(Unit) { journal.firstDate()?.let { firstYear = minOf(it.year, today.year) } }
    val entries by remember(year) { journal.entriesOfYear(year) }.collectAsState(initial = emptyList())
    val turn: (Int) -> Unit = { year = (year + it).coerceIn(firstYear, today.year) }
    PageKeysEffect(turn)

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextMMD(stringResource(R.string.galaxy), modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
            TextMMD(year.toString(), style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        YearGrid(year, entries, today, onOpenDay, Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TextMMD(stringResource(R.string.galaxy_hint), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(12.dp))
        PagerRow(
            canPrevious = year > firstYear,
            canNext = year < today.year,
            onPage = turn,
            onBack = onBack,
            previous = if (year > firstYear) "‹ ${year - 1}" else "‹",
            next = if (year < today.year) "${year + 1} ›" else "›",
        )
    }
}

@Composable
private fun YearGrid(year: Int, entries: List<Entry>, today: LocalDate, onOpenDay: (LocalDate) -> Unit, modifier: Modifier) {
    val shapes = remember(entries) { entries.groupBy { it.date }.mapValues { it.value.first().dotShape() } }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color.Black, fontSize = 11.sp)

    Canvas(
        modifier.pointerInput(year, shapes) {
            detectTapGestures { tap ->
                val label = LABEL_WIDTH.toPx()
                val cell = (size.width - label) / 31
                val row = size.height / 12f
                val month = (tap.y / row).toInt() + 1
                val day = ((tap.x - label) / cell).toInt() + 1
                if (tap.x < label || month !in 1..12) return@detectTapGestures
                val ym = java.time.YearMonth.of(year, month)
                if (day in 1..ym.lengthOfMonth()) {
                    val date = ym.atDay(day)
                    if (shapes.containsKey(date.toString())) onOpenDay(date)
                }
            }
        },
    ) {
        val label = LABEL_WIDTH.toPx()
        val cell = (size.width - label) / 31
        val row = size.height / 12
        val radius = minOf(cell, row) * DOT_FILL
        for (m in 1..12) {
            val month = Month.of(m)
            val y = row * (m - 0.5f)
            val initial = measurer.measure(month.getDisplayName(DateTextStyle.NARROW, Locale.ENGLISH), labelStyle)
            drawText(initial, topLeft = Offset(0f, y - initial.size.height / 2f))
            for (d in 1..month.length(java.time.Year.isLeap(year.toLong()))) {
                val date = LocalDate.of(year, m, d)
                if (date.isAfter(today)) break
                val center = Offset(label + cell * (d - 0.5f), y)
                val shape = shapes[date.toString()]
                if (shape != null) drawDot(shape, center, radius) else drawRestDot(center, radius)
                if (date == today) drawTodayMark(center, radius, below = radius + 2.dp.toPx())
            }
        }
    }
}
