package com.dotilda.app.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdaterTest {
    @Test
    fun comparesVersionsNumerically() {
        assertEquals(1, compareVersions("0.10.0", "0.9.1"))
        assertEquals(-1, compareVersions("0.3.0", "0.3.1"))
        assertEquals(0, compareVersions("1.0", "1.0.0"))
    }

    @Test
    fun notesBecomePlainText() {
        assertEquals("Added\n• The Galaxy", plainNotes("### Added\n\n- The **Galaxy**"))
    }
}

class PaginateTest {
    @Test
    fun keepsEveryWordAndEveryPageShort() {
        val text = (1..400).joinToString(" ") { "word$it" } + "\n\nShort paragraph.\n\n" + "x".repeat(50)
        val pages = com.dotilda.app.ui.paginate(text, 200)
        assertEquals(text.split(Regex("\\s+")).filter { it.isNotEmpty() }, pages.joinToString(" ").split(Regex("\\s+")).filter { it.isNotEmpty() })
        pages.forEach { org.junit.Assert.assertTrue(it.length <= 200) }
    }
}
