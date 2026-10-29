package com.dotilda.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One answer. The first entry of a day gives that day's dot its shape. */
@Entity(tableName = "entry")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO date (yyyy-MM-dd), in the user's time zone when they answered. */
    val date: String,
    val questionId: String,
    val type: QuestionType,
    /** [Answer] as JSON, so one table holds every format and exports as is. */
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val photoPath: String? = null,
)

@Serializable
sealed interface Answer {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : Answer

    @Serializable
    @SerialName("word")
    data class Word(val word: String) : Answer

    /** 0 = sunny … 4 = stormy. */
    @Serializable
    @SerialName("weather")
    data class Weather(val level: Int) : Answer

    @Serializable
    @SerialName("choice")
    data class Choice(val index: Int, val label: String) : Answer
}

fun Answer.toPayload(): String = Json.encodeToString<Answer>(this)

fun Entry.answer(): Answer = Json.decodeFromString<Answer>(payload)
