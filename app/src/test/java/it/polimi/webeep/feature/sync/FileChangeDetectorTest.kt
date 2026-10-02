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

        assertTrue(FileChangeDetector.detect(previous = null, current = snap(current)).isEmpty())
    }

    @Test
    fun `unchanged course reports nothing`() {
        val snapshot = course(module(10, "resource", file("a.pdf", rev = 1, time = 100)))

        assertTrue(FileChangeDetector.detect(snap(snapshot), snap(snapshot)).isEmpty())
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
            listOf(FileChange(10, "/", "b.pdf", ChangeType.ADDED)),
            FileChangeDetector.detect(snap(previous), snap(current))
        )
    }

    @Test
    fun `reports a re-uploaded file as updated`() {
        val previous = course(module(10, "resource", file("a.pdf", rev = 1, time = 100)))
        val current = course(module(10, "resource", file("a.pdf", rev = 2, time = 300)))

        assertEquals(
            listOf(FileChange(10, "/", "a.pdf", ChangeType.UPDATED)),
            FileChangeDetector.detect(snap(previous), snap(current))
        )
    }

    @Test
    fun `reports size change as updated when timestamps are missing`() {
        val previous = course(module(10, "resource", file("a.pdf", rev = 1, time = 0, size = 10)))
        val current = course(module(10, "resource", file("a.pdf", rev = 1, time = 0, size = 20)))

        assertEquals(
            listOf(FileChange(10, "/", "a.pdf", ChangeType.UPDATED)),
            FileChangeDetector.detect(snap(previous), snap(current))
        )
    }

    @Test
    fun `reports files of a new module and of a previously empty course`() {
        val emptySection = listOf(Section(id = 1, name = "General", summary = null, sectionNumber = 0, modules = emptyList()))
        val current = course(module(11, "resource", file("new.pdf", rev = 1, time = 100)))

        assertEquals(
            listOf(FileChange(11, "/", "new.pdf", ChangeType.ADDED)),
            FileChangeDetector.detect(snap(emptySection), snap(current))
        )
    }

    @Test
    fun `ignores links and removed files`() {
        val previous = course(module(10, "resource", file("old.pdf", rev = 1, time = 100)))
        val current = course(module(20, "url", file("Recording", rev = 1, time = 0)))

        assertTrue(FileChangeDetector.detect(snap(previous), snap(current)).isEmpty())
    }

    @Test
    fun `merge keeps unseen changes and lists newest first`() {
        val older = listOf(change("a.pdf", ChangeType.ADDED), change("b.pdf", ChangeType.UPDATED))
        val newer = listOf(change("c.pdf", ChangeType.ADDED))

        assertEquals(
            listOf(change("c.pdf", ChangeType.ADDED), change("a.pdf", ChangeType.ADDED), change("b.pdf", ChangeType.UPDATED)),
            FileChangeDetector.mergeChanges(older, newer)
        )
    }

    @Test
    fun `merge keeps a file new when it is updated before being seen`() {
        val older = listOf(change("a.pdf", ChangeType.ADDED))
        val newer = listOf(change("a.pdf", ChangeType.UPDATED))

        assertEquals(listOf(change("a.pdf", ChangeType.ADDED)), FileChangeDetector.mergeChanges(older, newer))
    }

    private fun change(name: String, type: ChangeType) = FileChange(10, "/", name, type)

    private fun snap(sections: List<Section>) = FileChangeDetector.snapshot(sections)

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
