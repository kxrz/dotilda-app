package com.dotilda.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The whole journal in one zip: journal.json (plain JSON, readable without the app) and the
 * dithered photos under photos/, at the same paths as in the app's files.
 */
@Serializable
class JournalFile(
    val format: Int = 1,
    val exportedAt: String,
    val entries: List<Entry>,
    val constellations: List<Constellation> = emptyList(),
)

private const val JOURNAL = "journal.json"
private const val PHOTOS = "photos/"
private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

class Backup(private val dao: EntryDao, private val prefs: Prefs, private val filesDir: File) {
    suspend fun export(out: OutputStream, exportedAt: String) = withContext(Dispatchers.IO) {
        val entries = dao.allNow()
        val file = JournalFile(exportedAt = exportedAt, entries = entries, constellations = prefs.constellations.first())
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(JOURNAL))
            zip.write(json.encodeToString(JournalFile.serializer(), file).toByteArray())
            zip.closeEntry()
            entries.mapNotNull { it.photoPath }.map { File(filesDir, it) }.filter { it.exists() }.forEach { photo ->
                zip.putNextEntry(ZipEntry(photo.relativeTo(filesDir).invariantSeparatorsPath))
                photo.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Adds the file's entries, photos and constellations, skipping what is already there. Returns how many entries were added. */
    suspend fun import(input: InputStream): Int = withContext(Dispatchers.IO) {
        var file: JournalFile? = null
        ZipInputStream(input).use { zip ->
            generateSequence { zip.nextEntry }.forEach { item ->
                when {
                    item.name == JOURNAL -> file = json.decodeFromString(JournalFile.serializer(), zip.readBytes().decodeToString())
                    // Only plain names under photos/: nothing in the zip may write elsewhere.
                    item.name.startsWith(PHOTOS) && item.name.count { it == '/' } == 1 && !item.name.contains("..") -> {
                        val target = File(filesDir, item.name)
                        if (!target.exists()) {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
            }
        }
        val journal = file ?: error("no $JOURNAL in this file")
        val fresh = newEntries(existing = dao.allNow(), incoming = journal.entries)
        dao.insertAll(fresh)
        val known = prefs.constellations.first().mapTo(HashSet()) { it.journeyId to it.dates.firstOrNull() }
        prefs.addConstellations(journal.constellations.filter { known.add(it.journeyId to it.dates.firstOrNull()) })
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
