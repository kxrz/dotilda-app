package com.dotilda.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.dotilda.app.data.ditherFloydSteinberg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Photo of the day: the system camera takes the picture, then Dotilda keeps it as a small
 * dithered PNG (four greys, the Kompakt's width), stored in the app's private files.
 */
object Photos {
    /** Width of the kept picture: the Kompakt's screen width in pixels. */
    private const val WIDTH = 480

    /** Where the camera app writes the full picture before it is developed. */
    fun captureUri(context: Context): Uri {
        val file = File(context.cacheDir, "camera/capture.jpg").apply { parentFile?.mkdirs(); delete() }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    /** Shrinks, straightens and dithers the last capture; returns its path relative to filesDir. */
    suspend fun develop(context: Context): String = withContext(Dispatchers.IO) {
        val source = File(context.cacheDir, "camera/capture.jpg")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= WIDTH) sample *= 2
        val decoded = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("no picture")
        val rotation = when (ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
        val height = upright.height * WIDTH / upright.width
        val small = Bitmap.createScaledBitmap(upright, WIDTH, height, true)
        val path = "photos/${UUID.randomUUID()}.png"
        save(dither(small), File(context.filesDir, path))
        source.delete()
        path
    }

    fun dither(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h).also { bitmap.getPixels(it, 0, w, 0, 0, w, h) }
        val luma = IntArray(pixels.size) {
            val p = pixels[it]
            ((p shr 16 and 0xFF) * 299 + (p shr 8 and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
        }
        val grey = ditherFloydSteinberg(luma, w, h)
        val out = IntArray(grey.size) { 0xFF shl 24 or (grey[it] shl 16) or (grey[it] shl 8) or grey[it] }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    fun save(bitmap: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    fun load(context: Context, path: String): Bitmap? = BitmapFactory.decodeFile(File(context.filesDir, path).path)
}
