package com.dotilda.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionPickerTest {
    private val bank = listOf(
        Question("ol-1", QuestionType.ONE_LINE, "a"),
        Question("ol-2", QuestionType.ONE_LINE, "b"),
        Question("ow-1", QuestionType.ONE_WORD, "c"),
        Question("tt-1", QuestionType.THIS_OR_THAT, "d", options = listOf("A", "B")),
        Question("iw-1", QuestionType.INNER_WEATHER, "e"),
        Question("ph-1", QuestionType.PHOTO, "f"),
        Question("mi-1", QuestionType.MIRROR, "g", mirrorGroup = "g"),
    )

    private fun yesterday(type: QuestionType, answer: Answer) =
        Entry(date = "2026-10-28", questionId = "x", type = type, payload = answer.toPayload())

    private fun pick(
        recent: Set<String> = emptySet(),
        shown: List<String> = emptyList(),
        yesterday: Entry? = null,
        types: Set<QuestionType> = SUPPORTED_TYPES,
        mirrorDue: Boolean = false,
        seed: Int,
    ) = chooseQuestion(bank, recent, shown, yesterday, mirrorDue, Random(seed), types)

    @Test
    fun neverOffersUnsupportedFormats() {
        repeat(50) { assertNotEquals(QuestionType.PHOTO, pick(seed = it)!!.type) }
    }

    @Test
    fun neverRepeatsAQuestionShownToday() {
        repeat(50) { assertNotEquals("ol-1", pick(shown = listOf("ol-1"), seed = it)!!.id) }
    }

    @Test
    fun avoidsYesterdaysFormat() {
        val y = yesterday(QuestionType.ONE_LINE, Answer.Text("hi"))
        repeat(50) { assertNotEquals(QuestionType.ONE_LINE, pick(yesterday = y, seed = it)!!.type) }
    }

    @Test
    fun goesLightAfterAStorm() {
        val y = yesterday(QuestionType.INNER_WEATHER, Answer.Weather(STORM))
        repeat(50) {
            val type = pick(yesterday = y, seed = it)!!.type
            assertTrue(type == QuestionType.ONE_WORD || type == QuestionType.THIS_OR_THAT)
        }
    }

    @Test
    fun prefersQuestionsNotAnsweredRecently() {
        repeat(50) { assertEquals("ol-2", pick(recent = setOf("ol-1", "ow-1", "tt-1", "iw-1", "mi-1"), seed = it)!!.id) }
    }

    @Test
    fun fallsBackToRecentQuestionsRatherThanNothing() {
        val all = bank.map { it.id }.toSet()
        repeat(20) { assertTrue(pick(recent = all, seed = it) != null) }
    }

    @Test
    fun offersAMirrorWhenDue() {
        repeat(20) { assertEquals("mi-1", pick(mirrorDue = true, seed = it)!!.id) }
    }

    @Test
    fun stormBeatsADueMirror() {
        val y = yesterday(QuestionType.INNER_WEATHER, Answer.Weather(STORM))
        repeat(20) { assertNotEquals(QuestionType.MIRROR, pick(yesterday = y, mirrorDue = true, seed = it)!!.type) }
    }

    @Test
    fun weatherOnlyWhenAsked() {
        assertEquals("iw-1", pick(types = setOf(QuestionType.INNER_WEATHER), seed = 1)!!.id)
        assertNull(pick(types = setOf(QuestionType.INNER_WEATHER), shown = listOf("iw-1"), seed = 1))
    }

    @Test
    fun answerPayloadRoundTrips() {
        val entry = yesterday(QuestionType.THIS_OR_THAT, Answer.Choice(1, "Coffee"))
        assertEquals(Answer.Choice(1, "Coffee"), entry.answer())
        val things = yesterday(QuestionType.THREE_THINGS, Answer.Things(listOf("a", "b", "c")))
        assertEquals(Answer.Things(listOf("a", "b", "c")), things.answer())
    }
}
