package it.polimi.webeep.feature.sync

import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.sync.FileChangeDetector.ChangeType
import it.polimi.webeep.feature.sync.FileChangeDetector.FileChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileChangeDetectorTest {

    @Test
    fun `first sync of a course reports nothing`() {
        val current = course(module(10, "folder", file("a.pdf", rev = 1, time = 100)))

        assertTrue(FileChangeDetector.detect(previous = emptyList(), current = current).isEmpty())
    }

    @Test
    fun `unchanged course reports nothing`() {
        val snapshot = course(module(10, "resource", file("a.pdf", rev = 1, time = 100)))

        assertTrue(FileChangeDetector.detect(snapshot, snapshot).isEmpty())
    }

    @Test
    fun `reports only the file added to a folder even if the folder revision changes`() {
        val previous = course(module(10, "folder", file("a.pdf", rev = 1, time = 100)))
        val current = course(
            module(
                10,
                "folder",
                file("a.pdf", rev = 2, time = 100),
                file("b.pdf", rev = 2, time = 200)
            )
        )

        assertEquals(
            listOf(FileChange("b.pdf", ChangeType.ADDED)),
            FileChangeDetector.detect(previous, current)
        )
    }

    @Test
    fun `reports a re-uploaded file as updated`() {
        val previous = course(module(10, "resource", file("a.pdf", rev = 1, time = 100)))
        val current = course(module(10, "resource", file("a.pdf", rev = 2, time = 300)))

        assertEquals(
            listOf(FileChange("a.pdf", ChangeType.UPDATED)),
            FileChangeDetector.detect(previous, current)
        )
    }

    @Test
    fun `reports size change as updated when timestamps are missing`() {
        val previous = course(module(10, "resource", file("a.pdf", rev = 1, time = 0, size = 10)))
        val current = course(module(10, "resource", file("a.pdf", rev = 1, time = 0, size = 20)))

        assertEquals(
            listOf(FileChange("a.pdf", ChangeType.UPDATED)),
            FileChangeDetector.detect(previous, current)
        )
    }

    @Test
    fun `reports files of a new module and of a previously empty course`() {
        val emptySection = listOf(Section(id = 1, name = "General", summary = null, sectionNumber = 0, modules = emptyList()))
        val current = course(module(11, "resource", file("new.pdf", rev = 1, time = 100)))

        assertEquals(
            listOf(FileChange("new.pdf", ChangeType.ADDED)),
            FileChangeDetector.detect(emptySection, current)
        )
    }

    @Test
    fun `ignores links and removed files`() {
        val previous = course(module(10, "resource", file("old.pdf", rev = 1, time = 100)))
        val current = course(module(20, "url", file("Recording", rev = 1, time = 0)))

        assertTrue(FileChangeDetector.detect(previous, current).isEmpty())
    }

    private fun course(vararg modules: Module) =
        listOf(Section(id = 1, name = "General", summary = null, sectionNumber = 0, modules = modules.toList()))

    private fun module(id: Int, modName: String, vararg files: FileResource) =
        Module(
            id = id,
            name = "Module $id",
            modName = modName,
            iconUrl = null,
            description = null,
            url = null,
            files = files.toList()
        )

    private fun file(name: String, rev: Int, time: Long, size: Long = 1_000L) =
        FileResource(
            filename = name,
            filepath = "/",
            fileUrl = "https://webeep.polimi.it/webservice/pluginfile.php/5/mod_folder/content/$rev/$name",
            fileSize = size,
            mimeType = "application/pdf",
            timeModified = time
        )
}
