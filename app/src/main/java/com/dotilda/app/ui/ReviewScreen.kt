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
import com.dotilda.app.data.weatherTrend
import com.dotilda.app.data.wordCloud
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * The month looking back, on two pages: Dotilda's word on its weather, the weather frieze and
 * the words; then a line of yours and the contact sheet of photos.
 */
@Composable
fun ReviewScreen(journal: Journal, month: YearMonth, onOpenDay: (LocalDate) -> Unit, onBack: () -> Unit) {
    val loaded by remember(month) { journal.entriesOf(month) }.collectAsState(initial = null)
    val entries = loaded ?: return
    var page by rememberSaveable { mutableIntStateOf(0) }
    val turn: (Int) -> Unit = { page = (page + it).coerceIn(0, 1) }
    PageKeysEffect(turn)

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(
                "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineLarge,
            )
            TextMMD("${page + 1} / 2", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f)) {
            if (page == 0) WeatherAndWords(month, entries) else LineAndPhotos(entries, onOpenDay)
        }
        PagerRow(canPrevious = page > 0, canNext = page < 1, onPage = turn, onBack = onBack)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeatherAndWords(month: YearMonth, entries: List<Entry>) {
    val weather = entries.mapNotNull { e -> (e.answer() as? Answer.Weather)?.let { e.date to it.level } }
    val comment = stringArrayResource(R.array.review_comments)[weatherTrend(weather.map { it.second }).ordinal]
    DotildaBeside(Pose.Pleased, markSize = 32.dp) {
        TextMMD(comment, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.height(24.dp))
    TextMMD(stringResource(R.string.review_weather), style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(8.dp))
    // The frieze: each weather answer as a moon, at its day of the month.
    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
        val cell = size.width / 31
        val radius = cell * 0.4f
        for ((date, level) in weather) {
            val day = LocalDate.parse(date).dayOfMonth
            drawDot(DotShape.Moon(level), Offset(cell * (day - 0.5f), size.height / 2), radius)
        }
    }
    Spacer(Modifier.height(24.dp))
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
