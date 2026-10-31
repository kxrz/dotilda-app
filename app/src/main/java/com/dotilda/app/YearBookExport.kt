package com.dotilda.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.dotilda.app.data.Answer
import com.dotilda.app.data.BookPage
import com.dotilda.app.data.BookStrings
import com.dotilda.app.data.Constellation
import com.dotilda.app.data.Journal
import com.dotilda.app.data.YearBook
import com.dotilda.app.data.answer
import com.dotilda.app.data.writeEpub
import com.dotilda.app.ui.drawDot
import com.dotilda.app.ui.drawRestDot
import com.dotilda.app.ui.drawStarRays
import com.dotilda.app.ui.dotShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The year as an EPUB book: answers put into words, photos, and the year's galaxy as the cover. */
object YearBookExport {

    suspend fun export(context: Context, journal: Journal, year: Int, out: OutputStream) = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val locale = Locale.forLanguageTag(context.getString(R.string.app_locale))
        val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
        val entries = journal.entriesOfYear(year).first()
        val constellations = journal.constellations.first().filter { c -> c.dates.any { it.startsWith("$year-") } }
        val weather = context.resources.getStringArray(R.array.weather_labels)
        val phrases = context.resources.getStringArray(R.array.weather_phrases)
        val pages = entries.groupBy { it.date }.values.map { it.first() }.map { entry ->
            val question = journal.question(entry.questionId)?.text.orEmpty()
            val answer = entry.answer()
            val lines = when (answer) {
                is Answer.Text -> listOf(answer.text)
                is Answer.Word -> listOf(answer.word)
                is Answer.Weather -> listOf("${weather[answer.level]}. ${phrases[answer.level]}")
                is Answer.Choice -> listOf(answer.label)
                is Answer.Things -> answer.items
                is Answer.Photo -> listOfNotNull(answer.caption.ifEmpty { null })
                is Answer.Letter ->
                    if (answer.sealedUntil <= today.toString()) listOf(answer.text)
                    else listOf(context.getString(R.string.book_letter_sealed, LocalDate.parse(answer.sealedUntil).format(longDate)))
            }
            BookPage(
                date = LocalDate.parse(entry.date),
                question = question,
                lines = lines,
                asList = answer is Answer.Things,
                photo = entry.photoPath?.let { path -> java.io.File(context.filesDir, path).takeIf { it.exists() }?.readBytes() },
            )
        }
        val book = YearBook(
            year = year,
            pages = pages,
            constellations = constellations.map { it.name }.distinct(),
            cover = cover(context, year, entries, constellations, today),
            madeOn = today,
            strings = BookStrings(
                title = context.getString(R.string.book_title),
                contents = context.getString(R.string.book_contents),
                coverAlt = context.getString(R.string.book_cover_alt),
                constellations = context.getString(R.string.book_constellations),
                constellationsIntro = context.getString(R.string.book_constellations_intro),
            ),
            locale = locale,
        )
        writeEpub(book, out)
    }

    /** The year's galaxy, drawn with the app's own dots, under the year and Dotilda's name. */
    private fun cover(
        context: Context,
        year: Int,
        entries: List<com.dotilda.app.data.Entry>,
        constellations: List<Constellation>,
        today: LocalDate,
    ): ByteArray {
        val width = 1200
        val height = 1800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        val shapes = entries.groupBy { it.date }.mapValues { it.value.first().dotShape(today) }
        val stars = constellations.flatMap { it.dates }.toSet()
        val margin = 110f
        val top = 520f
        val cell = (width - 2 * margin) / 31
        val row = (height - top - 200f) / 12
        val radius = cell * 0.3f
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(width.toFloat(), height.toFloat())) {
            for (m in 1..12) {
                val month = java.time.YearMonth.of(year, m)
                for (d in 1..month.lengthOfMonth()) {
                    val date = month.atDay(d)
                    if (date.isAfter(today)) break
                    val center = Offset(margin + cell * (d - 0.5f), top + row * (m - 0.5f))
                    val shape = shapes[date.toString()]
                    if (shape != null) drawDot(shape, center, radius) else drawRestDot(center, radius)
                    if (shape != null && date.toString() in stars) drawStarRays(center, radius)
                }
            }
        }
        val canvas = android.graphics.Canvas(bitmap)
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 150f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        canvas.drawText(year.toString(), width / 2f, 300f, title)
        title.textSize = 56f
        title.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        canvas.drawText(context.getString(R.string.book_cover_subtitle), width / 2f, 400f, title)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    }
}
