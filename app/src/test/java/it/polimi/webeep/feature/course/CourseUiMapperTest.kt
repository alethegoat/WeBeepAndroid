package it.polimi.webeep.feature.course

import it.polimi.webeep.core.common.FileUtils
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.course.presentation.FileUiModel
import it.polimi.webeep.feature.course.presentation.ModuleUiModel
import it.polimi.webeep.feature.course.presentation.SectionUiModel
import it.polimi.webeep.feature.course.presentation.expansionById
import it.polimi.webeep.feature.course.presentation.mapFiles
import it.polimi.webeep.feature.course.presentation.toUiModel
import it.polimi.webeep.feature.course.presentation.toUiModels
import it.polimi.webeep.feature.course.presentation.withInFlightDownloadStates
import it.polimi.webeep.feature.download.domain.model.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CourseUiMapperTest {

    private val resource = FileResource(
        filename = "notes.pdf",
        filepath = "/",
        fileUrl = "https://webeep.polimi.it/pluginfile.php/1/notes.pdf",
        fileSize = 2048L,
        mimeType = "application/pdf",
        timeModified = 1L
    )

    private fun section(id: Int, summary: String? = null) = Section(
        id = id,
        name = "Section $id",
        summary = summary,
        sectionNumber = id,
        modules = listOf(
            Module(
                id = id * 10,
                name = "Notes",
                modName = "resource",
                iconUrl = null,
                description = "<p>Read <b>this</b> &amp; that</p>",
                url = null,
                files = listOf(resource)
            )
        )
    )

    @Test
    fun `file mapping builds the local name, size text and disk state`() {
        val onDisk = resource.toUiModel { true }
        val missing = resource.toUiModel { false }

        assertEquals(FileUtils.buildLocalFileName("notes.pdf", resource.fileUrl), onDisk.localFileName)
        assertEquals("2.0 KB", onDisk.formattedSize)
        assertEquals(DownloadState.COMPLETED, onDisk.downloadState)
        assertEquals(DownloadState.NOT_STARTED, missing.downloadState)
    }

    @Test
    fun `the downloaded check receives the local name`() {
        val asked = mutableListOf<String>()

        resource.toUiModel {
            asked += it
            false
        }

        assertEquals(listOf(FileUtils.buildLocalFileName("notes.pdf", resource.fileUrl)), asked)
    }

    @Test
    fun `html is cleaned and blank text becomes null`() {
        val mapped = section(id = 1, summary = "<p> </p>").toUiModel(isExpanded = true) { false }

        assertNull(mapped.summary)
        assertEquals("Read this & that", mapped.modules.single().description)
    }

    @Test
    fun `only the first section starts expanded unless a previous state says otherwise`() {
        val sections = listOf(section(1), section(2), section(3))

        val fresh = sections.toUiModels(emptyMap()) { false }
        val refreshed = sections.toUiModels(mapOf(1 to false, 3 to true)) { false }

        assertEquals(listOf(true, false, false), fresh.map { it.isExpanded })
        assertEquals(listOf(false, false, true), refreshed.map { it.isExpanded })
    }

    @Test
    fun `expansionById lists every section`() {
        val mapped = listOf(section(1), section(2)).toUiModels(emptyMap()) { false }

        assertEquals(mapOf(1 to true, 2 to false), mapped.expansionById())
    }

    @Test
    fun `mapFiles rewrites every file and keeps the rest`() {
        val mapped = listOf(section(1)).toUiModels(emptyMap()) { false }

        val renamed = mapped.mapFiles { it.copy(filename = "x.pdf") }

        assertEquals("x.pdf", renamed.single().modules.single().files.single().filename)
        assertEquals("Section 1", renamed.single().name)
    }

    @Test
    fun `a rebuild carries over DOWNLOADING and FAILED`() {
        val previous = sections(DownloadState.DOWNLOADING, DownloadState.FAILED)
        val rebuilt = sections(DownloadState.NOT_STARTED, DownloadState.NOT_STARTED)

        val states = rebuilt.withInFlightDownloadStates(previous).files().map { it.downloadState }

        assertEquals(listOf(DownloadState.DOWNLOADING, DownloadState.FAILED), states)
    }

    @Test
    fun `a rebuild does not resurrect FAILED for a file now on disk`() {
        val previous = sections(DownloadState.FAILED, DownloadState.NOT_STARTED)
        val rebuilt = sections(DownloadState.COMPLETED, DownloadState.NOT_STARTED)

        val states = rebuilt.withInFlightDownloadStates(previous).files().map { it.downloadState }

        assertEquals(listOf(DownloadState.COMPLETED, DownloadState.NOT_STARTED), states)
    }

    @Test
    fun `a rebuild without anything in flight is unchanged`() {
        val rebuilt = sections(DownloadState.COMPLETED, DownloadState.NOT_STARTED)

        assertEquals(rebuilt, rebuilt.withInFlightDownloadStates(sections(DownloadState.COMPLETED, DownloadState.COMPLETED)))
    }

    private fun sections(first: DownloadState, second: DownloadState) = listOf(
        SectionUiModel(
            id = 1,
            name = "S",
            summary = null,
            modules = listOf(
                ModuleUiModel(
                    id = 1,
                    name = "M",
                    type = "resource",
                    iconUrl = null,
                    description = null,
                    url = null,
                    files = listOf(file("a.pdf", first), file("b.pdf", second))
                )
            )
        )
    )

    private fun file(name: String, state: DownloadState) = FileUiModel(
        filename = name,
        filepath = "/",
        localFileName = name,
        fileUrl = "https://example.com/$name",
        fileSize = 1L,
        mimeType = null,
        formattedSize = "1 B",
        downloadState = state
    )

    private fun List<SectionUiModel>.files() = flatMap { it.modules }.flatMap { it.files }
}
