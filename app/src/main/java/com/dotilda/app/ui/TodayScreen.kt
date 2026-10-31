package com.dotilda.app.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.dotilda.app.data.DayState
import com.dotilda.app.data.Entry
import com.dotilda.app.data.Journal
import com.dotilda.app.data.JourneyProgress
import com.dotilda.app.data.MAX_CHANGES
import com.dotilda.app.data.Question
import com.dotilda.app.data.answer
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val shortDate = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/** Wraps the entry so "not loaded yet" (null) differs from "no entry today". */
private class Today(val entry: Entry?)

@Composable
fun TodayScreen(
    journal: Journal,
    onAnswer: () -> Unit,
    onNotebook: () -> Unit,
    onGalaxy: () -> Unit,
    onSpira: () -> Unit,
    onSettings: () -> Unit,
    onJourneys: () -> Unit,
    onLetter: (date: String) -> Unit,
    onReview: (YearMonth) -> Unit,
    newVersion: String?,
    justUpdatedTo: String?,
    onUpdate: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val month = remember { YearMonth.from(today) }
    val day by remember { journal.entryOn(today).map(::Today) }.collectAsState(initial = null)
    val monthEntries by remember { journal.entriesOf(month) }.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextMMD(
            month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(6.dp))
        MonthLine(month, monthEntries, today, Modifier.clickable(onClick = onGalaxy))
        Spacer(Modifier.height(24.dp))

        val entry = day?.entry
        when {
            day == null -> Spacer(Modifier.weight(1f))
            entry != null -> Answered(journal, entry, Modifier.weight(1f))
            else -> Open(journal, today, onAnswer, Modifier.weight(1f))
        }

        // Dotilda's asides, one at most, never more than a line: a letter whose day has come,
        // last month's review in the first days of a month, then versions.
        val letter by produceState<Entry?>(null) { value = journal.lettersReady(today).firstOrNull() }
        val lastMonth = month.minusMonths(1)
        val hasLastMonth by produceState(false) { value = journal.hasEntries(lastMonth) }
        val letterOpen = letter
        when {
            letterOpen != null -> DotildaSays(
                stringResource(R.string.letter_ready, LocalDate.parse(letterOpen.date).format(shortDate)),
                Pose.Pleased,
            ) { onLetter(letterOpen.date) }
            today.dayOfMonth <= 3 && hasLastMonth -> DotildaSays(
                stringResource(R.string.review_ready, lastMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
                Pose.Peering,
            ) { onReview(lastMonth) }
            newVersion != null -> DotildaSays(stringResource(R.string.update_ready), Pose.Curious, onUpdate)
            justUpdatedTo != null -> DotildaSays(stringResource(R.string.just_updated, justUpdatedTo), Pose.Pleased)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val small = MaterialTheme.typography.bodySmall
            TextLink(stringResource(R.string.notebook), onNotebook, style = small)
            TextLink(stringResource(R.string.galaxy), onGalaxy, style = small)
            TextLink(stringResource(R.string.journeys), onJourneys, style = small)
            TextLink(stringResource(R.string.spira), onSpira, style = small)
            TextLink(stringResource(R.string.settings), onSettings, style = small)
        }
    }
}

@Composable
private fun Answered(journal: Journal, entry: Entry, modifier: Modifier) {
    var question by remember { mutableStateOf<Question?>(null) }
    LaunchedEffect(entry.questionId) { question = journal.question(entry.questionId) }

    Column(modifier) {
        DotildaBeside(Pose.Pleased, markSize = 40.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DayDot(entry.dotShape(), size = 20.dp)
                Spacer(Modifier.width(10.dp))
                TextMMD(stringResource(R.string.noted), style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(32.dp))
        TextMMD(question?.text.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        TextMMD(entry.answer().summary(), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.weight(1f))
        TextMMD(stringResource(R.string.see_you_tomorrow), fontStyle = FontStyle.Italic)
    }
}

@Composable
private fun Open(journal: Journal, today: LocalDate, onAnswer: () -> Unit, modifier: Modifier) {
    var offer by remember { mutableStateOf<Pair<Question, DayState>?>(null) }
    var away by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        offer = journal.questionOfTheDay(today)
        val last = journal.lastEntryBefore(today)
        away = last != null && LocalDate.parse(last.date).isBefore(today.minusDays(2))
    }
    val (question, state) = offer ?: return

    Column(modifier) {
        if (away) {
            TextMMD(stringResource(R.string.welcome_back), fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(16.dp))
        }
        // A new question, a new look: she alternates poses instead of animating.
        val progress by produceState<JourneyProgress?>(null, question.id) { value = journal.journeyProgress() }
        progress?.takeIf { it.next?.id == question.id }?.let {
            TextMMD(
                stringResource(R.string.journey_step, it.journey.title, it.done + 1, it.journey.length),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(10.dp))
        }
        DotildaQuestion(
            question.text,
            style = MaterialTheme.typography.headlineLarge,
            pose = if (state.changes % 2 == 1) Pose.Curious else Pose.Peering,
        )
        Spacer(Modifier.weight(1f))

        ButtonMMD(onClick = onAnswer, modifier = Modifier.fillMaxWidth()) {
            TextMMD(stringResource(R.string.answer))
        }
        Spacer(Modifier.height(12.dp))
        val left = MAX_CHANGES - state.changes
        when {
            left > 0 -> OutlinedButtonMMD(
                onClick = { scope.launch { journal.anotherQuestion(today)?.let { offer = it } } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextMMD(stringResource(R.string.another_question, left))
            }
            !state.weatherOffered -> {
                TextMMD(stringResource(R.string.no_more_questions), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButtonMMD(
                    onClick = { scope.launch { journal.weatherQuestion(today)?.let { offer = it } } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextMMD(stringResource(R.string.just_the_weather))
                }
            }
            else -> TextMMD(stringResource(R.string.or_tomorrow), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
