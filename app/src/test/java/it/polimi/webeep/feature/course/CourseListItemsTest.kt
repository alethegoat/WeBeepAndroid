package it.polimi.webeep.feature.course

import it.polimi.webeep.core.ui.components.GroupPosition
import it.polimi.webeep.feature.course.presentation.CourseListItem
import it.polimi.webeep.feature.course.presentation.FileUiModel
import it.polimi.webeep.feature.course.presentation.ModuleKind
import it.polimi.webeep.feature.course.presentation.ModuleUiModel
import it.polimi.webeep.feature.course.presentation.SectionUiModel
import it.polimi.webeep.feature.course.presentation.buildCourseListItems
import it.polimi.webeep.feature.course.presentation.folderKey
import it.polimi.webeep.feature.course.presentation.folderRootKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseListItemsTest {

    private val folder = ModuleUiModel(
        id = 30,
        name = "Materiale",
        type = "folder",
        iconUrl = null,
        description = null,
        url = null,
        files = listOf(
            file("readme.txt", "/"),
            file("sol1.pdf", "/Esercizi/Soluzioni/")
        )
    )

    private val singleFile = ModuleUiModel(
        id = 31,
        name = "Slide",
        type = "resource",
        iconUrl = null,
        description = null,
        url = null,
        files = listOf(file("slide.pdf", "/"))
    )

    private val multiFile = ModuleUiModel(
        id = 32,
        name = "Esercitazioni",
        type = "resource",
        iconUrl = null,
        description = null,
        url = null,
        files = listOf(file("es1.pdf", "/"), file("es2.pdf", "/"))
    )

    private val sections = listOf(
        SectionUiModel(id = 1, name = "Lezioni", summary = null, modules = listOf(folder, singleFile), isExpanded = true),
        SectionUiModel(id = 2, name = "Esercitazioni", summary = null, modules = listOf(multiFile), isExpanded = false),
        SectionUiModel(id = 3, name = "Avvisi", summary = null, modules = emptyList(), isExpanded = true)
    )

    @Test
    fun collapsedFolderShowsOnlyModules() {
        val items = buildCourseListItems(sections, expandedFolders = emptySet(), query = "").items

        assertEquals(
            listOf("section:1", "module:30", "module:31", "section:2", "section:3", "section-empty:3"),
            items.map { it.key }
        )
        val folderRow = items[1] as CourseListItem.Module
        assertEquals(ModuleKind.Folder, folderRow.kind)
        assertFalse(folderRow.isFolderExpanded)
    }

    @Test
    fun positionsRoundOnlyTheEdgesOfEachSection() {
        val items = buildCourseListItems(sections, expandedFolders = emptySet(), query = "").items

        assertEquals(
            listOf(
                GroupPosition.Top,
                GroupPosition.Middle,
                GroupPosition.Bottom,
                GroupPosition.Single,
                GroupPosition.Top,
                GroupPosition.Bottom
            ),
            items.map { it.position }
        )
    }

    @Test
    fun openFoldersAddTheirTreeWithDepth() {
        val items = buildCourseListItems(
            sections,
            expandedFolders = setOf(folderRootKey(30), folderKey(30, "/Esercizi")),
            query = ""
        ).items

        val keys = items.map { it.key }
        assertEquals(
            listOf(
                "section:1",
                "module:30",
                "file:30:/readme.txt",
                "folder:30:/Esercizi",
                "folder:30:/Esercizi/Soluzioni",
                "module:31"
            ),
            keys.take(6)
        )
        val nested = items[4] as CourseListItem.Subfolder
        assertEquals(2, nested.depth)
        assertFalse(nested.isExpanded)
    }

    @Test
    fun multiFileModulesAlwaysListTheirFiles() {
        val expanded = sections.map { it.copy(isExpanded = true) }

        val keys = buildCourseListItems(expanded, expandedFolders = emptySet(), query = "").items.map { it.key }

        assertTrue("file:32:/es1.pdf" in keys)
        assertTrue("file:32:/es2.pdf" in keys)
    }

    @Test
    fun searchOpensTheFoldersLeadingToMatches() {
        val content = buildCourseListItems(sections, expandedFolders = emptySet(), query = "sol1")

        assertEquals(
            listOf(
                "section:1",
                "module:30",
                // A matching module keeps its whole tree, as before flattening.
                "file:30:/readme.txt",
                "folder:30:/Esercizi",
                "folder:30:/Esercizi/Soluzioni",
                "file:30:/Esercizi/Soluzioni/sol1.pdf"
            ),
            content.items.map { it.key }
        )
        assertFalse(content.noMatches)
    }

    @Test
    fun searchWithoutResultsReportsNoMatches() {
        val content = buildCourseListItems(sections, expandedFolders = emptySet(), query = "zzz")

        assertTrue(content.items.isEmpty())
        assertTrue(content.noMatches)
    }

    @Test
    fun duplicateFilesGetDistinctKeys() {
        val duplicated = listOf(
            sections.first().copy(
                modules = listOf(multiFile.copy(files = listOf(file("es1.pdf", "/"), file("es1.pdf", "/"))))
            )
        )

        val keys = buildCourseListItems(duplicated, expandedFolders = emptySet(), query = "").items.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
    }

    private fun file(name: String, path: String) = FileUiModel(
        filename = name,
        filepath = path,
        localFileName = name,
        fileUrl = "https://example.invalid$path$name",
        fileSize = 1L,
        mimeType = null,
        formattedSize = "1 B"
    )
}
