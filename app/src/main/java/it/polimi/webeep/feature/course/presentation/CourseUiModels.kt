package it.polimi.webeep.feature.course.presentation

import androidx.compose.runtime.Immutable
import it.polimi.webeep.feature.download.domain.model.DownloadState

@Immutable
data class SectionUiModel(
    val id: Int,
    val name: String,
    val summary: String?,
    val modules: List<ModuleUiModel>,
    val isExpanded: Boolean = false
)

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
