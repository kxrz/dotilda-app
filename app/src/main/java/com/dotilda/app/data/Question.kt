package com.dotilda.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class QuestionType {
    @SerialName("one_line") ONE_LINE,
    @SerialName("inner_weather") INNER_WEATHER,
    @SerialName("one_word") ONE_WORD,
    @SerialName("this_or_that") THIS_OR_THAT,
    @SerialName("three_things") THREE_THINGS,
    @SerialName("photo") PHOTO,
    @SerialName("mirror") MIRROR,
    @SerialName("letter") LETTER,
}

@Serializable
@Entity(tableName = "question")
data class Question(
    @PrimaryKey val id: String,
    val type: QuestionType,
    val text: String,
    /** The two choices of a THIS_OR_THAT question. */
    val options: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val journeyId: String? = null,
    /** MIRROR questions sharing a group are asked again 30 days apart. */
    val mirrorGroup: String? = null,
)
