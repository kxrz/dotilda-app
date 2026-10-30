package com.dotilda.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The whole journal in one zip: journal.json now, the dithered photos next to it later.
 * Plain JSON on purpose, so it stays readable without the app.
 */
@Serializable
class JournalFile(
    val format: Int = 1,
    val exportedAt: String,
    val entries: List<Entry>,
)

private const val JOURNAL = "journal.json"
private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

class Backup(private val dao: EntryDao) {
    suspend fun export(out: OutputStream, exportedAt: String) = withContext(Dispatchers.IO) {
        val file = JournalFile(exportedAt = exportedAt, entries = dao.allNow())
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(JOURNAL))
            zip.write(json.encodeToString(JournalFile.serializer(), file).toByteArray())
            zip.closeEntry()
        }
    }

    /** Adds the file's entries to the journal, skipping those already there. Returns how many were added. */
    suspend fun import(input: InputStream): Int = withContext(Dispatchers.IO) {
        val file = ZipInputStream(input).use { zip ->
            generateSequence { zip.nextEntry }.firstOrNull { it.name == JOURNAL }
                ?: error("no $JOURNAL in this file")
            json.decodeFromString(JournalFile.serializer(), zip.readBytes().decodeToString())
        }
        val fresh = newEntries(existing = dao.allNow(), incoming = file.entries)
        dao.insertAll(fresh)
        fresh.size
    }
}

/** Entries not already in the journal (same day, question and moment), with fresh ids. */
fun newEntries(existing: List<Entry>, incoming: List<Entry>): List<Entry> {
    val known = existing.mapTo(HashSet()) { Triple(it.date, it.questionId, it.createdAt) }
    return incoming
        .filter { known.add(Triple(it.date, it.questionId, it.createdAt)) }
        .map { it.copy(id = 0) }
}
