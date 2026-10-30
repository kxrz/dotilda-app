package com.dotilda.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dotilda.app.DotildaApp
import com.dotilda.app.MainActivity
import com.dotilda.app.data.Answer
import com.dotilda.app.data.DayState
import com.dotilda.app.data.Entry
import com.dotilda.app.data.Question
import com.dotilda.app.data.QuestionType
import com.dotilda.app.data.toPayload
import com.dotilda.app.ui.DayDot
import com.dotilda.app.ui.DotShape
import com.dotilda.app.ui.DotildaMark
import com.dotilda.app.ui.DotildaTheme
import com.dotilda.app.ui.Pose
import com.dotilda.app.ui.WeatherIcon
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Tours the real screens at the Kompakt's size (360 × 564 dp, 213 dpi) with ten months of
 * made-up journal, for design reviews. Images are written only when run with
 * -Proborazzi.test.record=true (see .github/workflows/screenshots.yml).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = KOMPAKT)
class ScreensTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<DotildaApp>()
    private val today = LocalDate.now()

    @Test
    fun onboarding() {
        launch()
        waitFor("Begin")
        shot("01-welcome")
        click("Begin")
        waitFor("When shall I come by?")
        shot("02-setup")
    }

    @Test
    fun ritual() {
        seed(question = "iw-001")
        launch()
        waitFor("Answer")
        shot("03-today")
        click("Answer")
        waitFor("Stormy")
        shot("05-answer-weather")
        click("Rainy")
        waitFor("Noted", substring = true)
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        shot("06-done")
        click("Back to today")
        waitFor("See you tomorrow.")
        shot("07-today-answered")
    }

    @Test
    fun answerFormats() {
        for ((id, name) in listOf("ol-001" to "04-answer-line", "tt-001" to "08-answer-choice", "th-001" to "09-answer-things", "mi-001" to "10-answer-mirror")) {
            seed(question = id)
            val scenario = launch()
            waitFor("Answer")
            click("Answer")
            waitFor("Back")
            settle()
            shot(name)
            scenario.close()
        }
    }

    @Test
    fun rereading() {
        seed(question = "ol-001")
        launch()
        waitFor("Answer")
        click("Notebook")
        waitFor(" / ", substring = true)
        shot("11-notebook")
        click("Back")
        waitFor("Answer")
        click("Galaxy")
        waitFor("Touch a dot", substring = true)
        settle()
        shot("12-galaxy")
    }

    @Test
    fun spiraAndSettings() {
        seed(question = "ol-001")
        launch()
        waitFor("Answer")
        click("spira")
        waitFor("One minute", substring = true)
        shot("13-spira-breathe")
        click("Timer")
        shot("14-spira-timer")
        click("Stopwatch")
        shot("15-spira-stopwatch")
        click("Back")
        waitFor("Answer")
        click("Settings")
        waitFor("Daily reminder")
        shot("16-settings")
    }

    private fun launch(): ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java)

    private fun waitFor(text: String, substring: Boolean = false) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text, substring).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun click(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    /** Lets Room's background queries land on screen. */
    private fun settle() {
        Thread.sleep(300)
        ShadowLooper.idleMainLooper()
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        settle()
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    /** Ten months of journal: most days answered, a Mirror each month, today open on [question]. */
    private fun seed(question: String) {
        // The bank loads on the main thread, which is this test's thread: let its looper run.
        while (!app.bankReady.isCompleted) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(10)
        }
        runBlocking { seedJournal(question) }
    }

    private suspend fun seedJournal(question: String) {
        app.prefs.setWelcomed()
        val bank = app.db.questionDao().all()
        val byType = bank.groupBy { it.type }
        val random = Random(7)
        val entries = mutableListOf<Entry>()
        var date = today.minusDays(300)
        while (date.isBefore(today)) {
            val q = when {
                date == today.minusDays(32) -> bank.first { it.id == "mi-001" }
                random.nextFloat() > 0.8f -> null
                date.dayOfMonth == 12 -> byType.getValue(QuestionType.MIRROR).random(random)
                else -> byType.getValue(
                    listOf(
                        QuestionType.ONE_LINE, QuestionType.ONE_WORD, QuestionType.INNER_WEATHER,
                        QuestionType.INNER_WEATHER, QuestionType.THIS_OR_THAT, QuestionType.THREE_THINGS,
                    ).random(random),
                ).random(random)
            }
            if (q != null) {
                entries += Entry(
                    date = date.toString(),
                    questionId = q.id,
                    type = q.type,
                    payload = sampleAnswer(q, random).toPayload(),
                    createdAt = date.toEpochDay() * 86_400_000,
                )
            }
            date = date.plusDays(1)
        }
        app.db.entryDao().insertAll(entries)
        app.prefs.saveDay(DayState(today.toString(), question, listOf(question), 0, false))
    }

    private fun sampleAnswer(q: Question, random: Random): Answer = when (q.type) {
        QuestionType.ONE_WORD -> Answer.Word(listOf("Quiet", "Busy", "Golden", "Tired", "Hopeful").random(random))
        QuestionType.INNER_WEATHER -> Answer.Weather(random.nextInt(5))
        QuestionType.THIS_OR_THAT -> random.nextInt(2).let { Answer.Choice(it, q.options[it]) }
        QuestionType.THREE_THINGS -> Answer.Things(listOf("A long walk", "Soup with Anna", "Finished the chapter"))
        else -> Answer.Text(
            listOf(
                "The light on the kitchen table this morning.",
                "Mum called, we laughed about the cat for ten minutes.",
                "I finally fixed the bike, with oily hands and a big grin.",
            ).random(random),
        )
    }
}

/** The design pieces side by side: Dotilda's poses, the dot shapes, the weather pictos. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = KOMPAKT)
class DesignSheetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sheet() {
        compose.setContent {
            DotildaTheme {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pose.entries.forEach { DotildaMark(size = 56.dp, pose = it) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pose.entries.forEach { DotildaMark(size = 24.dp, pose = it) }
                    }
                    val shapes = listOf(DotShape.Full, DotShape.Double) + (0..4).map { DotShape.Moon(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { shapes.forEach { DayDot(it, size = 32.dp) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { shapes.forEach { DayDot(it, size = 9.dp) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (0..4).forEach { WeatherIcon(it) } }
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/00-design-sheet.png")
    }
}

/** The Mudita Kompakt: 480 × 800 px at 213 dpi, of which the app gets 360 × 564 dp. */
const val KOMPAKT = "w360dp-h564dp-port-tvdpi"
