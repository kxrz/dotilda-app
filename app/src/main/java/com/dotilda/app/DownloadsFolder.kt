package com.dotilda.app

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * For devices without a file picker (the Xteink S4 has none): exports go straight to
 * Download/Dotilda, which survives uninstalling and is reachable over USB, and imports look there.
 * No permission is needed to write there from Android 10 on.
 */
object DownloadsFolder {
    private const val FOLDER = "Dotilda"
    const val SHOWN = "Download/$FOLDER"

    /** Writes a new file and returns where it landed, as the user will find it. */
    suspend fun save(context: Context, name: String, mime: String, write: suspend (OutputStream) -> Unit): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) { "no Download folder" }
        try {
            checkNotNull(resolver.openOutputStream(uri)).use { write(it) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        // Android renames a file whose name is taken ("… (1).zip"): say the real one.
        val saved = resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        "$SHOWN/${saved ?: name}"
    }

    /** The newest journal export in Download/Dotilda, if any can be read. */
    fun latestBackup(): File? {
        @Suppress("DEPRECATION")
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER)
        return folder.listFiles { f -> f.name.startsWith("dotilda-") && f.name.endsWith(".zip") && f.canRead() }
            ?.maxByOrNull { it.lastModified() }
    }
}
