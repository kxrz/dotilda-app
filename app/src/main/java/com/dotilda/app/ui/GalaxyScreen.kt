package com.dotilda.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotilda.app.R
import com.dotilda.app.data.Entry
import com.dotilda.app.data.Journal
import com.dotilda.app.data.SkyLayout
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

private val LABEL_WIDTH = 18.dp

/** A year on one page: 12 rows (months) of up to 31 dots (days). The volume buttons turn the years. */
@Composable
fun GalaxyScreen(
    journal: Journal,
    onOpenDay: (LocalDate) -> Unit,
    onOpenMonth: (YearMonth) -> Unit,
    onOpenSeason: (YearMonth) -> Unit,
    onBack: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var firstYear by remember { mutableIntStateOf(today.year) }
    LaunchedEffect(Unit) { journal.firstDate()?.let { firstYear = minOf(it.year, today.year) } }
    val entries by remember(year) { journal.entriesOfYear(year) }.collectAsState(initial = emptyList())
    val allConstellations by remember { journal.constellations }.collectAsState(initial = emptyList())
    val constellations = allConstellations.filter { c -> c.dates.any { it.startsWith("$year-") } }
    val turn: (Int) -> Unit = { year = (year + it).coerceIn(firstYear, today.year) }
    PageKeysEffect(turn)

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextMMD(stringResource(R.string.galaxy), modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
            TextMMD(year.toString(), style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        val stars = remember(constellations) { constellations.flatMap { it.dates }.toSet() }
        YearGrid(year, entries, stars, today, onOpenDay, onOpenMonth, Modifier.weight(1f).fillMaxWidth())
        // The year's seasons, each opening its review (only those already begun).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (q in 0 until 4) {
                val first = YearMonth.of(year, q * 3 + 1)
                if (first.atDay(1).isAfter(today)) continue
                TextLink(
                    "${first.month.getDisplayName(DateTextStyle.SHORT, Locale.ENGLISH)}–${first.plusMonths(2).month.getDisplayName(DateTextStyle.SHORT, Locale.ENGLISH)}",
                    { onOpenSeason(first) },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        TextMMD(
            if (constellations.isEmpty()) stringResource(R.string.galaxy_hint)
            else stringResource(R.string.galaxy_constellations, constellations.map { it.name }.distinct().joinToString(", ")),
            style = MaterialTheme.typography.labelSmall,
        )
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

/** The month initials (each opens the month's review) beside the year's dots. */
@Composable
private fun YearGrid(
    year: Int,
    entries: List<Entry>,
    stars: Set<String>,
    today: LocalDate,
    onOpenDay: (LocalDate) -> Unit,
    onOpenMonth: (YearMonth) -> Unit,
    modifier: Modifier,
) {
    val shapes = remember(entries) { entries.groupBy { it.date }.mapValues { it.value.first().dotShape(today) } }
    val style = LocalSkyStyle.current

    Row(modifier) {
        Column(Modifier.width(LABEL_WIDTH).fillMaxHeight()) {
            for (m in 1..12) {
                val month = YearMonth.of(year, m)
                val name = month.month.getDisplayName(DateTextStyle.FULL, Locale.ENGLISH)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable(enabled = !month.atDay(1).isAfter(today)) { onOpenMonth(month) }
                        .semantics { contentDescription = name },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    TextMMD(month.month.getDisplayName(DateTextStyle.NARROW, Locale.ENGLISH), fontSize = 11.sp)
                }
            }
        }
        Canvas(
            Modifier.weight(1f).fillMaxHeight().pointerInput(year, shapes) {
                detectTapGestures { tap ->
                    val cell = size.width / 31f
                    val row = size.height / 12f
                    val month = (tap.y / row).toInt() + 1
                    val day = (tap.x / cell).toInt() + 1
                    if (month !in 1..12) return@detectTapGestures
                    val ym = YearMonth.of(year, month)
                    if (day in 1..ym.lengthOfMonth()) {
                        val date = ym.atDay(day)
                        if (shapes.containsKey(date.toString())) onOpenDay(date)
                    }
                }
            },
        ) {
            val cell = size.width / 31
            val row = size.height / 12
            val radius = minOf(cell, row) * style.dots.galaxy
            for (m in 1..12) {
                val month = Month.of(m)
                val y = row * (m - 0.5f)
                for (d in 1..month.length(java.time.Year.isLeap(year.toLong()))) {
                    val date = LocalDate.of(year, m, d)
                    if (date.isAfter(today)) break
                    val jitter = if (style.layout == SkyLayout.NightSky) nightOffset(date.toString()) else Offset.Zero
                    val center = Offset(cell * (d - 0.5f + jitter.x), y + minOf(cell, row) * jitter.y * 2)
                    val shape = shapes[date.toString()]
                    if (shape != null) drawDot(shape, center, radius) else drawRestDot(center, radius)
                    if (shape != null && date.toString() in stars) drawStarRays(center, radius)
                    if (date == today) drawTodayMark(center, radius, below = radius + 2.dp.toPx())
                }
            }
        }
    }
}
