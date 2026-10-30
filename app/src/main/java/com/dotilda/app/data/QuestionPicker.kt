package com.dotilda.app.data

import kotlin.random.Random

/** Formats the app can answer so far. Grows sprint after sprint. */
val SUPPORTED_TYPES = setOf(
    QuestionType.ONE_LINE,
    QuestionType.ONE_WORD,
    QuestionType.INNER_WEATHER,
    QuestionType.THIS_OR_THAT,
    QuestionType.THREE_THINGS,
    QuestionType.MIRROR,
    QuestionType.PHOTO,
    QuestionType.LETTER,
)

/** Light formats offered the day after a stormy inner weather. */
private val LIGHT_TYPES = setOf(QuestionType.ONE_WORD, QuestionType.THIS_OR_THAT)

const val STORM = 4

/**
 * Chooses a question, relaxing the rules one by one until something is left:
 * never one already offered today, light formats after a storm, a Mirror or a Letter when due,
 * not the same format as yesterday, and nothing answered recently.
 */
fun chooseQuestion(
    questions: List<Question>,
    recentQuestionIds: Set<String>,
    shownToday: Collection<String>,
    yesterday: Entry?,
    mirrorDue: Boolean = false,
    random: Random = Random.Default,
    allowedTypes: Set<QuestionType> = SUPPORTED_TYPES,
    letterDue: Boolean = false,
): Question? {
    // Journey questions only come within their journey; a letter, a long piece, only once a season.
    val pool = questions.filter {
        it.type in allowedTypes && it.id !in shownToday && it.journeyId == null &&
            (it.type != QuestionType.LETTER || letterDue || allowedTypes == setOf(QuestionType.LETTER))
    }
    val stormy = (yesterday?.answer() as? Answer.Weather)?.level == STORM
    val preferredTypes = when {
        stormy -> LIGHT_TYPES
        mirrorDue && QuestionType.MIRROR in allowedTypes -> setOf(QuestionType.MIRROR)
        letterDue && QuestionType.LETTER in allowedTypes -> setOf(QuestionType.LETTER)
        yesterday != null -> allowedTypes - yesterday.type
        else -> allowedTypes
    }
    val fresh = pool.filter { it.id !in recentQuestionIds }
    return sequenceOf(
        fresh.filter { it.type in preferredTypes },
        pool.filter { it.type in preferredTypes },
        fresh,
        pool,
    ).firstOrNull { it.isNotEmpty() }?.random(random)
}
