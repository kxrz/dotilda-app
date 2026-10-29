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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.dotilda.app.DotildaApp
import com.dotilda.app.R
import com.dotilda.app.data.Question
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun TodayScreen(app: DotildaApp, onOpenDiagnostics: () -> Unit) {
    var question by remember { mutableStateOf<Question?>(null) }
    var draw by remember { mutableIntStateOf(0) }
    LaunchedEffect(draw) {
        app.bankReady.await()
        question = app.db.questionDao().random()
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DotildaMark(size = 40.dp)
            Spacer(Modifier.width(12.dp))
            TextMMD(stringResource(R.string.dotilda_asks), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(32.dp))
        TextMMD(question?.text.orEmpty(), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        TextMMD(
            question?.type?.name?.lowercase()?.replace('_', ' ').orEmpty(),
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.weight(1f))
        OutlinedButtonMMD(onClick = { draw++ }, modifier = Modifier.fillMaxWidth()) {
            TextMMD(stringResource(R.string.another_question))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextMMD(
                stringResource(R.string.motto),
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.bodySmall,
            )
            TextMMD(
                stringResource(R.string.diagnostics),
                modifier = Modifier.clickable(onClick = onOpenDiagnostics),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
