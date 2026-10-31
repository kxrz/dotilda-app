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
import androidx.compose.ui.test.onAllNodesWithTag
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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import com.dotilda.app.Photos
import com.dotilda.app.data.ActiveJourney
import com.dotilda.app.data.Answer
import com.dotilda.app.data.Constellation
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
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
// DataStore keeps one instance per process, so "welcomed" outlives a test: onboarding runs first
// and starts from a clean slate. Open, so the same tour runs at other sizes (SizesTest.kt).
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
open class ScreensTest {
    /** Folder of this tour's images, inside screenshots/ ("" for the Kompakt). */
    protected open val prefix: String = ""

    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<DotildaApp>()
    private val today = LocalDate.now()

    @Test
    fun a_onboarding() {
        awaitBank()
        offMain { app.prefs.resetAll() }
        launch()
        waitFor("Begin")
        shot("01-welcome")
        click("Begin")
        waitFor("When shall I come by?")
        shot("02-setup")
    }

    @Test
    fun b_ritual() {
        seed(question = "iw-001")
        launch()
        waitFor("Answer")
        shot("03-today")
        // The hidden game: poke Dotilda and she makes faces, then answers back.
        repeat(4) {
            compose.onAllNodesWithTag("dotilda")[0].performClick()
            compose.waitForIdle()
        }
        waitFor("That tickles.")
        shot("31-today-poked")
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

    @Test fun c1_answerLine() = answerShot("ol-001", "04-answer-line")

    @Test fun c2_answerChoice() = answerShot("tt-001", "08-answer-choice")

    @Test fun c3_answerThings() = answerShot("th-001", "09-answer-things")

    @Test fun c4_answerMirror() = answerShot("mi-001", "10-answer-mirror")

    @Test fun g1_answerLetter() = answerShot("le-001", "20-answer-letter")

    @Test fun g2_answerPhoto() = answerShot("ph-001", "21-answer-photo")

    /** One launch per test: several activities in a row make Robolectric's Compose flaky. */
    private fun answerShot(question: String, name: String) {
        seed(question = question)
        launch()
        waitFor("Answer")
        click("Answer")
        waitFor("Back")
        settle()
        shot(name)
    }

    @Test
    fun d_rereading() {
        seed(question = "ol-001")
        launch()
        waitFor("Answer")
        click("Notebook")
        waitFor(" / ", substring = true)
        shot("11-notebook")
        click("Back")
        waitFor("Answer")
        click("Galaxy")
        waitFor("Constellations", substring = true)
        settle()
        shot("12-galaxy")
    }

    @Test
    fun e_spiraAndSettings() {
        seed(question = "ol-001")
        launch()
        waitFor("Answer")
        click("Spira")
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
        click("Licences")
        waitFor("Apache", substring = true)
        shot("30-licenses")
    }

    @Test
    fun f_journeysAndReview() {
        seed(question = "ol-001")
        launch()
        waitFor("Answer")
        click("Journeys")
        waitFor("7 days to slow down")
        shot("17-journeys")
        click("Back")
        waitFor("Answer")
        click("Galaxy")
        waitFor("Constellations", substring = true)
        compose.onNodeWithContentDescription(today.minusMonths(1).month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)).performClick()
        waitFor("1 / 2")
        settle()
        shot("18-review-weather")
        click("›")
        waitFor("2 / 2")
        settle()
        shot("19-review-photos")
        click("Back")
        waitFor("Constellations", substring = true)
        click("Jul–Sep")
        waitFor("1 / 3")
        settle()
        shot("28-review-season")
        click("›")
        waitFor("2 / 3")
        settle()
        shot("29-review-season-choices")
    }

    @Test
    fun g3_letterReady() {
        seed(question = "ol-001")
        launch()
        waitFor("letter from", substring = true)
        shot("22-today-letter")
        click("letter from", substring = true)
        waitFor(" / ", substring = true)
        settle()
        shot("23-notebook-letter")
    }

    @Test
    fun h_journeyEnds() {
        seed(question = null, journeyDone = 6)
        launch()
        waitFor("day 7 of 7", substring = true)
        shot("24-today-journey")
        click("Answer")
        waitFor("Back")
        compose.onNode(hasSetTextAction()).performTextInput("Thank you, Mrs Pell, for the piano lessons.")
        click("Save")
        waitFor("I'll call this one", substring = true)
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        shot("25-done-constellation")
    }

    @Test
    fun i_skyStyle() {
        seed(question = "ol-001")
        launch()
        waitFor("Answer")
        click("Settings")
        waitFor("Daily reminder")
        click("›")
        waitFor("Your sky")
        click("Night sky")
        click("Bold")
        settle()
        shot("26-settings-journal")
        click("Back")
        waitFor("Answer")
        click("Galaxy")
        waitFor("Constellations", substring = true)
        settle()
        shot("27-galaxy-night-sky")
    }

    private var scenario: ActivityScenario<MainActivity>? = null

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun close() {
        scenario?.close()
    }

    /** Timestamped steps in the CI log, to see where a screenshot run spends its time. */
    private fun step(what: String) = println("[screens ${System.currentTimeMillis() % 1_000_000}] $what")

    private fun waitFor(text: String, substring: Boolean = false) {
        step("wait for '$text'")
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text, substring).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun click(text: String, substring: Boolean = false) {
        compose.onNodeWithText(text, substring).performClick()
        compose.waitForIdle()
    }

