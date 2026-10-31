package com.dotilda.app.data

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

const val MAX_CHANGES = 3

/** Days during which an answered question is not asked again. */
private const val RECENT_DAYS = 90L

/** A Mirror question comes back after this many days, and one is offered at least this often. */
const val MIRROR_DAYS = 30L

/** No Mirror in the first days: there is nothing to look back on yet. */
private const val MIRROR_AFTER_DAYS = 7L

/** A letter to the future is offered once a season, from the third week on. */
private const val LETTER_DAYS = 90L
private const val LETTER_AFTER_DAYS = 14L

/** How far along a journey is: [done] questions answered out of [Journey.length]. */
data class JourneyProgress(val journey: Journey, val startedAt: String, val done: Int) {
    val next: Question? get() = journey.questions.getOrNull(done)
}

/** The daily ritual: today's question, the 3 changes, saving the answer. */
class Journal(
    private val db: DotildaDatabase,
    private val prefs: Prefs,
    private val bankReady: Deferred<Unit>,
    val journeys: List<Journey>,
) {
    private val questions get() = db.questionDao()
    private val entries get() = db.entryDao()

    fun entryOn(date: LocalDate): Flow<Entry?> = entries.firstOn(date.toString())

    fun allEntries(): Flow<List<Entry>> = entries.all()

    suspend fun firstDate(): LocalDate? = entries.firstDate()?.let(LocalDate::parse)

    /** What the user answered the last time this question was asked (for Mirror). */
    suspend fun previousAnswer(question: Question, today: LocalDate): Entry? =
        entries.previousAnswer(question.id, today.toString())

    fun entriesBetween(from: LocalDate, to: LocalDate): Flow<List<Entry>> = entries.between(from.toString(), to.toString())

    fun entriesOfYear(year: Int): Flow<List<Entry>> = entries.between("$year-01-01", "$year-12-31")

    fun entriesOf(month: YearMonth): Flow<List<Entry>> =
        entries.between(month.atDay(1).toString(), month.atEndOfMonth().toString())

    suspend fun question(id: String): Question? = questions.byId(id)

    suspend fun hasEntries(month: YearMonth): Boolean =
        entries.since(month.atDay(1).toString()).any { it.date <= month.atEndOfMonth().toString() }

    suspend fun hasAnswered(date: LocalDate): Boolean = entries.firstOnNow(date.toString()) != null

    suspend fun lastEntryBefore(date: LocalDate): Entry? = entries.lastBefore(date.toString())

    /** Today's question, the same one until the user asks for another. During a journey, its next step. */
    suspend fun questionOfTheDay(today: LocalDate): Pair<Question, DayState> {
        bankReady.await()
        val day = prefs.day()
        if (day.date == today.toString() && day.questionId != null) {
            questions.byId(day.questionId)?.let { return it to day }
        }
        val fresh = DayState(today.toString(), null, emptyList(), 0, false)
        journeyProgress()?.next?.let { step ->
            val next = fresh.copy(questionId = step.id, shown = listOf(step.id))
            prefs.saveDay(next)
            return step to next
        }
        return offer(today, fresh, changes = 0)!!
    }

    /** The journey under way and how far along it is, or null. */
    suspend fun journeyProgress(): JourneyProgress? {
        val active = prefs.activeJourney.first() ?: return null
        val journey = journeys.firstOrNull { it.id == active.journeyId } ?: return null
        val ids = journey.questions.mapTo(HashSet()) { it.id }
        val done = entries.since(active.startedAt).filter { it.questionId in ids }.distinctBy { it.questionId }.size
        return JourneyProgress(journey, active.startedAt, done)
    }

    /**
     * Starts a journey. Its first question becomes today's, unless today is already written:
     * then the journey starts tomorrow.
     */
    suspend fun startJourney(journey: Journey, today: LocalDate) {
        val answered = hasAnswered(today)
        val start = if (answered) today.plusDays(1) else today
        prefs.setActiveJourney(ActiveJourney(journey.id, start.toString()))
        if (!answered) {
            val day = prefs.day().takeIf { it.date == today.toString() } ?: DayState(today.toString(), null, emptyList(), 0, false)
            val first = journey.questions.first()
            prefs.saveDay(day.copy(questionId = first.id, shown = day.shown + first.id))
        }
    }

    suspend fun stopJourney() = prefs.setActiveJourney(null)

    /** Every finished journey, oldest first. */
    val constellations: Flow<List<Constellation>> get() = prefs.constellations

    val activeJourney: Flow<ActiveJourney?> get() = prefs.activeJourney

    val asides: Flow<Asides> get() = prefs.asides

    suspend fun dismissAside(key: String) = prefs.dismissAside(key)

    suspend fun markUpdateSeen(version: String, today: LocalDate) = prefs.markUpdateSeen(version, today.toString())

    suspend fun markLetterOpened(entry: Entry) = prefs.markLetterOpened(entry.key)

    /** Sealed letters whose day has come and that haven't been read yet. */
    suspend fun lettersReady(today: LocalDate): List<Entry> {
        val opened = prefs.openedLetters.first()
        return entries.allNow().filter { entry ->
            val letter = entry.answer() as? Answer.Letter
            letter != null && letter.sealedUntil <= today.toString() && entry.key !in opened
        }
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

    /** Saves the answer; when it is a journey's last step, returns the constellation it completes. */
    suspend fun answer(question: Question, answer: Answer, today: LocalDate, photoPath: String? = null): Constellation? {
        entries.insert(
            Entry(
                date = today.toString(),
                questionId = question.id,
                type = question.type,
                payload = answer.toPayload(),
                photoPath = photoPath,
            ),
        )
        val progress = journeyProgress() ?: return null
        if (question.journeyId != progress.journey.id || progress.done < progress.journey.length) return null
        val ids = progress.journey.questions.mapTo(HashSet()) { it.id }
        val dates = entries.since(progress.startedAt).filter { it.questionId in ids }.map { it.date }.distinct()
        val constellation = Constellation(progress.journey.id, progress.journey.constellation, dates)
        prefs.addConstellations(listOf(constellation))
        prefs.stopJourneyIfActive(progress.journey.id)
        return constellation
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
        val letterDue = day.shown.isEmpty() && !mirrorDue && first != null &&
            !first.isAfter(today.minusDays(LETTER_AFTER_DAYS)) &&
            recent.none { it.type == QuestionType.LETTER && it.date >= today.minusDays(LETTER_DAYS).toString() }
        val question = chooseQuestion(
            questions = questions.all(),
            recentQuestionIds = recentIds,
            shownToday = day.shown,
            yesterday = recent.lastOrNull { it.date == today.minusDays(1).toString() },
            mirrorDue = mirrorDue,
            allowedTypes = types,
            letterDue = letterDue,
        ) ?: return null
        val next = day.copy(questionId = question.id, shown = day.shown + question.id, changes = changes)
        prefs.saveDay(next)
        return question to next
    }
}
