package it.polimi.webeep.feature.course.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.di.DefaultDispatcher
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.core.util.FileUtils
import it.polimi.webeep.core.util.MoodleText
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.core.util.toMessageRes
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.course.domain.GetCourseContentsUseCase
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.download.domain.DownloadFileUseCase
import it.polimi.webeep.feature.download.domain.DownloadRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CourseDetailUiState {
    data object Loading : CourseDetailUiState
    data class Success(val courseName: String, val sections: List<SectionUiModel>) : CourseDetailUiState
    data class Error(@StringRes val messageRes: Int) : CourseDetailUiState
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

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val getCourseContentsUseCase: GetCourseContentsUseCase,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val downloadRepository: DownloadRepository,
    private val courseRepository: CourseRepository,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    val courseId: Int = checkNotNull(savedStateHandle["courseId"]) {
        "course id not passed to navigator"
    }

    private val _uiState = MutableStateFlow<CourseDetailUiState>(CourseDetailUiState.Loading)
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    /**
     * Open folders, keyed by [folderRootKey] / [folderKey]. Lives here (and in the
     * saved state) so folders stay open when coming back from the file viewer.
     */
    val expandedFolders: StateFlow<Set<String>> = savedStateHandle
        .getStateFlow(EXPANDED_FOLDERS_KEY, arrayListOf<String>())
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /**
     * Current search. The text field keeps its own state (typing stays synchronous)
     * and mirrors every change here; saved state restores both after process death.
     */
    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(SEARCH_QUERY_KEY, "")

    /**
     * The flattened rows to show, rebuilt off the main thread whenever contents,
     * open folders or the search change. Null until built for the first content.
     */
    val listContent: StateFlow<CourseListContent?> = combine(
        _uiState,
        expandedFolders,
        searchQuery
    ) { state, folders, query ->
        (state as? CourseDetailUiState.Success)?.let { buildCourseListItems(it.sections, folders, query) }
    }
        .flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private var courseName: String = ""
    private var contentsJob: Job? = null

    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val downloadIds = ConcurrentHashMap<String, Long>()

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
        contentsJob?.cancel()
        contentsJob = viewModelScope.launch {
            if (_uiState.value !is CourseDetailUiState.Success) {
                _uiState.update { CourseDetailUiState.Loading }
            }
            getCourseContentsUseCase(courseId).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (_uiState.value !is CourseDetailUiState.Success) {
                            _uiState.update { CourseDetailUiState.Loading }
                        }
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
                        _uiState.update { current ->
                            CourseDetailUiState.Success(
                                courseName = courseName,
                                sections = uiSections.withInFlightDownloadStates(current)
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { CourseDetailUiState.Error(resource.error.toMessageRes()) }
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

    fun setSearchQuery(query: String) {
        savedStateHandle[SEARCH_QUERY_KEY] = query
    }

    fun toggleFolder(key: String) {
        val current = savedStateHandle.get<ArrayList<String>>(EXPANDED_FOLDERS_KEY).orEmpty()
        val updated = if (key in current) current - key else current + key
        savedStateHandle[EXPANDED_FOLDERS_KEY] = ArrayList(updated)
    }

    fun downloadFile(file: FileUiModel) {
        downloadJobs[file.fileUrl]?.cancel()
        downloadIds.remove(file.fileUrl)?.let { downloadRepository.cancelDownload(it) }

        val job = viewModelScope.launch {
            updateFileDownloadState(file.fileUrl, DownloadState.DOWNLOADING)
            try {
                val downloadId = downloadFileUseCase.execute(
                    url = file.fileUrl,
                    localFileName = file.localFileName,
                    displayName = file.filename,
                    mimeType = file.mimeType
                )
                downloadIds[file.fileUrl] = downloadId

                downloadRepository.getDownloadProgress(downloadId).collect { progress ->
                    when (progress) {
                        100 -> {
                            updateFileDownloadState(file.fileUrl, DownloadState.COMPLETED)
                            downloadJobs.remove(file.fileUrl)
                            downloadIds.remove(file.fileUrl)
                        }
                        -1 -> {
                            updateFileDownloadState(file.fileUrl, DownloadState.FAILED)
                            downloadJobs.remove(file.fileUrl)
                            downloadIds.remove(file.fileUrl)
                        }
                        else -> updateFileDownloadState(file.fileUrl, DownloadState.DOWNLOADING)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                updateFileDownloadState(file.fileUrl, DownloadState.FAILED)
                downloadJobs.remove(file.fileUrl)
                downloadIds.remove(file.fileUrl)
            }
        }
        downloadJobs[file.fileUrl] = job
    }

    fun openFile(file: FileUiModel) {
        downloadRepository.openFile(file.localFileName)
    }

    private fun updateFileDownloadState(fileUrl: String, newState: DownloadState) {
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

    /**
     * A cache re-emit rebuilds every [FileUiModel] from disk state only: carry over
     * DOWNLOADING (the job is still running) and FAILED (unless the file is now on disk).
     */
    private fun List<SectionUiModel>.withInFlightDownloadStates(previous: CourseDetailUiState): List<SectionUiModel> {
        if (previous !is CourseDetailUiState.Success) return this
        val carriedStates = previous.sections.asSequence()
            .flatMap { it.modules.asSequence() }
            .flatMap { it.files.asSequence() }
            .filter {
                it.downloadState == DownloadState.DOWNLOADING || it.downloadState == DownloadState.FAILED
            }
            .associate { it.fileUrl to it.downloadState }
        if (carriedStates.isEmpty()) return this
        return map { section ->
            section.copy(
                modules = section.modules.map { module ->
                    module.copy(
                        files = module.files.map { file ->
                            when (carriedStates[file.fileUrl]) {
                                DownloadState.DOWNLOADING -> file.copy(downloadState = DownloadState.DOWNLOADING)
                                DownloadState.FAILED ->
                                    if (file.downloadState == DownloadState.COMPLETED) {
                                        file
                                    } else {
                                        file.copy(downloadState = DownloadState.FAILED)
                                    }
                                else -> file
                            }
                        }
                    )
                }
            )
        }
    }

    // HTML is cleaned here, off the main thread, never while rows compose.
    private fun Section.toUiModel(isExpanded: Boolean) = SectionUiModel(
        id = id,
        name = name,
        summary = MoodleText.cleanHtml(summary).takeIf { it.isNotBlank() },
        modules = modules.map { it.toUiModel() },
        isExpanded = isExpanded
    )

    private fun Module.toUiModel() = ModuleUiModel(
        id = id,
        name = name,
        type = modName,
        iconUrl = iconUrl,
        description = MoodleText.cleanHtml(description).takeIf { it.isNotBlank() },
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

    private companion object {
        const val EXPANDED_FOLDERS_KEY = "expandedFolders"
        const val SEARCH_QUERY_KEY = "searchQuery"
        const val STOP_TIMEOUT_MS = 5_000L
    }

    override fun onCleared() {
        super.onCleared()
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        downloadIds.values.forEach { downloadRepository.cancelDownload(it) }
        downloadIds.clear()
    }
}
