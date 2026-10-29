package com.dotilda.app.data

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

const val MAX_CHANGES = 3

/** Days during which an answered question is not asked again. */
private const val RECENT_DAYS = 90L

/** A Mirror question comes back after this many days, and one is offered at least this often. */
const val MIRROR_DAYS = 30L

/** No Mirror in the first days: there is nothing to look back on yet. */
private const val MIRROR_AFTER_DAYS = 7L

/** The daily ritual: today's question, the 3 changes, saving the answer. */
class Journal(
    private val db: DotildaDatabase,
    private val prefs: Prefs,
    private val bankReady: Deferred<Unit>,
) {
    private val questions get() = db.questionDao()
    private val entries get() = db.entryDao()

    fun entryOn(date: LocalDate): Flow<Entry?> = entries.firstOn(date.toString())

    fun allEntries(): Flow<List<Entry>> = entries.all()

    suspend fun firstDate(): LocalDate? = entries.firstDate()?.let(LocalDate::parse)

    /** What the user answered the last time this question was asked (for Mirror). */
    suspend fun previousAnswer(question: Question, today: LocalDate): Entry? =
        entries.previousAnswer(question.id, today.toString())

    fun entriesOfYear(year: Int): Flow<List<Entry>> = entries.between("$year-01-01", "$year-12-31")

    fun entriesOf(month: YearMonth): Flow<List<Entry>> =
        entries.between(month.atDay(1).toString(), month.atEndOfMonth().toString())

    suspend fun question(id: String): Question? = questions.byId(id)

    suspend fun hasAnswered(date: LocalDate): Boolean = entries.firstOnNow(date.toString()) != null

    suspend fun lastEntryBefore(date: LocalDate): Entry? = entries.lastBefore(date.toString())

    /** Today's question, the same one until the user asks for another. */
    suspend fun questionOfTheDay(today: LocalDate): Pair<Question, DayState> {
        bankReady.await()
        val day = prefs.day()
        if (day.date == today.toString() && day.questionId != null) {
            questions.byId(day.questionId)?.let { return it to day }
        }
        val fresh = DayState(today.toString(), null, emptyList(), 0, false)
        return offer(today, fresh, changes = 0)!!
    }

    /** Swaps today's question, at most [MAX_CHANGES] times. Null once they are used. */
    suspend fun anotherQuestion(today: LocalDate): Pair<Question, DayState>? {
        val day = prefs.day()
        if (day.changes >= MAX_CHANGES) return null
        return offer(today, day, changes = day.changes + 1)
    }

    /** After the last change: just the inner weather. */
    suspend fun weatherQuestion(today: LocalDate): Pair<Question, DayState>? {
        val day = prefs.day().copy(weatherOffered = true)
        return offer(today, day, changes = day.changes, types = setOf(QuestionType.INNER_WEATHER))
    }

    suspend fun answer(question: Question, answer: Answer, today: LocalDate) {
        entries.insert(
            Entry(date = today.toString(), questionId = question.id, type = question.type, payload = answer.toPayload()),
        )
    }

    private suspend fun offer(
        today: LocalDate,
        day: DayState,
        changes: Int,
        types: Set<QuestionType> = SUPPORTED_TYPES,
    ): Pair<Question, DayState>? {
        val recent = entries.since(today.minusDays(RECENT_DAYS).toString())
        val mirrorSince = today.minusDays(MIRROR_DAYS).toString()
        // A Mirror answered more than 30 days ago may come back: that is the point of it.
        val recentIds = recent
            .filterNot { it.type == QuestionType.MIRROR && it.date < mirrorSince }
            .mapTo(HashSet()) { it.questionId }
        val first = firstDate()
        // Only for the day's first offer, so "another question" really changes the subject.
        val mirrorDue = day.shown.isEmpty() && first != null &&
            !first.isAfter(today.minusDays(MIRROR_AFTER_DAYS)) &&
            recent.none { it.type == QuestionType.MIRROR && it.date >= mirrorSince }
        val question = chooseQuestion(
            questions = questions.all(),
            recentQuestionIds = recentIds,
            shownToday = day.shown,
            yesterday = recent.lastOrNull { it.date == today.minusDays(1).toString() },
            mirrorDue = mirrorDue,
            allowedTypes = types,
        ) ?: return null
        val next = day.copy(questionId = question.id, shown = day.shown + question.id, changes = changes)
        prefs.saveDay(next)
        return question to next
    }
}
