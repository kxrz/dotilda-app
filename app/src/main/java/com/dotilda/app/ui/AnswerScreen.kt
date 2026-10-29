package com.dotilda.app.ui

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.dotilda.app.data.Answer
import com.dotilda.app.data.Journal
import com.dotilda.app.data.Question
import com.dotilda.app.data.QuestionType
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import java.time.LocalDate
import kotlinx.coroutines.launch

private const val ONE_LINE_MAX = 140
private const val ONE_WORD_MAX = 30

@Composable
fun AnswerScreen(journal: Journal, onSaved: () -> Unit, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    var question by remember { mutableStateOf<Question?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { question = journal.questionOfTheDay(today).first }
    val q = question ?: return

    val save: (Answer) -> Unit = { answer ->
        if (!saving) {
            saving = true
            scope.launch {
                journal.answer(q, answer, today)
                onSaved()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        DotildaAsks()
        Spacer(Modifier.height(16.dp))
        TextMMD(q.text, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        val rest = Modifier.weight(1f)
        when (q.type) {
            QuestionType.ONE_WORD -> TextInput(ONE_WORD_MAX, singleWord = true, onBack, rest) { save(Answer.Word(it)) }
            QuestionType.INNER_WEATHER -> WeatherInput(onBack, rest) { save(Answer.Weather(it)) }
            QuestionType.THIS_OR_THAT -> ChoiceInput(q.options, onBack, rest) { i -> save(Answer.Choice(i, q.options[i])) }
            else -> TextInput(ONE_LINE_MAX, singleWord = false, onBack, rest) { save(Answer.Text(it)) }
        }
    }
}

@Composable
private fun TextInput(
    maxLength: Int,
    singleWord: Boolean,
    onBack: () -> Unit,
    modifier: Modifier,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val canSave = text.isNotBlank()

    Column(modifier) {
        TextFieldMMD(
            value = text,
            onValueChange = { value ->
                text = (if (singleWord) value.filterNot(Char::isWhitespace) else value.replace('\n', ' ')).take(maxLength)
            },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            singleLine = singleWord,
            minLines = if (singleWord) 1 else 3,
            placeholder = {
                TextMMD(stringResource(if (singleWord) R.string.one_word_hint else R.string.one_line_hint))
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (canSave) onSave(text.trim()) }),
        )
        if (!singleWord) {
            Spacer(Modifier.height(4.dp))
            TextMMD(
                "${text.length} / $maxLength",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.weight(1f))
        ActionRow(
            back = stringResource(R.string.back),
            onBack = onBack,
            action = stringResource(R.string.save),
            actionEnabled = canSave,
            onAction = { onSave(text.trim()) },
        )
    }
}

/** Five cards, sunny to stormy, each with a word from Dotilda. A tap saves, like This or that. */
@Composable
private fun WeatherInput(onBack: () -> Unit, modifier: Modifier, onSave: (Int) -> Unit) {
    val phrases = stringArrayResource(R.array.weather_phrases)
    Column(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (level in 0..4) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color.Black, RoundedCornerShape(10.dp))
                        .clickable { onSave(level) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    WeatherIcon(level, size = 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        TextMMD(weatherLabel(level), style = MaterialTheme.typography.titleSmall)
                        TextMMD(phrases[level], style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        ActionRow(back = stringResource(R.string.back), onBack = onBack)
    }
}

@Composable
private fun ChoiceInput(options: List<String>, onBack: () -> Unit, modifier: Modifier, onSave: (Int) -> Unit) {
    Column(modifier) {
        options.forEachIndexed { i, label ->
            OutlinedButtonMMD(onClick = { onSave(i) }, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                TextMMD(label, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(16.dp))
        }
        Spacer(Modifier.weight(1f))
        ActionRow(back = stringResource(R.string.back), onBack = onBack)
    }
}
