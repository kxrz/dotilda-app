package com.dotilda.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.dotilda.app.data.Journal
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate
import java.time.YearMonth

/** The day's dot, filled once, and Dotilda's brief word. */
@Composable
fun DoneScreen(journal: Journal, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    val month = remember { YearMonth.from(today) }
    val entry by remember { journal.entryOn(today) }.collectAsState(initial = null)
    val monthEntries by remember { journal.entriesOf(month) }.collectAsState(initial = emptyList())

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        entry?.let { DayDot(it.dotShape(), size = 96.dp) }
        Spacer(Modifier.height(28.dp))
        TextMMD(
            stringResource(R.string.noted),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(32.dp))
        MonthLine(month, monthEntries, today)
        Spacer(Modifier.weight(1f))
        ButtonMMD(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            TextMMD(stringResource(R.string.back_to_today))
        }
    }
}
