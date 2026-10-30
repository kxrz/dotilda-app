package com.dotilda.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.dotilda.app.data.JourneyProgress
import com.dotilda.app.data.Journal
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate
import kotlinx.coroutines.launch

/** The journeys: one at a time, one question a day; finishing one draws its constellation. */
@Composable
fun JourneysScreen(journal: Journal, onStarted: () -> Unit, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    val scope = rememberCoroutineScope()
    val active by remember { journal.activeJourney }.collectAsState(initial = null)
    val constellations by remember { journal.constellations }.collectAsState(initial = emptyList())
    val progress by produceState<JourneyProgress?>(null, active) { value = journal.journeyProgress() }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextMMD(stringResource(R.string.journeys), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        journal.journeys.forEach { journey ->
            HorizontalDividerMMD(thickness = 1.dp)
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextMMD(journey.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    when {
                        active?.journeyId == journey.id -> TextLink(stringResource(R.string.journey_stop), { scope.launch { journal.stopJourney() } })
                        active == null -> TextLink(stringResource(R.string.journey_start), {
                            scope.launch {
                                journal.startJourney(journey, today)
                                onStarted()
                            }
                        })
                    }
                }
                val done = constellations.lastOrNull { it.journeyId == journey.id }
                val line = when {
                    active?.journeyId == journey.id && progress?.journey?.id == journey.id ->
                        stringResource(R.string.journey_day, progress!!.done + 1, journey.length)
                    done != null -> stringResource(R.string.journey_done, done.name)
                    else -> stringResource(R.string.journey_about, journey.length, journey.description)
                }
                TextMMD(line, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
        HorizontalDividerMMD(thickness = 1.dp)
        Spacer(Modifier.weight(1f))
        ActionRow(back = stringResource(R.string.back), onBack = onBack)
    }
}
