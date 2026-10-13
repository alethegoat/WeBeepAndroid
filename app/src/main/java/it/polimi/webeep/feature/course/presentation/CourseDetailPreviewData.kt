package it.polimi.webeep.feature.course.presentation

import it.polimi.webeep.core.common.CourseTitleParser
import it.polimi.webeep.feature.download.domain.model.DownloadState

/** Sample course shared by the screen previews. */
private val previewSections = listOf(
    SectionUiModel(
        id = 1,
        name = "General",
        summary = "Welcome to the course",
        isExpanded = true,
        modules = listOf(
            ModuleUiModel(
                id = 10,
                name = "Syllabus",
                type = "resource",
                iconUrl = null,
                description = "Read before the first lecture",
                url = null,
                files = listOf(previewFile("syllabus.pdf", "/", DownloadState.COMPLETED))
            ),
            ModuleUiModel(
                id = 11,
                name = "Lecture notes",
                type = "folder",
                iconUrl = null,
                description = null,
                url = null,
                files = listOf(
                    previewFile("week1.pdf", "/Week 1/", DownloadState.NOT_STARTED),
                    previewFile("week2.pptx", "/Week 2/", DownloadState.DOWNLOADING)
                )
            )
        )
    ),
    SectionUiModel(id = 2, name = "Exercises", summary = null, modules = emptyList(), isExpanded = true)
)

internal val previewExpandedFolders = setOf(folderRootKey(moduleId = 11), folderKey(moduleId = 11, fullPath = "/Week 1"))

private fun previewFile(name: String, path: String, state: DownloadState) = FileUiModel(
    filename = name,
    filepath = path,
    localFileName = name,
    fileUrl = "https://example.com/$name",
    fileSize = 204_800L,
    mimeType = null,
    formattedSize = "200.0 KB",
    downloadState = state
)

internal fun previewList(folders: Set<String> = emptySet(), query: String = "") = buildCourseListItems(previewSections, folders, query)

internal fun previewCourseState(content: CourseDetailContent, query: String = "", folders: Set<String> = emptySet()) = CourseDetailUiState(
    courseId = 1,
    courseTitle = CourseTitleParser.parse("052537 - TECHNOLOGIES FOR INFORMATION SYSTEMS (TANCA LETIZIA) [2026-27]"),
    content = content,
    searchQuery = query,
    expandedFolders = folders
)
