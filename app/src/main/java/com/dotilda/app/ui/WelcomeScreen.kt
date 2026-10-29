package com.dotilda.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD

/** First launch (and "About"): who Dotilda is and how the ritual works, on one page. */
@Composable
fun WelcomeScreen(button: String, onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))
        DotildaMark(size = 88.dp)
        Spacer(Modifier.height(16.dp))
        TextMMD(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(4.dp))
        TextMMD(stringResource(R.string.welcome_tagline), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(R.string.welcome_1, R.string.welcome_2, R.string.welcome_3).forEach {
                TextMMD(
                    stringResource(it),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        ButtonMMD(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { TextMMD(button) }
    }
}
