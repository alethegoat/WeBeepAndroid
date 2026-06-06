package it.polimi.webeep.feature.course.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.course.domain.GetCourseContentsUseCase
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.download.domain.DownloadFileUseCase
import it.polimi.webeep.feature.download.domain.DownloadRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import it.polimi.webeep.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import it.polimi.webeep.core.util.FileUtils

sealed interface CourseDetailUiState {
    data object Loading : CourseDetailUiState
    data class Success(
        val courseName: String,
        val sections: List<SectionUiModel>
    ) : CourseDetailUiState
    data class Error(val message: String) : CourseDetailUiState
}

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

enum class DownloadState { NOT_STARTED, DOWNLOADING, COMPLETED, FAILED }

/**
 * viewmodel for the course detail screen, managing state and downloading logic.
 */
@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val getCourseContentsUseCase: GetCourseContentsUseCase,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val downloadRepository: DownloadRepository,
    private val courseRepository: CourseRepository,
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val courseId: Int = checkNotNull(savedStateHandle["courseId"]) {
        "course id not passed to navigator"
    }

    private val _uiState = MutableStateFlow<CourseDetailUiState>(CourseDetailUiState.Loading)
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    private var courseName: String = ""

    // map to keep track of current download jobs for structured concurrency
    private val downloadJobs = ConcurrentHashMap<String, Job>()

    init {
        loadCourseName()
        loadContents()
    }

    private fun loadCourseName() {
        viewModelScope.launch {
            val name = courseRepository.getCourseName(courseId)
            if (name != null) {
                courseName = name
            }
        }
    }

    fun loadContents() {
        viewModelScope.launch {
            _uiState.update { CourseDetailUiState.Loading }
            getCourseContentsUseCase(courseId).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { CourseDetailUiState.Loading }
                    }
                    is Resource.Success -> {
                        val uiSections = kotlinx.coroutines.withContext(ioDispatcher) {
                            val currentExpandedStates = if (_uiState.value is CourseDetailUiState.Success) {
                                (_uiState.value as CourseDetailUiState.Success).sections.associate { it.id to it.isExpanded }
                            } else {
                                emptyMap()
                            }
                            resource.data.mapIndexed { index, section ->
                                val wasExpanded = currentExpandedStates[section.id] ?: (index == 0)
                                section.toUiModel(wasExpanded)
                            }
                        }
                        _uiState.update { 
                            CourseDetailUiState.Success(
                                courseName = courseName,
                                sections = uiSections
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { CourseDetailUiState.Error(resource.message) }
                    }
                }
            }
        }
    }

    fun toggleSection(sectionId: Int) {
        _uiState.update { state ->
            if (state is CourseDetailUiState.Success) {
                val updatedSections = state.sections.map { section ->
                    if (section.id == sectionId) {
                        section.copy(isExpanded = !section.isExpanded)
                    } else {
                        section
                    }
                }
                state.copy(sections = updatedSections)
            } else {
                state
            }
        }
    }

    fun downloadFile(file: FileUiModel) {
        // cancel any previous download for the same file to prevent duplicates
        downloadJobs[file.fileUrl]?.cancel()

        val job = viewModelScope.launch {
            updateFileDownloadState(file.fileUrl, DownloadState.DOWNLOADING)
            try {
                val downloadId = downloadFileUseCase.execute(
                    url = file.fileUrl,
                    localFileName = file.localFileName,
                    displayName = file.filename,
                    mimeType = file.mimeType
                )
                
                downloadRepository.getDownloadProgress(downloadId).collect { progress ->
                    when (progress) {
                        100 -> {
                            updateFileDownloadState(file.fileUrl, DownloadState.COMPLETED)
                            downloadJobs.remove(file.fileUrl)
                        }
                        -1 -> {
                            updateFileDownloadState(file.fileUrl, DownloadState.FAILED)
                            downloadJobs.remove(file.fileUrl)
                        }
                        else -> updateFileDownloadState(file.fileUrl, DownloadState.DOWNLOADING)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                updateFileDownloadState(file.fileUrl, DownloadState.FAILED)
                downloadJobs.remove(file.fileUrl)
            }
        }
        downloadJobs[file.fileUrl] = job
    }

    fun openFile(file: FileUiModel) {
        downloadRepository.openFile(file.localFileName)
    }

    private fun updateFileDownloadState(fileUrl: String, newState: DownloadState) {
        // recursively update the ui state by replacing only the affected file
        _uiState.update { state ->
            if (state is CourseDetailUiState.Success) {
                val updatedSections = state.sections.map { section ->
                    section.copy(
                        modules = section.modules.map { module ->
                            module.copy(
                                files = module.files.map { f ->
                                    if (f.fileUrl == fileUrl) f.copy(downloadState = newState) else f
                                }
                            )
                        }
                    )
                }
                state.copy(sections = updatedSections)
            } else {
                state
            }
        }
    }

    private fun Section.toUiModel(isExpanded: Boolean) = SectionUiModel(
        id = id,
        name = name,
        summary = summary,
        modules = modules.map { it.toUiModel() },
        isExpanded = isExpanded
    )

    private fun Module.toUiModel() = ModuleUiModel(
        id = id,
        name = name,
        type = modName,
        iconUrl = iconUrl,
        description = description,
        url = url,
        files = files.map { it.toUiModel() }
    )

    private fun FileResource.toUiModel() = FileUiModel(
        filename = filename,
        filepath = filepath,
        localFileName = FileUtils.buildLocalFileName(filename, fileUrl),
        fileUrl = fileUrl,
        fileSize = fileSize,
        mimeType = mimeType,
        formattedSize = FileUtils.formatFileSize(fileSize),
        downloadState = if (downloadRepository.isFileDownloaded(FileUtils.buildLocalFileName(filename, fileUrl))) {
            DownloadState.COMPLETED
        } else {
            DownloadState.NOT_STARTED
        }
    )



    override fun onCleared() {
        super.onCleared()
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
    }
}
