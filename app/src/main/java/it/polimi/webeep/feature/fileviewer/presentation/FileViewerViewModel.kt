package it.polimi.webeep.feature.fileviewer.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.common.FileUtils
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.common.runCatchingCancellable
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.download.domain.DownloadFileUseCase
import it.polimi.webeep.feature.download.domain.DownloadRepository
import it.polimi.webeep.feature.download.domain.DownloadRepository.Companion.PROGRESS_COMPLETE
import it.polimi.webeep.feature.download.domain.DownloadRepository.Companion.PROGRESS_FAILED
import it.polimi.webeep.feature.download.domain.model.DownloadState
import it.polimi.webeep.feature.fileviewer.domain.FileContentRepository
import it.polimi.webeep.feature.fileviewer.domain.ViewerFile
import it.polimi.webeep.feature.fileviewer.domain.findSiblings
import it.polimi.webeep.navigation.FileViewer
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for file preview. Auth tokens are added only by OkHttp interceptors
 * (pluginfile): no URL in the UI state carries a token.
 *
 * When opened from a course, the files next to the requested one (same folder,
 * or single-file modules of the same section) can be swiped through; their
 * contents load lazily and only the current page and its neighbours stay loaded.
 */
@HiltViewModel
class FileViewerViewModel @Inject constructor(
    private val loader: FileContentRepository,
    private val courseRepository: CourseRepository,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val downloadRepository: DownloadRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<FileViewer>()

    private val initialFile = ViewerFile(
        url = MoodleFileUrlBuilder.stripAuthFromUrl(route.url),
        title = route.title,
        mimeType = route.mimeType.orEmpty()
    )
    private val courseId: Int? = route.courseId

    private val _uiState = MutableStateFlow(FileViewerUiState(initialFile = initialFile))
    val uiState: StateFlow<FileViewerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FileViewerEffect>(Channel.BUFFERED)
    val effects: Flow<FileViewerEffect> = _effects.receiveAsFlow()

    private val loadJobs = mutableMapOf<String, Job>()
    private var autoLaunchConsumed = false

    init {
        // Start the requested file right away; siblings resolve in parallel.
        onPageVisible(initialFile)
        viewModelScope.launch { resolveFiles() }
    }

    fun onAction(action: FileViewerAction) {
        when (action) {
            is FileViewerAction.PageVisible -> onPageVisible(action.file)
            is FileViewerAction.PageSettled -> onPageSettled(action.index)
            is FileViewerAction.Retry -> retry(action.file)
            is FileViewerAction.Download -> download(action.file)
            is FileViewerAction.Share -> withLocalFile(action.file) { local ->
                FileViewerEffect.Share(local, loader.resolveMimeType(action.file), action.file.title)
            }
            is FileViewerAction.OpenWith -> withLocalFile(action.file) { local ->
                FileViewerEffect.OpenExternally(local, loader.resolveMimeType(action.file))
            }
            is FileViewerAction.OpenExternally ->
                _effects.trySend(FileViewerEffect.OpenExternally(action.file, action.mimeType))
        }
    }

    private suspend fun resolveFiles() {
        val resolved = if (courseId == null) {
            listOf(initialFile) to 0
        } else {
            val siblings = runCatchingCancellable {
                findSiblings(courseRepository.getCachedCourseContents(courseId).first(), initialFile.url)
            }.getOrDefault(emptyList())
            val index = siblings.indexOfFirst { it.url == initialFile.url }
            if (index < 0) {
                listOf(initialFile) to 0
            } else {
                // Keep the title/mime the caller passed for the opened file.
                siblings.toMutableList().also { it[index] = initialFile.copy(size = it[index].size) } to index
            }
        }
        _uiState.update { it.copy(files = resolved.first, initialIndex = resolved.second) }
    }

    private fun onPageVisible(file: ViewerFile) {
        startLoading(file)
        if (file.url !in _uiState.value.downloadStates) {
            val initial = if (downloadRepository.isFileDownloaded(localName(file))) {
                DownloadState.COMPLETED
            } else {
                DownloadState.NOT_STARTED
            }
            setDownloadState(file, initial)
        }
    }

    private fun retry(file: ViewerFile) {
        evict(file.url)
        startLoading(file)
    }

    /**
     * Keeps [index] and its neighbours, drops the rest, and preloads small
     * neighbours so swiping lands on a ready page.
     */
    private fun onPageSettled(index: Int) {
        val list = _uiState.value.files ?: return
        val keep = ((index - 1)..(index + 1)).mapNotNull { list.getOrNull(it)?.url }.toSet()
        (loadJobs.keys - keep).forEach(::evict)
        listOfNotNull(list.getOrNull(index + 1), list.getOrNull(index - 1))
            .filter { it.size in 1..PRELOAD_MAX_BYTES }
            .forEach { startLoading(it) }
    }

    private fun download(file: ViewerFile) {
        if (_uiState.value.downloadStates[file.url] == DownloadState.DOWNLOADING) return
        viewModelScope.launch {
            setDownloadState(file, DownloadState.DOWNLOADING)
            val started = downloadFileUseCase(
                url = file.url,
                localFileName = localName(file),
                displayName = file.title,
                mimeType = file.mimeType.ifBlank { null }
            )
            when (started) {
                is Resource.Error -> setDownloadState(file, DownloadState.FAILED)
                is Resource.Success -> downloadRepository.getDownloadProgress(started.data).collect { progress ->
                    when (progress) {
                        PROGRESS_COMPLETE -> setDownloadState(file, DownloadState.COMPLETED)
                        PROGRESS_FAILED -> setDownloadState(file, DownloadState.FAILED)
                        else -> Unit
                    }
                }
            }
        }
    }

    /** Prepares a local copy of [file] for share / open-with and emits the resulting [effect]. */
    private fun withLocalFile(file: ViewerFile, effect: (File) -> FileViewerEffect) {
        viewModelScope.launch {
            localFileFor(file)?.let { _effects.send(effect(it)) }
        }
    }

    /** Local copy for share / open-with, reusing what the page already downloaded. */
    private suspend fun localFileFor(file: ViewerFile): File? {
        _uiState.value.contents[file.url]?.localFile?.let { return it }
        _uiState.update { it.copy(preparingShare = true) }
        return try {
            runCatchingCancellable { loader.localCopy(file) }.getOrNull()
        } finally {
            _uiState.update { it.copy(preparingShare = false) }
        }
    }

    private fun startLoading(file: ViewerFile) {
        if (file.url in loadJobs) return
        setContent(file, FileContentState.Downloading(0L, null))
        loadJobs[file.url] = viewModelScope.launch {
            loader.load(file).collect { onContent(file, it.toContentState()) }
        }
    }

    private fun onContent(file: ViewerFile, content: FileContentState) {
        setContent(file, content)
        // Only the file the viewer was opened on opens by itself, and only once.
        if (content is FileContentState.External && file.url == initialFile.url && !autoLaunchConsumed) {
            autoLaunchConsumed = true
            _effects.trySend(FileViewerEffect.OpenExternally(content.file, content.mimeType))
        }
    }

    private fun evict(url: String) {
        loadJobs.remove(url)?.cancel()
        _uiState.update { it.copy(contents = it.contents - url) }
    }

    private fun setContent(file: ViewerFile, content: FileContentState) {
        _uiState.update { it.copy(contents = it.contents + (file.url to content)) }
    }

    private fun setDownloadState(file: ViewerFile, state: DownloadState) {
        _uiState.update { it.copy(downloadStates = it.downloadStates + (file.url to state)) }
    }

    private fun localName(file: ViewerFile) = FileUtils.buildLocalFileName(file.title, file.url)

    private companion object {
        const val PRELOAD_MAX_BYTES = 5L * 1024L * 1024L
    }
}
