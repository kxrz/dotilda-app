package com.dotilda.app.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import com.dotilda.app.Photos
import com.dotilda.app.data.key
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.dotilda.app.data.Answer
import com.dotilda.app.data.Entry
import com.dotilda.app.data.Journal
import com.dotilda.app.data.Question
import com.dotilda.app.data.answer
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate

/** The journal as a book: one page per entry, oldest first, opened at [startDate] or the last page. */
@Composable
fun NotebookScreen(journal: Journal, startDate: String?, onBack: () -> Unit) {
    val loaded by remember { journal.allEntries() }.collectAsState(initial = null)
    val entries = loaded ?: return

    if (entries.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            TextMMD(stringResource(R.string.notebook), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            TextMMD(stringResource(R.string.notebook_empty), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            ActionRow(back = stringResource(R.string.back), onBack = onBack)
        }
        return
    }

    var page by rememberSaveable {
        mutableIntStateOf(entries.indexOfFirst { it.date == startDate }.takeIf { it >= 0 } ?: entries.lastIndex)
    }
    val turn: (Int) -> Unit = { page = (page + it).coerceIn(0, entries.lastIndex) }
    PageKeysEffect(turn)
    val entry = entries[page.coerceIn(0, entries.lastIndex)]

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextMMD(stringResource(R.string.notebook), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextMMD("${page + 1} / ${entries.size}", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDividerMMD(thickness = 1.dp)
        Spacer(Modifier.height(16.dp))
        NotebookPage(journal, entry, Modifier.weight(1f))
        PagerRow(canPrevious = page > 0, canNext = page < entries.lastIndex, onPage = turn, onBack = onBack)
    }
}

@Composable
private fun NotebookPage(journal: Journal, entry: Entry, modifier: Modifier) {
    val question by produceState<Question?>(null, entry.questionId) { value = journal.question(entry.questionId) }
    Column(modifier) {
        TextMMD(LocalDate.parse(entry.date).longTitle(appLocale()), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DayDot(entry.dotShape(), size = 20.dp)
            Spacer(Modifier.width(12.dp))
            TextMMD(question?.text.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(24.dp))
        when (val answer = entry.answer()) {
            is Answer.Things -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                answer.items.forEach { TextMMD("·  $it", style = MaterialTheme.typography.titleLarge) }
            }
            is Answer.Weather -> Row(verticalAlignment = Alignment.CenterVertically) {
                WeatherIcon(answer.level, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    TextMMD(weatherLabel(answer.level), style = MaterialTheme.typography.titleLarge)
                    TextMMD(stringArrayResource(R.array.weather_phrases)[answer.level], style = MaterialTheme.typography.bodySmall)
                }
            }
            is Answer.Photo -> {
                val context = LocalContext.current
                val photo = remember(entry.photoPath) { entry.photoPath?.let { Photos.load(context, it)?.asImageBitmap() } }
                photo?.let {
                    Image(it, contentDescription = answer.caption, contentScale = ContentScale.Fit, modifier = Modifier.weight(1f).fillMaxWidth())
                }
                if (answer.caption.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    TextMMD(answer.caption, fontStyle = FontStyle.Italic)
                }
            }
            is Answer.Letter -> {
                val today = remember { LocalDate.now() }
                if (answer.sealedUntil > today.toString()) {
                    TextMMD(
                        stringResource(R.string.letter_sealed_until, LocalDate.parse(answer.sealedUntil).longTitle(appLocale())),
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else {
                    // Read on or after its day: it no longer waits on Today.
                    LaunchedEffect(entry.key) { journal.markLetterOpened(entry) }
                    TextMMD(
                        answer.text,
                        modifier = Modifier.weight(1f),
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            else -> TextMMD(answer.summary(), style = MaterialTheme.typography.headlineLarge)
        }
    }
}
