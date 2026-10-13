package it.polimi.webeep.feature.fileviewer.domain

import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.ModuleType
import it.polimi.webeep.feature.course.domain.model.Section

/**
 * Files the viewer can swipe through, in course order, starting from [fileUrl]:
 * - a file inside a folder or multi-file module: files of that module in the same directory;
 * - a single-file module: the single-file modules of the same section.
 * Returns an empty list when [fileUrl] is not found.
 */
fun findSiblings(sections: List<Section>, fileUrl: String): List<ViewerFile> {
    val target = MoodleFileUrlBuilder.stripAuthFromUrl(fileUrl)
    val (section, module, file) = sections.firstNotNullOfOrNull { section ->
        section.modules
            .filter { it.modName !in ModuleType.Link }
            .firstNotNullOfOrNull { module ->
                module.files.firstOrNull { it.matches(target) }?.let { Triple(section, module, it) }
            }
    } ?: return emptyList()
    val siblings = if (module.isSingleFile()) {
        section.modules.filter { it.isSingleFile() }.map { it.files.first() }
    } else {
        module.files.filter { it.filepath == file.filepath }
    }
    return siblings.map { it.toViewerFile() }
}

private fun Module.isSingleFile(): Boolean = modName != ModuleType.FOLDER && modName !in ModuleType.Link && files.size == 1

private fun FileResource.matches(strippedUrl: String): Boolean = MoodleFileUrlBuilder.stripAuthFromUrl(fileUrl) == strippedUrl

private fun FileResource.toViewerFile() = ViewerFile(
    url = MoodleFileUrlBuilder.stripAuthFromUrl(fileUrl),
    title = filename,
    mimeType = mimeType.orEmpty(),
    size = fileSize
)
