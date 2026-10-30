package com.dotilda.app.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.dotilda.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * In-app updates from GitHub Releases, as in the readme.club app: read the manifest the CI
 * attaches to the latest release, download the APK, check its SHA-256, and hand it to
 * Android's installer (which always asks the user to confirm). This is Dotilda's only
 * network access: it sends nothing about the user, and it can be turned off in Settings.
 */
object Updater {
    private const val TAG = "Updater"

    // "latest" skips pre-releases (tags like v1.0.0-rc1), so test builds never show up as updates.
    private const val MANIFEST_URL = "https://github.com/kxrz/dotilda-app/releases/latest/download/manifest.json"

    /** [notes]: the version's CHANGELOG.md section. */
    data class Release(val version: String, val apkUrl: String, val sha256: String?, val notes: String?) {
        val isNewer: Boolean get() = compareVersions(version, BuildConfig.VERSION_NAME.substringBefore('-')) > 0

        /**
         * Only release builds update themselves: Android installs an update only when it is signed
         * with the same key, and debug builds are a separate app (.debug) anyway.
         */
        val canInstall: Boolean get() = isNewer && !BuildConfig.DEBUG && sha256 != null
    }

    /** The latest published release, or null when offline or the manifest is unreadable. */
    suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        try {
            val o = JSONObject(String(get(MANIFEST_URL)))
            Release(
                version = o.getString("latestVersion"),
                apkUrl = o.getString("apkUrl"),
                sha256 = o.optString("sha256").ifEmpty { null }?.lowercase(),
                notes = o.optString("notes").trim().ifEmpty { null },
            )
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed", e)
            null
        }
    }

    /** Downloads the APK into the cache and checks it against the manifest's SHA-256. */
    suspend fun download(context: Context, release: Release): File = withContext(Dispatchers.IO) {
        val bytes = get(release.apkUrl)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        check(digest == release.sha256) { "checksum mismatch" }
        File(context.cacheDir, "update.apk").apply { writeBytes(bytes) }
    }

    /**
     * False when the user must first allow Dotilda to install apps: Android's settings screen
     * for that is then opened, and the update can be retried when they come back.
     */
    fun ensureInstallAllowed(context: Context): Boolean {
        if (context.packageManager.canRequestPackageInstalls()) return true
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
        )
        return false
    }

    /** Starts the install; [InstallResultReceiver] then shows Android's confirmation screen. */
    suspend fun install(context: Context, apk: File) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("dotilda.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            // The installer adds the status extras to this intent, so it must stay mutable on Android 12+.
            val callback = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, InstallResultReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(callback.intentSender)
        }
    }

    private fun get(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        try {
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}

/** Compares dotted versions numerically ("0.10.0" > "0.9.1"); missing parts count as 0. */
fun compareVersions(a: String, b: String): Int {
    val left = a.split('.').map { it.toIntOrNull() ?: 0 }
    val right = b.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(left.size, right.size)) {
        val diff = left.getOrElse(i) { 0 } - right.getOrElse(i) { 0 }
        if (diff != 0) return diff.coerceIn(-1, 1)
    }
    return 0
}

/** The changelog's Markdown subset as plain text: headings kept as lines, "- " bullets as "• ". */
fun plainNotes(markdown: String): String = markdown.lines()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .joinToString("\n") { line ->
        when {
            line.startsWith("### ") -> line.removePrefix("### ")
            line.startsWith("- ") -> "• " + line.removePrefix("- ")
            else -> line
        }.replace("**", "").replace("`", "")
    }
