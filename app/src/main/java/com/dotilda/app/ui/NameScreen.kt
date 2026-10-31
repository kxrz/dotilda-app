package com.dotilda.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.dotilda.app.data.Prefs
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * "What shall I call you?": a first name or a nickname, kept on the device, which Dotilda uses
 * now and then. During onboarding (and from Settings); saying nothing is fine.
 */
@Composable
fun NameScreen(prefs: Prefs, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { if (name == null) name = prefs.name.first() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(name != null) { if (name != null) focus.requestFocus() }

    fun finish(value: String) {
        scope.launch {
            prefs.setName(value)
            onDone()
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        DotildaQuestion(stringResource(R.string.name_question), style = MaterialTheme.typography.headlineLarge, pose = Pose.Curious)
        Spacer(Modifier.height(12.dp))
        TextMMD(stringResource(R.string.name_hint), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        TextFieldMMD(
            value = name.orEmpty(),
            onValueChange = { name = it.replace('\n', ' ').take(Prefs.NAME_MAX) },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            singleLine = true,
            placeholder = { TextMMD(stringResource(R.string.name_placeholder)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { finish(name.orEmpty()) }),
        )
        Spacer(Modifier.weight(1f))
        ButtonMMD(onClick = { finish(name.orEmpty()) }, enabled = !name.isNullOrBlank(), modifier = Modifier.fillMaxWidth()) {
            TextMMD(stringResource(R.string.name_save))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButtonMMD(onClick = { finish("") }, modifier = Modifier.fillMaxWidth()) {
            TextMMD(stringResource(R.string.name_skip))
        }
    }
}
