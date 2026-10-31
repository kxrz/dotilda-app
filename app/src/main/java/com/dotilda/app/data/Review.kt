package com.dotilda.app.data

/** How a month's inner weather went, for Dotilda's one-line comment on the review. */
enum class Trend { None, Sunny, Stormy, Brightening, Clouding, Mixed }

/** [levels]: the month's inner weather in date order, 0 (sunny) to 4 (stormy). */
fun weatherTrend(levels: List<Int>): Trend {
    if (levels.size < 2) return Trend.None
    val average = levels.average()
    val half = levels.size / 2
    val first = levels.take(half).average()
    val second = levels.drop(half).average()
    return when {
        average <= 1.0 -> Trend.Sunny
        average >= 3.0 -> Trend.Stormy
        first - second >= 1.0 -> Trend.Brightening
        second - first >= 1.0 -> Trend.Clouding
        else -> Trend.Mixed
    }
}

/** The month's one-word answers, most frequent first (case ignored), at most [max]. */
fun wordCloud(words: List<String>, max: Int = 12): List<Pair<String, Int>> =
    words.map { it.trim().lowercase().replaceFirstChar(Char::titlecase) }
        .filter { it.isNotEmpty() }
        .groupingBy { it }.eachCount()
        .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(max)
        .map { it.key to it.value }

/**
 * This-or-that answers counted per question, the most answered questions first; within one,
 * the choices from most to least picked: "Tea 9 · Coffee 4".
 */
fun choiceTallies(entries: List<Entry>): Map<String, List<Pair<String, Int>>> =
    entries.mapNotNull { e -> (e.answer() as? Answer.Choice)?.let { e.questionId to it.label } }
        .groupBy({ it.first }, { it.second })
        .entries.sortedByDescending { it.value.size }
        .associate { (id, labels) ->
            id to labels.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key to it.value }
        }
