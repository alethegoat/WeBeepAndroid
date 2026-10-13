package it.polimi.webeep.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUtilsTest {

    @Test
    fun `sanitizeFileName replaces separators and reserved characters`() {
        assertEquals("a_b_c_d_e_f_g_h_i_j.pdf", FileUtils.sanitizeFileName("""a\b/c:d*e?f"g<h>i|j.pdf"""))
    }

    @Test
    fun `sanitizeFileName replaces control characters`() {
        assertEquals("a_b_c.txt", FileUtils.sanitizeFileName("a\u0000b\nc.txt"))
    }

    @Test
    fun `sanitizeFileName neutralises path traversal`() {
        val result = FileUtils.sanitizeFileName("../../etc/passwd")

        assertEquals("_.._etc_passwd", result)
        assertTrue('/' !in result)
    }

    @Test
    fun `sanitizeFileName never returns a blank name`() {
        assertEquals("file", FileUtils.sanitizeFileName(""))
        assertEquals("file", FileUtils.sanitizeFileName("   "))
        assertEquals("file", FileUtils.sanitizeFileName("..."))
    }

    @Test
    fun `sanitizeFileName trims surrounding whitespace and dots`() {
        assertEquals("notes.pdf", FileUtils.sanitizeFileName("  .notes.pdf. "))
    }

    @Test
    fun `stableUrlHash is deterministic and twelve hex characters long`() {
        val hash = FileUtils.stableUrlHash("https://webeep.polimi.it/a.pdf")

        assertEquals(hash, FileUtils.stableUrlHash("https://webeep.polimi.it/a.pdf"))
        assertTrue(Regex("[0-9a-f]{12}").matches(hash))
    }

    @Test
    fun `stableUrlHash differs between urls`() {
        assertNotEquals(FileUtils.stableUrlHash("https://a/1.pdf"), FileUtils.stableUrlHash("https://a/2.pdf"))
    }

    @Test
    fun `buildLocalFileName keeps the extension after the hash`() {
        val hash = FileUtils.stableUrlHash("https://a/slides.pdf")

        assertEquals("slides-$hash.pdf", FileUtils.buildLocalFileName("slides.pdf", "https://a/slides.pdf"))
    }

    @Test
    fun `buildLocalFileName appends the hash when there is no extension`() {
        val hash = FileUtils.stableUrlHash("https://a/readme")

        assertEquals("readme-$hash", FileUtils.buildLocalFileName("readme", "https://a/readme"))
    }

    @Test
    fun `buildLocalFileName treats a leading or trailing dot as no extension`() {
        val hash = FileUtils.stableUrlHash("u")

        assertEquals("hidden-$hash", FileUtils.buildLocalFileName(".hidden", "u"))
        assertEquals("trailing-$hash", FileUtils.buildLocalFileName("trailing.", "u"))
    }

    @Test
    fun `buildLocalFileName truncates long base names but keeps the extension`() {
        val name = FileUtils.buildLocalFileName("a".repeat(200) + ".pdf", "u")

        assertEquals("a".repeat(80) + "-" + FileUtils.stableUrlHash("u") + ".pdf", name)
    }

    @Test
    fun `buildLocalFileName gives different names to same-named files at different urls`() {
        assertNotEquals(
            FileUtils.buildLocalFileName("slides.pdf", "https://a/1/slides.pdf"),
            FileUtils.buildLocalFileName("slides.pdf", "https://a/2/slides.pdf")
        )
    }

    @Test
    fun `formatFileSize formats each unit`() {
        assertEquals("0 B", FileUtils.formatFileSize(0))
        assertEquals("0 B", FileUtils.formatFileSize(-5))
        assertEquals("1.0 B", FileUtils.formatFileSize(1))
        assertEquals("1023.0 B", FileUtils.formatFileSize(1023))
        assertEquals("1.0 KB", FileUtils.formatFileSize(1024))
        assertEquals("1.5 KB", FileUtils.formatFileSize(1536))
        assertEquals("1.0 MB", FileUtils.formatFileSize(1024L * 1024))
        assertEquals("1.0 GB", FileUtils.formatFileSize(1024L * 1024 * 1024))
        assertEquals("1.0 TB", FileUtils.formatFileSize(1024L * 1024 * 1024 * 1024))
    }
}
