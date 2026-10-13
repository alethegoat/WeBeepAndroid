package it.polimi.webeep.feature.course.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.common.CourseTitleParser
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.di.DefaultDispatcher
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.core.storage.mimeTypeForFileName
import it.polimi.webeep.core.ui.util.STOP_TIMEOUT_MILLIS
import it.polimi.webeep.core.ui.util.toMessageRes
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.course.domain.GetCourseContentsUseCase
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.download.domain.DownloadRepository
import it.polimi.webeep.feature.download.domain.DownloadTracker
import it.polimi.webeep.feature.download.domain.model.DownloadState
import it.polimi.webeep.navigation.CourseDetail
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EXPANDED_FOLDERS_KEY = "expandedFolders"
private const val SEARCH_QUERY_KEY = "searchQuery"

/** What was last loaded, before it is filtered and flattened into [CourseDetailUiState]. */
private sealed interface Loaded {
    data object Loading : Loaded
    data class Sections(val sections: List<SectionUiModel>) : Loaded
    data class Error(val messageRes: Int) : Loaded
}

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val getCourseContentsUseCase: GetCourseContentsUseCase,
    private val downloadTracker: DownloadTracker,
    private val downloadRepository: DownloadRepository,
    private val courseRepository: CourseRepository,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    val courseId: Int = savedStateHandle.toRoute<CourseDetail>().courseId

    /**
     * The search field's value. Compose state, not part of the flow round-trip, so typing stays
     * synchronous (no dropped characters or cursor jumps); [uiState] mirrors it for filtering.
     */
    var searchQuery by mutableStateOf(savedStateHandle[SEARCH_QUERY_KEY] ?: "")
        private set

    private val loaded = MutableStateFlow<Loaded>(Loaded.Loading)
    private val courseName = MutableStateFlow("")

    /**
     * Open folders, keyed by [folderRootKey] / [folderKey]. Kept in the saved state so folders
     * stay open when coming back from the file viewer.
     */
    private val expandedFolders: Flow<Set<String>> = savedStateHandle
        .getStateFlow(EXPANDED_FOLDERS_KEY, arrayListOf<String>())
        .map { it.toSet() }

    /** Rows are rebuilt off the main thread whenever contents, open folders or the search change. */
    val uiState: StateFlow<CourseDetailUiState> = combine(
        loaded,
        courseName,
        expandedFolders,
        savedStateHandle.getStateFlow(SEARCH_QUERY_KEY, searchQuery)
    ) { loaded, name, folders, query ->
        CourseDetailUiState(
            courseId = courseId,
            courseTitle = name.takeIf { it.isNotBlank() }?.let(CourseTitleParser::parse),
            content = loaded.toContent(folders, query),
            searchQuery = query,
            expandedFolders = folders
        )
    }
        .flowOn(defaultDispatcher)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            CourseDetailUiState(courseId = courseId, searchQuery = searchQuery)
        )

    private val _effects = Channel<CourseDetailEffect>(Channel.BUFFERED)
    val effects: Flow<CourseDetailEffect> = _effects.receiveAsFlow()

    private var contentsJob: Job? = null

    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val downloadIds = ConcurrentHashMap<String, Long>()

    init {
        loadCourseName()
        loadContents()
    }

    fun onAction(action: CourseDetailAction) {
        when (action) {
            CourseDetailAction.Retry -> loadContents()
            is CourseDetailAction.ToggleSection -> toggleSection(action.sectionId)
            is CourseDetailAction.ToggleFolder -> toggleFolder(action.key)
            is CourseDetailAction.SearchQueryChange -> updateSearchQuery(action.query)
            is CourseDetailAction.PreviewFile -> previewFile(action.file)
            is CourseDetailAction.DownloadFile -> downloadFile(action.file)
            is CourseDetailAction.OpenFile -> openFile(action.file)
        }
    }

    private fun loadCourseName() {
        viewModelScope.launch {
            courseRepository.getCourseName(courseId)?.let { courseName.value = it }
        }
    }

    private fun loadContents() {
        contentsJob?.cancel()
        contentsJob = viewModelScope.launch {
            if (loaded.value !is Loaded.Sections) {
                loaded.value = Loaded.Loading
            }
            getCourseContentsUseCase(courseId).collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> showSections(resource.data)
                    is Resource.Error -> loaded.value = Loaded.Error(resource.error.toMessageRes())
                }
            }
        }
    }

    private suspend fun showSections(sections: List<Section>) {
        val uiSections = withContext(ioDispatcher) {
            val previous = (loaded.value as? Loaded.Sections)?.sections.orEmpty()
            sections.toUiModels(previous.expansionById(), downloadRepository::isFileDownloaded)
        }
        loaded.update { current ->
            Loaded.Sections(uiSections.withInFlightDownloadStates((current as? Loaded.Sections)?.sections.orEmpty()))
        }
    }

    private fun Loaded.toContent(folders: Set<String>, query: String): CourseDetailContent = when (this) {
        Loaded.Loading -> CourseDetailContent.Loading
        is Loaded.Error -> CourseDetailContent.Error(messageRes)
        is Loaded.Sections ->
            if (sections.isEmpty()) {
                CourseDetailContent.Empty
            } else {
                CourseDetailContent.Content(buildCourseListItems(sections, folders, query))
            }
    }

    private fun toggleSection(sectionId: Int) {
        updateSections { sections ->
            sections.map { if (it.id == sectionId) it.copy(isExpanded = !it.isExpanded) else it }
        }
    }

    private fun updateSearchQuery(query: String) {
        searchQuery = query
        savedStateHandle[SEARCH_QUERY_KEY] = query
    }

    private fun toggleFolder(key: String) {
        val current = savedStateHandle.get<ArrayList<String>>(EXPANDED_FOLDERS_KEY).orEmpty()
        val updated = if (key in current) current - key else current + key
        savedStateHandle[EXPANDED_FOLDERS_KEY] = ArrayList(updated)
    }

    private fun previewFile(file: FileUiModel) {
        _effects.trySend(
            when (val target = previewTargetFor(file, courseId)) {
                is FilePreviewTarget.ExternalLink -> CourseDetailEffect.OpenLink(target.url)
                is FilePreviewTarget.Viewer -> CourseDetailEffect.OpenViewer(target.route)
            }
        )
    }

    /** The ViewModel only locates the finished copy; starting the chooser is the route's job. */
    private fun openFile(file: FileUiModel) {
        downloadRepository.downloadedFile(file.localFileName)?.let { local ->
            _effects.trySend(CourseDetailEffect.OpenFile(local, mimeTypeForFileName(local.name)))
        }
    }

    private fun downloadFile(file: FileUiModel) {
        val url = file.fileUrl
        downloadJobs[url]?.cancel()
        downloadIds.remove(url)?.let { downloadRepository.cancelDownload(it) }

        downloadJobs[url] = viewModelScope.launch {
            downloadTracker.track(
                url = url,
                localFileName = file.localFileName,
                displayName = file.filename,
                mimeType = file.mimeType,
                onStarted = { downloadIds[url] = it }
            ).collect { state ->
                setFileDownloadState(url, state)
                if (state != DownloadState.DOWNLOADING) {
                    downloadJobs.remove(url)
                    downloadIds.remove(url)
                }
            }
        }
    }

    private fun setFileDownloadState(fileUrl: String, newState: DownloadState) {
        updateSections { sections ->
            sections.mapFiles { if (it.fileUrl == fileUrl) it.copy(downloadState = newState) else it }
        }
    }

    private fun updateSections(transform: (List<SectionUiModel>) -> List<SectionUiModel>) {
        loaded.update { current ->
            if (current is Loaded.Sections) Loaded.Sections(transform(current.sections)) else current
        }
    }

    override fun onCleared() {
        super.onCleared()
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        downloadIds.values.forEach { downloadRepository.cancelDownload(it) }
        downloadIds.clear()
    }
}
