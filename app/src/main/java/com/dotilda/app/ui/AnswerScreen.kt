package com.dotilda.app.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.dotilda.app.Photos
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
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.dotilda.app.data.Answer
import com.dotilda.app.data.Entry
import com.dotilda.app.data.answer
import com.dotilda.app.data.Journal
import com.dotilda.app.data.Question
import com.dotilda.app.data.QuestionType
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

private const val ONE_LINE_MAX = 140
private const val ONE_WORD_MAX = 30
private const val THING_MAX = 60
private const val CAPTION_MAX = 80
private const val LETTER_MAX = 600

@Composable
fun AnswerScreen(journal: Journal, onSaved: () -> Unit, onBreathe: () -> Unit, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    var question by remember { mutableStateOf<Question?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { question = journal.questionOfTheDay(today).first }
    val q = question ?: return

    fun save(answer: Answer, photoPath: String? = null) {
        if (!saving) {
            saving = true
            scope.launch {
                journal.answer(q, answer, today, photoPath)
                onSaved()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        DotildaAsks()
        Spacer(Modifier.height(16.dp))
        TextMMD(q.text, style = MaterialTheme.typography.titleLarge)
        // The formats that ask for writing can start with a breathing minute.
        if (q.type in setOf(QuestionType.ONE_LINE, QuestionType.MIRROR, QuestionType.THREE_THINGS)) {
            TextLink(stringResource(R.string.breathe_first), onBreathe)
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(24.dp))
        }
        val rest = Modifier.weight(1f)
        when (q.type) {
            QuestionType.ONE_WORD -> TextInput(ONE_WORD_MAX, singleWord = true, onBack, rest) { save(Answer.Word(it)) }
            QuestionType.INNER_WEATHER -> WeatherInput(onBack, rest) { save(Answer.Weather(it)) }
            QuestionType.THIS_OR_THAT -> ChoiceInput(q.options, onBack, rest) { i -> save(Answer.Choice(i, q.options[i])) }
            QuestionType.THREE_THINGS -> ThingsInput(onBack, rest) { save(Answer.Things(it)) }
            QuestionType.MIRROR -> Column(rest) {
                MirrorReminder(journal, q, today)
                TextInput(ONE_LINE_MAX, singleWord = false, onBack, Modifier.weight(1f)) { save(Answer.Text(it)) }
            }
            QuestionType.PHOTO -> PhotoInput(onBack, rest) { path, caption -> save(Answer.Photo(caption), path) }
            QuestionType.LETTER -> LetterInput(q, today, onBack, rest) { text, until -> save(Answer.Letter(text, until.toString())) }
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

/** "30 days ago, you wrote…", shown before a Mirror question is answered again. */
@Composable
private fun MirrorReminder(journal: Journal, question: Question, today: LocalDate) {
    val previous by produceState<Entry?>(null, question.id) { value = journal.previousAnswer(question, today) }
    val entry = previous ?: return
    val days = ChronoUnit.DAYS.between(LocalDate.parse(entry.date), today).toInt()
    TextMMD(stringResource(R.string.mirror_previous, days), style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(4.dp))
    TextMMD(entry.answer().summary(), fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(16.dp))
}

/** Three short lines. One is enough to save: small wins count. */
@Composable
private fun ThingsInput(onBack: () -> Unit, modifier: Modifier, onSave: (List<String>) -> Unit) {
    var a by rememberSaveable { mutableStateOf("") }
    var b by rememberSaveable { mutableStateOf("") }
    var c by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val items = listOf(a, b, c).map(String::trim).filter(String::isNotEmpty)

    Column(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(a to { v: String -> a = v }, b to { v: String -> b = v }, c to { v: String -> c = v })
                .forEachIndexed { i, (value, set) ->
                    TextFieldMMD(
                        value = value,
                        onValueChange = { set(it.replace('\n', ' ').take(THING_MAX)) },
                        modifier = Modifier.fillMaxWidth().let { if (i == 0) it.focusRequester(focus) else it },
                        singleLine = true,
                        placeholder = { TextMMD("${i + 1}.") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = if (i < 2) ImeAction.Next else ImeAction.Done,
                        ),
                    )
                }
        }
        Spacer(Modifier.weight(1f))
        ActionRow(
            back = stringResource(R.string.back),
            onBack = onBack,
            action = stringResource(R.string.save),
            actionEnabled = items.isNotEmpty(),
            onAction = { onSave(items) },
        )
    }
}

/** The system camera takes the picture; Dotilda keeps it small and dithered, with an optional caption. */
@Composable
private fun PhotoInput(onBack: () -> Unit, modifier: Modifier, onSave: (path: String, caption: String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var path by rememberSaveable { mutableStateOf<String?>(null) }
    var caption by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    val developing = stringResource(R.string.photo_developing)
    val noCamera = stringResource(R.string.photo_no_camera)
    val failed = stringResource(R.string.photo_failed)
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        if (!taken) return@rememberLauncherForActivityResult
        status = developing
        scope.launch {
            runCatching { Photos.develop(context) }
                .onSuccess { path = it; status = null }
                .onFailure { status = failed }
        }
    }
    val bitmap = remember(path) { path?.let { Photos.load(context, it)?.asImageBitmap() } }

    Column(modifier) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(bitmap, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            } else {
                TextMMD(status ?: stringResource(R.string.photo_hint), textAlign = TextAlign.Center)
            }
        }
        if (bitmap != null) {
            TextFieldMMD(
                value = caption,
                onValueChange = { caption = it.replace('\n', ' ').take(CAPTION_MAX) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { TextMMD(stringResource(R.string.photo_caption)) },
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButtonMMD(
            onClick = {
                try {
                    camera.launch(Photos.captureUri(context))
                } catch (e: ActivityNotFoundException) {
                    status = noCamera
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextMMD(stringResource(if (path == null) R.string.photo_take else R.string.photo_retake))
        }
        Spacer(Modifier.height(12.dp))
        ActionRow(
            back = stringResource(R.string.back),
            onBack = onBack,
            action = stringResource(R.string.save),
            actionEnabled = path != null,
            onAction = { path?.let { onSave(it, caption.trim()) } },
        )
    }
}

/** A longer letter, sealed until a date chosen among four. */
@Composable
private fun LetterInput(question: Question, today: LocalDate, onBack: () -> Unit, modifier: Modifier, onSave: (String, LocalDate) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val text0 = question.text.lowercase()
    var months by rememberSaveable {
        mutableIntStateOf(
            when {
                "season" in text0 -> 3
                "year" in text0 || "birthday" in text0 -> 12
                else -> 6
            },
        )
    }
    Column(modifier) {
        TextFieldMMD(
            value = text,
            onValueChange = { text = it.take(LETTER_MAX) },
            modifier = Modifier.fillMaxWidth().weight(1f),
            placeholder = { TextMMD(stringResource(R.string.letter_hint)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(8.dp))
        TextMMD(stringResource(R.string.letter_open_in), style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(1, 3, 6, 12).forEach { m ->
                TextMMD(
                    LocalContext.current.resources.getQuantityString(R.plurals.letter_months, m, m),
                    modifier = Modifier.clickable { months = m }.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (m == months) FontWeight.Bold else null,
                    textDecoration = if (m == months) TextDecoration.Underline else null,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        ActionRow(
            back = stringResource(R.string.back),
            onBack = onBack,
            action = stringResource(R.string.letter_seal),
            actionEnabled = text.isNotBlank(),
            onAction = { onSave(text.trim(), today.plusMonths(months.toLong())) },
        )
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
