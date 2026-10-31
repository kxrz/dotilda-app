package com.dotilda.app

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZonedDateTime

/**
 * Dotilda sends nothing, so a crash is kept on the device: the last one, in the app's files.
 * Diagnostics shows it and lets the user export it to send it themselves.
 */
object CrashLog {
    private const val FILE = "last-crash.txt"
    private const val SEEN = "last-crash.seen"
    private const val RECENT_MS = 60 * 60 * 1000L

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            save(app, error)
            previous?.uncaughtException(thread, error)
        }
    }

    fun save(context: Context, error: Throwable) {
        runCatching { File(context.filesDir, FILE).writeText(report(error)) }
    }

    /**
     * The last crash if it hasn't been shown yet: the next launch shows it on a plain screen first,
     * so it can be read even when Dotilda falls over before reaching Diagnostics.
     */
    fun unseen(context: Context): String? {
        val file = File(context.filesDir, FILE).takeIf { it.exists() } ?: return null
        // A crash from long ago (before this screen existed, say) is left to Diagnostics.
        if (System.currentTimeMillis() - file.lastModified() > RECENT_MS) return null
        val seen = File(context.filesDir, SEEN).takeIf { it.exists() }?.readText()
        return if (seen == file.lastModified().toString()) null else file.readText()
    }

    fun markSeen(context: Context) {
        val file = File(context.filesDir, FILE).takeIf { it.exists() } ?: return
        runCatching { File(context.filesDir, SEEN).writeText(file.lastModified().toString()) }
    }

    fun read(context: Context): String? = File(context.filesDir, FILE).takeIf { it.exists() }?.readText()

    fun clear(context: Context) {
        File(context.filesDir, FILE).delete()
    }

    private fun report(error: Throwable): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }
        return buildString {
            appendLine("Dotilda ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("When: ${ZonedDateTime.now()}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.DISPLAY}")
            appendLine()
            append(trace)
        }
    }
}
