package it.polimi.webeep.feature.course

import it.polimi.webeep.feature.course.presentation.FileUiModel
import it.polimi.webeep.feature.course.presentation.ModuleUiModel
import it.polimi.webeep.feature.course.presentation.SectionUiModel
import it.polimi.webeep.feature.course.presentation.filterSections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseContentSearchTest {

    private val sections = listOf(
        SectionUiModel(
            id = 1,
            name = "Lezione 1",
            summary = "Introduzione",
            isExpanded = false,
            modules = listOf(
                module(10, "Slide PDF", files = listOf(file("intro.pdf"))),
                module(11, "Esercizi", files = listOf(file("esercizi.docx")))
            )
        ),
        SectionUiModel(
            id = 2,
            name = "Laboratorio",
            summary = null,
            isExpanded = false,
            modules = listOf(
                module(20, "Codice", files = listOf(file("main.kt")))
            )
        )
    )

    @Test
    fun emptyQueryReturnsOriginalExpansion() {
        val result = filterSections(sections, "")
        assertEquals(sections, result)
        assertTrue(result.none { it.isExpanded })
    }

    @Test
    fun matchingSectionExpandsAndKeepsAllModules() {
        val result = filterSections(sections, "laboratorio")
        assertEquals(1, result.size)
        assertEquals(2, result.first().id)
        assertTrue(result.first().isExpanded)
        assertEquals(1, result.first().modules.size)
    }

    @Test
    fun matchingFilenameFiltersModulesAndExpands() {
        val result = filterSections(sections, "intro.pdf")
        assertEquals(1, result.size)
        assertEquals(listOf(10), result.first().modules.map { it.id })
        assertTrue(result.first().isExpanded)
    }

    @Test
    fun noMatchReturnsEmpty() {
        assertTrue(filterSections(sections, "xyz-not-found").isEmpty())
    }

    private fun module(id: Int, name: String, files: List<FileUiModel>) = ModuleUiModel(
        id = id,
        name = name,
        type = "resource",
        iconUrl = null,
        description = null,
        url = null,
        files = files
    )

    private fun file(name: String) = FileUiModel(
        filename = name,
        filepath = "/",
        localFileName = name,
        fileUrl = "https://webeep.polimi.it/$name",
        fileSize = 1L,
        mimeType = "application/octet-stream",
        formattedSize = "1 B"
    )
}
