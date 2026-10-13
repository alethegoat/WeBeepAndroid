package it.polimi.webeep.feature.course.presentation

import it.polimi.webeep.core.common.FileUtils
import it.polimi.webeep.core.common.MoodleText
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.download.domain.model.DownloadState

/**
 * Maps domain sections to UI models. The first section starts open unless [wasExpanded] says
 * otherwise, so a refresh keeps what the user opened or closed. HTML is cleaned here, off the
 * main thread, never while rows compose.
 */
internal fun List<Section>.toUiModels(
    wasExpanded: Map<Int, Boolean>,
    isDownloaded: (localFileName: String) -> Boolean
): List<SectionUiModel> = mapIndexed { index, section ->
    section.toUiModel(isExpanded = wasExpanded[section.id] ?: (index == 0), isDownloaded = isDownloaded)
}

internal fun Section.toUiModel(isExpanded: Boolean, isDownloaded: (localFileName: String) -> Boolean) = SectionUiModel(
    id = id,
    name = name,
    summary = MoodleText.cleanHtml(summary).takeIf { it.isNotBlank() },
    modules = modules.map { it.toUiModel(isDownloaded) },
    isExpanded = isExpanded
)

internal fun Module.toUiModel(isDownloaded: (localFileName: String) -> Boolean) = ModuleUiModel(
    id = id,
    name = name,
    type = modName,
    iconUrl = iconUrl,
    description = MoodleText.cleanHtml(description).takeIf { it.isNotBlank() },
    url = url,
    files = files.map { it.toUiModel(isDownloaded) }
)

internal fun FileResource.toUiModel(isDownloaded: (localFileName: String) -> Boolean): FileUiModel {
    val localFileName = FileUtils.buildLocalFileName(filename, fileUrl)
    return FileUiModel(
        filename = filename,
        filepath = filepath,
        localFileName = localFileName,
        fileUrl = fileUrl,
        fileSize = fileSize,
        mimeType = mimeType,
        formattedSize = FileUtils.formatFileSize(fileSize),
        downloadState = if (isDownloaded(localFileName)) DownloadState.COMPLETED else DownloadState.NOT_STARTED
    )
}

/** Expansion of every section, to carry over when the contents are rebuilt. */
internal fun List<SectionUiModel>.expansionById(): Map<Int, Boolean> = associate { it.id to it.isExpanded }

/** The same tree with [transform] applied to every file. */
internal fun List<SectionUiModel>.mapFiles(transform: (FileUiModel) -> FileUiModel): List<SectionUiModel> = map { section ->
    section.copy(
        modules = section.modules.map { module -> module.copy(files = module.files.map(transform)) }
    )
}

/**
 * A cache re-emit rebuilds every [FileUiModel] from disk state only: carry over
 * DOWNLOADING (the job is still running) and FAILED (unless the file is now on disk).
 */
internal fun List<SectionUiModel>.withInFlightDownloadStates(previous: List<SectionUiModel>): List<SectionUiModel> {
    val carriedStates = mutableMapOf<String, DownloadState>()
    previous.forEach { section ->
        section.modules.forEach { module ->
            module.files
                .filter { it.downloadState == DownloadState.DOWNLOADING || it.downloadState == DownloadState.FAILED }
                .forEach { carriedStates[it.fileUrl] = it.downloadState }
        }
    }
    if (carriedStates.isEmpty()) return this
    return mapFiles { file ->
        when (carriedStates[file.fileUrl]) {
            DownloadState.DOWNLOADING -> file.copy(downloadState = DownloadState.DOWNLOADING)
            DownloadState.FAILED ->
                if (file.downloadState == DownloadState.COMPLETED) file else file.copy(downloadState = DownloadState.FAILED)
            else -> file
        }
    }
}
