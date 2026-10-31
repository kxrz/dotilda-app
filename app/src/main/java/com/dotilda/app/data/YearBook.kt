package com.dotilda.app.data

import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** One day of the book: its question and the answer, already put into words. */
class BookPage(
    val date: LocalDate,
    val question: String,
    val lines: List<String>,
    /** Three things and the like: shown as a list rather than paragraphs. */
    val asList: Boolean = false,
    val photo: ByteArray? = null,
)

/** A year of journal, ready to become an EPUB 3 book to read on an e-reader. */
class YearBook(
    val year: Int,
    val pages: List<BookPage>,
    val constellations: List<String>,
    /** The cover: the year's galaxy, as a PNG. */
    val cover: ByteArray,
    val madeOn: LocalDate,
    val strings: BookStrings = BookStrings(),
)

/** The book's few words of its own, so they can follow the app's language. */
class BookStrings(
    val title: String = "My year with Dotilda",
    val contents: String = "Contents",
    val coverAlt: String = "The year in dots",
    val constellations: String = "Constellations",
    val constellationsIntro: String = "Journeys finished this year, each one a little constellation in the sky:",
)

private val dayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)
private val monthFormat = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

/**
 * Writes [book] as an EPUB 3: a cover, a table of contents, one chapter per month with one
 * section per day, the photos, and the year's constellations. Plain XHTML and a little CSS,
 * which e-readers such as KOReader render well.
 */
fun writeEpub(book: YearBook, out: OutputStream) {
    val months = book.pages.sortedBy { it.date }.groupBy { it.date.monthValue }
    val photos = mutableListOf<Pair<String, ByteArray>>()
    val chapters = months.map { (month, pages) ->
        val id = "m%02d".format(month)
        val name = pages.first().date.format(monthFormat)
        Triple(id, name, chapter(name, pages) { photo ->
            val path = "images/photo-${photos.size + 1}.png"
            photos += path to photo
            path
        })
    }
    val extra = if (book.constellations.isEmpty()) null else constellationsPage(book)

    ZipOutputStream(out).use { zip ->
        // The mimetype comes first and uncompressed, as EPUB requires.
        val mimetype = "application/epub+zip".toByteArray()
        zip.putNextEntry(
            ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mimetype.size.toLong()
                compressedSize = mimetype.size.toLong()
                crc = CRC32().apply { update(mimetype) }.value
            },
        )
        zip.write(mimetype)
        zip.closeEntry()
        fun put(path: String, bytes: ByteArray) {
            zip.putNextEntry(ZipEntry(path))
            zip.write(bytes)
            zip.closeEntry()
        }
        fun put(path: String, text: String) = put(path, text.toByteArray())

        put("META-INF/container.xml", CONTAINER)
        put("EPUB/package.opf", packageOpf(book, chapters.map { it.first to "${it.first}.xhtml" }, photos.map { it.first }, extra != null))
        put("EPUB/nav.xhtml", nav(book, chapters.map { "${it.first}.xhtml" to it.second }, extra != null))
        put("EPUB/cover.xhtml", page(book.strings.title, "<div class=\"cover\"><img src=\"images/cover.png\" alt=\"${esc(book.strings.coverAlt)}\"/></div>"))
        put("EPUB/style.css", CSS)
        put("EPUB/images/cover.png", book.cover)
        chapters.forEach { (id, _, xhtml) -> put("EPUB/$id.xhtml", xhtml) }
        extra?.let { put("EPUB/constellations.xhtml", it) }
        photos.forEach { (path, bytes) -> put("EPUB/$path", bytes) }
    }
}

private fun chapter(name: String, pages: List<BookPage>, addPhoto: (ByteArray) -> String): String {
    val body = StringBuilder("<section epub:type=\"chapter\">\n<h1>${esc(name)}</h1>\n")
    for (day in pages) {
        body.append("<section class=\"day\">\n<h2>${esc(day.date.format(dayFormat))}</h2>\n")
        body.append("<p class=\"question\">${esc(day.question)}</p>\n")
        day.photo?.let { body.append("<div class=\"photo\"><img src=\"${addPhoto(it)}\" alt=\"${esc(day.question)}\"/></div>\n") }
        if (day.asList) {
            body.append("<ul>\n").append(day.lines.joinToString("") { "<li>${esc(it)}</li>\n" }).append("</ul>\n")
        } else {
            day.lines.forEach { body.append("<p class=\"answer\">${esc(it)}</p>\n") }
        }
        body.append("</section>\n")
    }
    body.append("</section>")
    return page(name, body.toString())
}

