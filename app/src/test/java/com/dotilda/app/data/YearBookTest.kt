package com.dotilda.app.data

import com.adobe.epubcheck.api.EpubCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipFile
import javax.imageio.ImageIO

class YearBookTest {
    private fun png(): ByteArray = ByteArrayOutputStream().also {
        ImageIO.write(BufferedImage(60, 90, BufferedImage.TYPE_BYTE_GRAY), "png", it)
    }.toByteArray()

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
        assertTrue("EPUBCheck found errors, see the test output", EpubCheck(file).validate())
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