    /** Lets Room's background queries land on screen. */
    private fun settle() {
        Thread.sleep(300)
        ShadowLooper.idleMainLooper()
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        step("shot $name")
        settle()
        compose.onRoot().captureRoboImage("screenshots/$prefix$name.png")
    }

    private fun seed(question: String?, journeyDone: Int = 0) {
        step("seed $question")
        awaitBank()
        offMain { seedJournal(question, journeyDone) }
    }

    /**
     * Runs [block] on a background thread while this (main) thread keeps its looper turning:
     * blocking the main thread can deadlock Room, DataStore or the bank sync.
     */
    private fun offMain(block: suspend () -> Unit) {
        val job = CoroutineScope(Dispatchers.IO).async { block() }
        val deadline = System.currentTimeMillis() + 30_000
        while (!job.isCompleted) {
            check(System.currentTimeMillis() < deadline) { "seeding took more than 30 s" }
            ShadowLooper.idleMainLooper()
            Thread.sleep(10)
        }
        runBlocking { job.await() }
    }

    /** The bank loads on the main thread, which is this test's thread: let its looper run. */
    private fun awaitBank() {
        while (!app.bankReady.isCompleted) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(10)
        }
    }

    /**
     * Ten months of journal: most days answered, a Mirror each month, a photo now and then
     * (yesterday's too), two letters (one ready to open), a finished journey in early spring
     * and, with [journeyDone] > 0, the Gratitude journey under way. Today opens on [question].
     */
    private suspend fun seedJournal(question: String?, journeyDone: Int) {
        app.prefs.setWelcomed()
        val bank = app.db.questionDao().all()
        val byType = bank.filter { it.journeyId == null }.groupBy { it.type }
        val random = Random(7)
        val entries = mutableListOf<Entry>()
        val slow = app.journal.journeys.first { it.id == "slow" }
        val gratitude = app.journal.journeys.first { it.id == "gratitude" }
        val slowStart = today.minusDays(220)
        var date = today.minusDays(300)
        while (date.isBefore(today)) {
            val slowStep = java.time.temporal.ChronoUnit.DAYS.between(slowStart, date).toInt()
            val gratitudeStep = journeyDone - java.time.temporal.ChronoUnit.DAYS.between(date, today).toInt()
            var photo: String? = null
            val q = when {
                slowStep in 0 until slow.length -> slow.questions[slowStep]
                journeyDone > 0 && gratitudeStep in 0 until journeyDone -> gratitude.questions[gratitudeStep]
                date == today.minusDays(32) -> bank.first { it.id == "mi-001" }
                date == today.minusDays(100) || date == today.minusDays(20) -> byType.getValue(QuestionType.LETTER).random(random)
                date == today.minusDays(1) || date.dayOfMonth == 20 || date.dayOfMonth == 5 -> {
                    photo = samplePhoto(date.toString())
                    byType.getValue(QuestionType.PHOTO).random(random)
                }
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
                val answer = when (q.type) {
                    QuestionType.LETTER -> Answer.Letter(
                        "Dear me, I hope the bike is fixed, the plants are alive, and you still take the long way home.",
                        (if (date == today.minusDays(100)) today.minusDays(1) else today.plusMonths(5)).toString(),
                    )
                    QuestionType.PHOTO -> Answer.Photo(if (random.nextBoolean()) "The view from the bench" else "")
                    else -> sampleAnswer(q, random)
                }
                entries += Entry(
                    date = date.toString(),
                    questionId = q.id,
                    type = q.type,
                    payload = answer.toPayload(),
                    createdAt = date.toEpochDay() * 86_400_000,
                    photoPath = photo,
                )
            }
            date = date.plusDays(1)
        }
        app.db.entryDao().insertAll(entries)
        app.prefs.addConstellations(
            listOf(Constellation("slow", slow.constellation, (0 until slow.length).map { slowStart.plusDays(it.toLong()).toString() })),
        )
        if (journeyDone > 0) app.prefs.setActiveJourney(ActiveJourney("gratitude", today.minusDays(journeyDone.toLong()).toString()))
        // DataStore outlives a test (one instance per process): always reset the day.
        app.prefs.saveDay(DayState(if (question != null) today.toString() else "", question, listOfNotNull(question), 0, false))
    }

    /** A made-up snapshot (sky, sun, a bench), dithered like a real one. */
    private fun samplePhoto(name: String): String {
        val bitmap = Bitmap.createBitmap(480, 360, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (y in 0 until 360 step 4) {
            val v = 235 - y * 150 / 360
            paint.color = Color.rgb(v, v, v)
            canvas.drawRect(0f, y.toFloat(), 480f, y + 4f, paint)
        }
        paint.color = Color.rgb(250, 250, 250)
        canvas.drawCircle(360f, 90f, 46f, paint)
        paint.color = Color.rgb(60, 60, 60)
        canvas.drawRect(0f, 280f, 480f, 360f, paint)
        paint.color = Color.rgb(25, 25, 25)
        canvas.drawRect(120f, 230f, 300f, 250f, paint)
        canvas.drawRect(135f, 250f, 145f, 290f, paint)
        canvas.drawRect(275f, 250f, 285f, 290f, paint)
        val path = "photos/seed-$name.png"
        Photos.save(Photos.dither(bitmap), File(app.filesDir, path))
        return path
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pose.entries.forEach { DotildaMark(size = 36.dp, pose = it) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
