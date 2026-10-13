package it.polimi.webeep.feature.download

import it.polimi.webeep.feature.download.data.LegacyDownloadsMigration
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LegacyDownloadsMigrationTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `moves completed files and removes the legacy directory`() {
        val legacy = temp.newFolder("external", "downloads")
        val target = File(temp.root, "internal/downloads")
        File(legacy, "slides.pdf").writeText("legacy-pdf")
        File(legacy, "partial.pdf.part").writeText("partial")

        LegacyDownloadsMigration.migrate(legacy, target)

        assertEquals("legacy-pdf", File(target, "slides.pdf").readText())
        assertFalse(File(target, "partial.pdf.part").exists())
        assertFalse(legacy.exists())
    }

    @Test
    fun `keeps the internal copy when both exist`() {
        val legacy = temp.newFolder("external", "downloads")
        val target = temp.newFolder("internal", "downloads")
        File(legacy, "notes.pdf").writeText("old")
        File(target, "notes.pdf").writeText("new")

        LegacyDownloadsMigration.migrate(legacy, target)

        assertEquals("new", File(target, "notes.pdf").readText())
        assertFalse(legacy.exists())
    }

    @Test
    fun `missing legacy directory is a no-op`() {
        val target = File(temp.root, "internal/downloads")

        LegacyDownloadsMigration.migrate(File(temp.root, "absent"), target)
        LegacyDownloadsMigration.migrate(null, target)

        assertFalse(target.exists())
    }

    @Test
    fun `nested legacy directories are deleted without being copied`() {
        val legacy = temp.newFolder("external", "downloads")
        File(legacy, "nested").mkdirs()
        File(legacy, "nested/inner.pdf").writeText("inner")
        val target = File(temp.root, "internal/downloads")

        LegacyDownloadsMigration.migrate(legacy, target)

        assertTrue(target.isDirectory)
        assertEquals(0, target.listFiles()?.size ?: 0)
        assertFalse(legacy.exists())
    }
}
