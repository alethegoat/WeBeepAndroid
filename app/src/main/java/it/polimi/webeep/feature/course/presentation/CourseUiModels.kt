package it.polimi.webeep.feature.course.presentation

import androidx.compose.runtime.Immutable
import it.polimi.webeep.feature.download.domain.model.DownloadState

/** A section as shown on screen; [isExpanded] is forced on while a search matches it. */
@Immutable
data class SectionUiModel(
    val id: Int,
    val name: String,
    val summary: String?,
    val modules: List<ModuleUiModel>,
    val isExpanded: Boolean = false
)

/** A module as shown on screen; [type] is the Moodle module type. */
@Immutable
data class ModuleUiModel(
    val id: Int,
    val name: String,
    val type: String,
    val iconUrl: String?,
    val description: String?,
    val url: String?,
    val files: List<FileUiModel>
)

/** A file as shown on screen. [localFileName] is the collision-free name used for the downloaded copy; [fileUrl] is token-free. */
@Immutable
data class FileUiModel(
    val filename: String,
    val filepath: String,
    val localFileName: String,
    val fileUrl: String,
    val fileSize: Long,
    val mimeType: String?,
    val formattedSize: String,
    val downloadState: DownloadState = DownloadState.NOT_STARTED
)
