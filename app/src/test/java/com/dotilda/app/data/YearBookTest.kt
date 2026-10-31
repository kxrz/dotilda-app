package com.dotilda.app.data

import com.adobe.epubcheck.api.EpubCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import java.time.LocalDate
import java.util.zip.ZipFile

class YearBookTest {
    /** A small grey PNG, written by hand (java.awt is not there in Android unit tests). */
    private fun png(width: Int = 6, height: Int = 9): ByteArray {
        fun chunk(type: String, data: ByteArray): ByteArray {
            val typed = type.toByteArray() + data
            return ByteBuffer.allocate(4).putInt(data.size).array() + typed +
                ByteBuffer.allocate(4).putInt(CRC32().apply { update(typed) }.value.toInt()).array()
        }
        val header = ByteBuffer.allocate(13).putInt(width).putInt(height).put(8).put(0).put(0).put(0).put(0).array()
        val raw = ByteArrayOutputStream().apply { repeat(height) { write(0); repeat(width) { write(200) } } }.toByteArray()
        val pixels = ByteArrayOutputStream().also { out -> DeflaterOutputStream(out).use { it.write(raw) } }.toByteArray()
        return byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10) +
            chunk("IHDR", header) + chunk("IDAT", pixels) + chunk("IEND", ByteArray(0))
    }

    private fun book() = YearBook(
        year = 2026,
        pages = listOf(
            BookPage(LocalDate.of(2026, 1, 3), "What made you smile today?", listOf("Tea & toast <before> \"dawn\"")),
            BookPage(LocalDate.of(2026, 1, 9), "Three small wins this week.", listOf("A walk", "Soup", "A chapter"), asList = true),
            BookPage(LocalDate.of(2026, 3, 20), "Show me your desk.", listOf("The view from the bench"), photo = png()),
        ),
        constellations = listOf("Slow"),
        cover = png(),
        madeOn = LocalDate.of(2026, 12, 31),
    )

    @Test
    fun passesEpubCheck() {
        val file = File.createTempFile("dotilda-", ".epub")
        file.outputStream().use { writeEpub(book(), it) }
        // doValidate: bit 1 = warnings, 2 = errors, 4 = fatal errors (details in the test output).
        val result = EpubCheck(file).doValidate()
        assertEquals("EPUBCheck found errors, see the test output", 0, result and 6)
    }

    @Test
    fun startsWithAnUncompressedMimetype() {
        val file = File.createTempFile("dotilda-", ".epub")
        file.outputStream().use { writeEpub(book(), it) }
        ZipFile(file).use { zip ->
            val first = zip.entries().nextElement()
            assertEquals("mimetype", first.name)
            assertEquals(java.util.zip.ZipEntry.STORED, first.method)
            assertTrue(zip.getEntry("EPUB/m01.xhtml") != null && zip.getEntry("EPUB/m03.xhtml") != null)
            assertTrue(zip.getEntry("EPUB/images/photo-1.png") != null)
        }
    }
}
