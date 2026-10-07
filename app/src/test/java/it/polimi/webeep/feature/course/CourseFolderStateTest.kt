package it.polimi.webeep.feature.course

import it.polimi.webeep.feature.course.presentation.FileUiModel
import it.polimi.webeep.feature.course.presentation.ModuleUiModel
import it.polimi.webeep.feature.course.presentation.SectionUiModel
import it.polimi.webeep.feature.course.presentation.folderKey
import it.polimi.webeep.feature.course.presentation.folderRootKey
import it.polimi.webeep.feature.course.presentation.foldersWithMatches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseFolderStateTest {

    private val sections = listOf(
        SectionUiModel(
            id = 1,
            name = "Lezioni",
            summary = null,
            modules = listOf(
                ModuleUiModel(
                    id = 30,
                    name = "Materiale",
                    type = "folder",
                    iconUrl = null,
                    description = null,
                    url = null,
                    files = listOf(
                        file("readme.txt", "/"),
                        file("sol1.pdf", "/Esercizi/Soluzioni/"),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun matchOpensEveryFolderOnItsPath() {
        assertEquals(
            setOf(folderRootKey(30), folderKey(30, "/Esercizi"), folderKey(30, "/Esercizi/Soluzioni")),
            foldersWithMatches(sections, "sol1"),
        )
    }

    @Test
    fun rootLevelMatchOpensOnlyTheModule() {
        assertEquals(setOf(folderRootKey(30)), foldersWithMatches(sections, "README"))
    }

    @Test
    fun blankQueryOpensNothing() {
        assertTrue(foldersWithMatches(sections, "  ").isEmpty())
    }

    private fun file(name: String, path: String) = FileUiModel(
        filename = name,
        filepath = path,
        localFileName = name,
        fileUrl = "https://webeep.polimi.it/pluginfile.php/1$path$name",
        fileSize = 1L,
        mimeType = "application/octet-stream",
        formattedSize = "1 B",
    )
}
