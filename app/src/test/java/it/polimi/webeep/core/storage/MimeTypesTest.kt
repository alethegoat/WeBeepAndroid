package it.polimi.webeep.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MimeTypesTest {

    @Test
    fun `known extensions resolve case insensitively`() {
        assertEquals("application/pdf", mimeTypeForFileName("lesson.pdf"))
        assertEquals("application/pdf", mimeTypeForFileName("LESSON.PDF"))
    }

    @Test
    fun `names with spaces still resolve`() {
        assertEquals("application/pdf", mimeTypeForFileName("Lecture notes week 1-ab12cd.pdf"))
    }

    @Test
    fun `only the text after the last dot counts`() {
        assertEquals("application/pdf", mimeTypeForFileName("v1.2 final.pdf"))
    }

    @Test
    fun `unknown or missing extensions fall back to a wildcard`() {
        assertEquals("*/*", mimeTypeForFileName("archive.unknownext"))
        assertEquals("*/*", mimeTypeForFileName("README"))
        assertEquals("*/*", mimeTypeForFileName("trailing."))
    }
}
