package com.dotilda.app.data

import kotlin.random.Random

/** Formats the app can answer so far. Grows sprint after sprint. */
val SUPPORTED_TYPES = setOf(
    QuestionType.ONE_LINE,
    QuestionType.ONE_WORD,
    QuestionType.INNER_WEATHER,
    QuestionType.THIS_OR_THAT,
)

/** Light formats offered the day after a stormy inner weather. */
private val LIGHT_TYPES = setOf(QuestionType.ONE_WORD, QuestionType.THIS_OR_THAT)

const val STORM = 4

/**
 * Chooses a question, relaxing the rules one by one until something is left:
 * never one already offered today, light formats after a storm, not the same format as
 * yesterday, and nothing answered recently.
 */
fun chooseQuestion(
    questions: List<Question>,
    recentQuestionIds: Set<String>,
    shownToday: Collection<String>,
    yesterday: Entry?,
    random: Random = Random.Default,
    allowedTypes: Set<QuestionType> = SUPPORTED_TYPES,
): Question? {
    val pool = questions.filter { it.type in allowedTypes && it.id !in shownToday }
    val stormy = (yesterday?.answer() as? Answer.Weather)?.level == STORM
    val preferredTypes = when {
        stormy -> LIGHT_TYPES
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
