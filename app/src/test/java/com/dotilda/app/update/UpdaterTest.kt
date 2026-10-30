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