private fun constellationsPage(book: YearBook): String = page(
    book.strings.constellations,
    "<section epub:type=\"chapter\">\n<h1>${esc(book.strings.constellations)}</h1>\n" +
        "<p>${esc(book.strings.constellationsIntro)}</p>\n<ul>\n" +
        book.constellations.joinToString("") { "<li>${esc(it)}</li>\n" } + "</ul>\n</section>",
)

private fun page(title: String, body: String) = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" lang="en" xml:lang="en">
<head>
<meta charset="UTF-8"/>
<title>${esc(title)}</title>
<link rel="stylesheet" type="text/css" href="style.css"/>
</head>
<body>
$body
</body>
</html>
"""

private fun nav(book: YearBook, chapters: List<Pair<String, String>>, constellations: Boolean): String {
    val items = chapters.joinToString("") { (href, name) -> "<li><a href=\"$href\">${esc(name)}</a></li>\n" } +
        (if (constellations) "<li><a href=\"constellations.xhtml\">${esc(book.strings.constellations)}</a></li>\n" else "")
    return page(
        book.strings.contents,
        "<nav epub:type=\"toc\" id=\"toc\">\n<h1>${esc(book.strings.contents)}</h1>\n<ol>\n$items</ol>\n</nav>",
    )
}

private fun packageOpf(book: YearBook, chapters: List<Pair<String, String>>, photos: List<String>, constellations: Boolean): String {
    val uid = UUID.nameUUIDFromBytes("dotilda-${book.year}-${book.madeOn}".toByteArray())
    val manifest = StringBuilder()
    manifest.append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n")
    manifest.append("<item id=\"cover\" href=\"cover.xhtml\" media-type=\"application/xhtml+xml\"/>\n")
    manifest.append("<item id=\"cover-image\" href=\"images/cover.png\" media-type=\"image/png\" properties=\"cover-image\"/>\n")
    manifest.append("<item id=\"css\" href=\"style.css\" media-type=\"text/css\"/>\n")
    chapters.forEach { (id, href) -> manifest.append("<item id=\"$id\" href=\"$href\" media-type=\"application/xhtml+xml\"/>\n") }
    if (constellations) manifest.append("<item id=\"constellations\" href=\"constellations.xhtml\" media-type=\"application/xhtml+xml\"/>\n")
    photos.forEachIndexed { i, href -> manifest.append("<item id=\"photo${i + 1}\" href=\"$href\" media-type=\"image/png\"/>\n") }
    val spine = (listOf("cover", "nav") + chapters.map { it.first } + if (constellations) listOf("constellations") else emptyList())
        .joinToString("") { "<itemref idref=\"$it\"/>\n" }
    return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid" xml:lang="en">
<metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
<dc:identifier id="uid">urn:uuid:$uid</dc:identifier>
<dc:title>${esc(book.strings.title)} · ${book.year}</dc:title>
<dc:language>en</dc:language>
<dc:date>${book.madeOn}</dc:date>
<meta property="dcterms:modified">${book.madeOn}T00:00:00Z</meta>
<meta name="cover" content="cover-image"/>
</metadata>
<manifest>
$manifest</manifest>
<spine>
$spine</spine>
</package>
"""
}

private const val CONTAINER = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
<rootfiles>
<rootfile full-path="EPUB/package.opf" media-type="application/oebps-package+xml"/>
</rootfiles>
</container>
"""

/** Black on white, generous margins, nothing a reader's own settings would fight. */
private const val CSS = """body { margin: 0 5%; line-height: 1.4; }
h1 { font-weight: normal; margin: 1.5em 0 1em; }
h2 { font-size: 1.1em; margin: 2em 0 0.3em; }
p.question { font-style: italic; margin: 0 0 0.6em; }
p.answer { margin: 0 0 0.4em; }
div.photo, div.cover { text-align: center; margin: 0.5em 0; }
div.photo img { max-width: 100%; }
div.cover img { max-width: 100%; max-height: 95vh; }
"""

private fun esc(text: String) = text
    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
