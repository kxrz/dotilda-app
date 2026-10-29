package com.dotilda.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(entities = [Question::class, Entry::class], version = 2)
@TypeConverters(Converters::class)
abstract class DotildaDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun entryDao(): EntryDao

    companion object {
        fun create(context: Context): DotildaDatabase =
            Room.databaseBuilder(context, DotildaDatabase::class.java, "dotilda.db")
                // Version 1 (sprint 0 test builds) only held the question bank, which is
                // re-synced at launch. From version 2 on, every change needs a real migration.
                .fallbackToDestructiveMigrationFrom(1)
                .build()
    }
}

class Converters {
    @TypeConverter
    fun fromList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toList(value: String): List<String> = Json.decodeFromString(value)
}
