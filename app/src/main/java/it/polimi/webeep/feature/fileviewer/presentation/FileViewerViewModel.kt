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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient

/** The files the pager can show and where it starts. */
data class ViewerFiles(val files: List<ViewerFile>, val initialIndex: Int)

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
    private val okHttpClient: OkHttpClient,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<FileViewer>()

    val initialFile = ViewerFile(
        url = MoodleFileUrlBuilder.stripAuthFromUrl(route.url),
        title = route.title,
        mimeType = route.mimeType.orEmpty()
    )
    private val courseId: Int? = route.courseId

    private val _files = MutableStateFlow<ViewerFiles?>(null)
    val files: StateFlow<ViewerFiles?> = _files.asStateFlow()

    private val contents = mutableMapOf<String, MutableStateFlow<FileContentState>>()
    private val loadJobs = mutableMapOf<String, Job>()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val _preparingShare = MutableStateFlow(false)
    val preparingShare: StateFlow<Boolean> = _preparingShare.asStateFlow()

    private var autoLaunchConsumed = false

    /** Shared authenticated client for streaming media (ExoPlayer). */
    val mediaCallFactory: Call.Factory
        get() = okHttpClient

    init {
        // Start the requested file right away; siblings resolve in parallel.
        startLoading(initialFile)
        viewModelScope.launch { _files.value = resolveFiles() }
    }

    private suspend fun resolveFiles(): ViewerFiles {
        if (courseId == null) return ViewerFiles(listOf(initialFile), 0)
        val siblings = runCatchingCancellable {
            findSiblings(courseRepository.getCachedCourseContents(courseId).first(), initialFile.url)
        }.getOrDefault(emptyList())
        val index = siblings.indexOfFirst { it.url == initialFile.url }
        return if (index < 0) {
            ViewerFiles(listOf(initialFile), 0)
        } else {
            // Keep the title/mime the caller passed for the opened file.
            ViewerFiles(siblings.toMutableList().also { it[index] = initialFile.copy(size = it[index].size) }, index)
        }
    }

    /** Content of [file], loading it on first request. */
    fun contentFor(file: ViewerFile): StateFlow<FileContentState> = startLoading(file)

    fun retry(file: ViewerFile) {
        loadJobs.remove(file.url)?.cancel()
        contents.remove(file.url)
        startLoading(file)
    }

    /**
     * Keeps [index] and its neighbours, drops the rest, and preloads small
     * neighbours so swiping lands on a ready page.
     */
    fun onPageSettled(index: Int) {
        val list = _files.value?.files ?: return
        val keep = ((index - 1)..(index + 1)).mapNotNull { list.getOrNull(it)?.url }.toSet()
        (contents.keys - keep).forEach { url ->
            loadJobs.remove(url)?.cancel()
            contents.remove(url)
        }
        listOfNotNull(list.getOrNull(index + 1), list.getOrNull(index - 1))
            .filter { it.size in 1..PRELOAD_MAX_BYTES }
            .forEach { startLoading(it) }
    }

    /** True once, for the file the viewer was opened on: external files auto-open only then. */
    fun consumeAutoLaunch(file: ViewerFile): Boolean {
        if (autoLaunchConsumed || file.url != initialFile.url) return false
        autoLaunchConsumed = true
        return true
    }

    fun downloadStateFor(file: ViewerFile): DownloadState =
        _downloadStates.value[file.url] ?: if (downloadRepository.isFileDownloaded(localName(file))) {
            DownloadState.COMPLETED
        } else {
            DownloadState.NOT_STARTED
        }

    fun download(file: ViewerFile) {
        if (_downloadStates.value[file.url] == DownloadState.DOWNLOADING) return
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

    /** Local copy for share / open-with, reusing what the page already downloaded. */
    suspend fun localFileFor(file: ViewerFile): File? {
        contents[file.url]?.value?.localFile?.let { return it }
        _preparingShare.value = true
        return try {
            runCatchingCancellable { loader.localCopy(file) }.getOrNull()
        } finally {
            _preparingShare.value = false
        }
    }

    fun shareMimeType(file: ViewerFile): String = loader.resolveMimeType(file)

    private fun startLoading(file: ViewerFile): StateFlow<FileContentState> {
        contents[file.url]?.let { return it }
        val state = MutableStateFlow<FileContentState>(FileContentState.Downloading(0L, null))
        contents[file.url] = state
        loadJobs[file.url] = viewModelScope.launch {
            loader.load(file).collect { state.value = it.toContentState() }
        }
        return state
    }

    private fun setDownloadState(file: ViewerFile, state: DownloadState) {
        _downloadStates.update { it + (file.url to state) }
    }

    private fun localName(file: ViewerFile) = FileUtils.buildLocalFileName(file.title, file.url)

    private companion object {
        const val PRELOAD_MAX_BYTES = 5L * 1024L * 1024L
    }
}
