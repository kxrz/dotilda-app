package com.dotilda.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.dotilda.app.Photos
import com.dotilda.app.R
import com.dotilda.app.data.Answer
import com.dotilda.app.data.Entry
import com.dotilda.app.data.Journal
import com.dotilda.app.data.Trend
import com.dotilda.app.data.answer
import com.dotilda.app.data.choiceTallies
import com.dotilda.app.data.weatherTrend
import com.dotilda.app.data.wordCloud
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * The month looking back, on two pages: Dotilda's word on its weather, the weather frieze and
 * the words; then a line of yours and the contact sheet of photos.
 */
@Composable
fun ReviewScreen(
    journal: Journal,
    first: YearMonth,
    months: Int,
    onOpenDay: (LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    val last = first.plusMonths(months - 1L)
    val loaded by remember(first, months) { journal.entriesBetween(first.atDay(1), last.atEndOfMonth()) }.collectAsState(initial = null)
    val entries = loaded ?: return
    val season = months > 1
    // A month on two pages; a season on three, with its This-or-that tallies.
    val pages: List<@Composable () -> Unit> = if (season) {
        listOf(
            { Weather(first, months, entries) },
            { WordsAndChoices(journal, entries) },
            { LineAndPhotos(entries, onOpenDay) },
        )
    } else {
        listOf(
            {
                Weather(first, months, entries)
                Spacer(Modifier.height(24.dp))
                WordsAndChoices(journal, entries, choices = false)
            },
            { LineAndPhotos(entries, onOpenDay) },
        )
    }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val turn: (Int) -> Unit = { page = (page + it).coerceIn(0, pages.lastIndex) }
    PageKeysEffect(turn)

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(
                periodTitle(first, last, appLocale()),
                modifier = Modifier.weight(1f),
                style = if (season) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge,
            )
            TextMMD("${page + 1} / ${pages.size}", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f)) { pages[page.coerceIn(0, pages.lastIndex)]() }
        PagerRow(canPrevious = page > 0, canNext = page < pages.lastIndex, onPage = turn, onBack = onBack)
    }
}

/** "September 2026", or "July – September 2026" for a season, in the app's language. */
fun periodTitle(first: YearMonth, last: YearMonth, locale: Locale): String {
    fun name(m: YearMonth) = m.month.title(locale)
    return if (first == last) "${name(first)} ${first.year}" else "${name(first)} – ${name(last)} ${last.year}"
}

@Composable
private fun Weather(first: YearMonth, months: Int, entries: List<Entry>) {
    val weather = entries.mapNotNull { e -> (e.answer() as? Answer.Weather)?.let { e.date to it.level } }
    val comments = stringArrayResource(if (months > 1) R.array.review_comments_season else R.array.review_comments)
    val comment = comments[weatherTrend(weather.map { it.second }).ordinal]
    DotildaBeside(Pose.Pleased, markSize = 32.dp) {
        TextMMD(comment, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.height(24.dp))
    TextMMD(stringResource(R.string.review_weather), style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(8.dp))
    // The frieze: each weather answer as a moon, at its day of the month, one line per month.
    for (k in 0 until months) {
        val month = first.plusMonths(k.toLong())
        if (months > 1) {
            Spacer(Modifier.height(10.dp))
            TextMMD(month.month.title(appLocale()), style = MaterialTheme.typography.labelSmall)
        }
        Canvas(Modifier.fillMaxWidth().height(16.dp)) {
            val cell = size.width / 31
            val radius = cell * 0.4f
            for ((date, level) in weather) {
                val day = LocalDate.parse(date)
                if (YearMonth.from(day) != month) continue
                drawDot(DotShape.Moon(level), Offset(cell * (day.dayOfMonth - 0.5f), size.height / 2), radius)
            }
        }
    }
}

/** The words of the period as a small cloud, and for a season the This-or-that tallies. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordsAndChoices(journal: Journal, entries: List<Entry>, choices: Boolean = true) {
    val words = wordCloud(entries.mapNotNull { (it.answer() as? Answer.Word)?.word })
    if (words.isNotEmpty()) {
        TextMMD(stringResource(R.string.review_words), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            words.forEach { (word, count) ->
                val style = when {
                    count >= 3 -> MaterialTheme.typography.headlineLarge
                    count == 2 -> MaterialTheme.typography.titleLarge
                    else -> MaterialTheme.typography.bodyMedium
                }
                TextMMD(word, style = style)
            }
        }
    }
    if (choices) {
        val tallies = remember(entries) { choiceTallies(entries) }
        val texts by produceState(emptyMap<String, String>(), tallies) {
            value = tallies.keys.associateWith { id -> journal.question(id)?.text.orEmpty() }
        }
        if (tallies.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            TextMMD(stringResource(R.string.review_choices), style = MaterialTheme.typography.labelMedium)
            tallies.entries.take(3).forEach { (id, counts) ->
                Spacer(Modifier.height(8.dp))
                TextMMD(texts[id].orEmpty(), fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodySmall)
                TextMMD(counts.joinToString("  ·  ") { (label, n) -> "$label $n" }, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun LineAndPhotos(entries: List<Entry>, onOpenDay: (LocalDate) -> Unit) {
    val context = LocalContext.current
    // A line of yours: the longest of the month, the one with the most in it.
    val line = entries.mapNotNull { (it.answer() as? Answer.Text)?.text }.maxByOrNull { it.length }
    if (line != null) {
        TextMMD(stringResource(R.string.review_line), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        TextMMD("“$line”", fontStyle = FontStyle.Italic, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
    }
    val photos = entries.filter { it.photoPath != null }.take(6)
    if (photos.isNotEmpty()) {
        TextMMD(stringResource(R.string.review_photos), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        photos.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { entry ->
                    val bitmap = remember(entry.photoPath) { Photos.load(context, entry.photoPath!!)?.asImageBitmap() }
                    bitmap?.let {
                        Image(
                            it,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).clickable { onOpenDay(LocalDate.parse(entry.date)) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (line == null && photos.isEmpty()) {
        TextMMD(stringResource(R.string.review_quiet), style = MaterialTheme.typography.bodyLarge)
    }
}
