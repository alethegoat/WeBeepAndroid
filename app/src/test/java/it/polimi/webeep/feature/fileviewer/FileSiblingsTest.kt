package it.polimi.webeep.feature.fileviewer

import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.fileviewer.domain.findSiblings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSiblingsTest {

    private val sections = listOf(
        Section(
            id = 1,
            name = "Lezioni",
            summary = null,
            sectionNumber = 1,
            modules = listOf(
                module(10, "resource", file("l1.pdf", "/")),
                module(11, "url"),
                module(12, "resource", file("l2.pdf", "/")),
                module(
                    13,
                    "folder",
                    file("a.pdf", "/"),
                    file("b.pdf", "/"),
                    file("sol.pdf", "/Soluzioni/")
                )
            )
        ),
        Section(
            id = 2,
            name = "Lab",
            summary = null,
            sectionNumber = 2,
            modules = listOf(module(20, "resource", file("lab.pdf", "/")))
        )
    )

    @Test
    fun singleFileModulesSwipeWithinTheirSection() {
        val siblings = findSiblings(sections, url("l2.pdf", "/"))
        assertEquals(listOf("l1.pdf", "l2.pdf"), siblings.map { it.title })
    }

    @Test
    fun folderFilesSwipeWithinTheSameDirectory() {
        assertEquals(listOf("a.pdf", "b.pdf"), findSiblings(sections, url("b.pdf", "/")).map { it.title })
        assertEquals(listOf("sol.pdf"), findSiblings(sections, url("sol.pdf", "/Soluzioni/")).map { it.title })
    }

    @Test
    fun tokenInRequestedUrlIsIgnored() {
        val siblings = findSiblings(sections, url("lab.pdf", "/") + "?token=secret")
        assertEquals(listOf("lab.pdf"), siblings.map { it.title })
    }

    @Test
    fun unknownFileHasNoSiblings() {
        assertTrue(findSiblings(sections, "https://webeep.polimi.it/pluginfile.php/9/missing.pdf").isEmpty())
    }

    private fun module(id: Int, type: String, vararg files: FileResource) = Module(
        id = id,
        name = "m$id",
        modName = type,
        iconUrl = null,
        description = null,
        url = if (type == "url") "https://example.com" else null,
        files = files.toList()
    )

    private fun url(name: String, path: String) = "https://webeep.polimi.it/pluginfile.php/1$path$name"

    private fun file(name: String, path: String) = FileResource(
        filename = name,
        filepath = path,
        fileUrl = url(name, path),
        fileSize = 10L,
        mimeType = "application/pdf",
        timeModified = null
    )
}
