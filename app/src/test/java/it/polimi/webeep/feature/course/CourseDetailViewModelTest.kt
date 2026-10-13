package it.polimi.webeep.feature.course

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import it.polimi.webeep.R
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.ui.components.GroupPosition
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.course.domain.GetCourseContentsUseCase
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.course.presentation.CourseDetailAction
import it.polimi.webeep.feature.course.presentation.CourseDetailContent
import it.polimi.webeep.feature.course.presentation.CourseDetailEffect
import it.polimi.webeep.feature.course.presentation.CourseDetailUiState
import it.polimi.webeep.feature.course.presentation.CourseDetailViewModel
import it.polimi.webeep.feature.course.presentation.CourseListContent
import it.polimi.webeep.feature.course.presentation.CourseListItem
import it.polimi.webeep.feature.course.presentation.FileUiModel
import it.polimi.webeep.feature.course.presentation.ModuleKind
import it.polimi.webeep.feature.download.domain.DownloadFileUseCase
import it.polimi.webeep.feature.download.domain.DownloadRepository
import it.polimi.webeep.feature.download.domain.DownloadTracker
import it.polimi.webeep.feature.download.domain.model.DownloadState
import it.polimi.webeep.navigation.FileViewer
import it.polimi.webeep.util.MainDispatcherRule
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
// SavedStateHandle.toRoute builds a Bundle, which needs Robolectric.
@RunWith(RobolectricTestRunner::class)
class CourseDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    lateinit var getCourseContentsUseCase: GetCourseContentsUseCase

    @MockK
    lateinit var downloadFileUseCase: DownloadFileUseCase

    @MockK
    lateinit var downloadRepository: DownloadRepository

    @MockK
    lateinit var courseRepository: CourseRepository

    private lateinit var viewModel: CourseDetailViewModel
    private lateinit var savedStateHandle: SavedStateHandle

    private val sampleSections = listOf(
        Section(
            id = 10,
            name = "General",
            summary = "Summary",
            sectionNumber = 0,
            modules = listOf(
                Module(
                    id = 101,
                    name = "Syllabus",
                    modName = "resource",
                    iconUrl = "icon.png",
                    description = "Course Details",
                    url = "https://webeep.polimi.it",
                    files = listOf(
                        FileResource(
                            filename = "syllabus.pdf",
                            filepath = "/",
                            fileUrl = "https://webeep.polimi.it/webservice/pluginfile.php/123/syllabus.pdf",
                            fileSize = 204800L, // 200 KB
                            mimeType = "application/pdf",
                            timeModified = 1000L
                        )
                    )
                )
            )
        )
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        savedStateHandle = SavedStateHandle(mapOf("courseId" to 5))

        coEvery { courseRepository.getCourseName(5) } returns "Informatica 1"
        coEvery { getCourseContentsUseCase(5) } returns flowOf(Resource.Success(sampleSections))
        every { downloadRepository.isFileDownloaded(any()) } returns false
    }

    private fun initViewModel() {
        viewModel = CourseDetailViewModel(
            getCourseContentsUseCase,
            DownloadTracker(downloadFileUseCase, downloadRepository),
            downloadRepository,
            courseRepository,
            savedStateHandle,
            kotlinx.coroutines.test.UnconfinedTestDispatcher(),
            kotlinx.coroutines.test.UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `loadContents emits Loading then Content with the course title`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            val state = awaitState { it.content is CourseDetailContent.Content }
            assertEquals(5, state.courseId)
            assertEquals("Informatica 1", state.courseTitle?.name)
            val list = (state.content as CourseDetailContent.Content).list
            assertEquals("General", (list.items.first() as CourseListItem.SectionHeader).name)
        }
    }

    @Test
    fun `initial state is Loading`() = runTest {
        coEvery { getCourseContentsUseCase(5) } returns MutableSharedFlow()

        initViewModel()

        assertEquals(CourseDetailContent.Loading, viewModel.uiState.value.content)
    }

    @Test
    fun `a course without sections is Empty`() = runTest {
        coEvery { getCourseContentsUseCase(5) } returns flowOf(Resource.Success(emptyList()))

        initViewModel()

        viewModel.uiState.test {
            assertEquals(CourseDetailContent.Empty, awaitState { it.content == CourseDetailContent.Empty }.content)
        }
    }

    @Test
    fun `loadContents emits Error on failure and Retry loads again`() = runTest {
        coEvery { getCourseContentsUseCase(5) } returns flowOf(Resource.Error(DataError.Network))

        initViewModel()

        viewModel.uiState.test {
            val state = awaitState { it.content is CourseDetailContent.Error }
            assertEquals(R.string.error_data_network, (state.content as CourseDetailContent.Error).messageRes)

            coEvery { getCourseContentsUseCase(5) } returns flowOf(Resource.Success(sampleSections))
            viewModel.onAction(CourseDetailAction.Retry)
            awaitState { it.content is CourseDetailContent.Content }
        }
    }

    @Test
    fun `toggleSection changes section expanded state`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            val first = awaitState { it.content is CourseDetailContent.Content }
            assertTrue((first.header().isExpanded)) // First section is expanded by default

            viewModel.onAction(CourseDetailAction.ToggleSection(10))

            val second = awaitState { (it.content as? CourseDetailContent.Content)?.header()?.isExpanded == false }
            assertFalse(second.header().isExpanded)
        }
    }

    @Test
    fun `toggleFolder opens and closes a folder and survives in saved state`() = runTest {
        initViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onAction(CourseDetailAction.ToggleFolder("101:"))
        assertEquals(setOf("101:"), viewModel.uiState.value.expandedFolders)
        assertEquals(arrayListOf("101:"), savedStateHandle.get<ArrayList<String>>("expandedFolders"))

        viewModel.onAction(CourseDetailAction.ToggleFolder("101:"))
        assertTrue(viewModel.uiState.value.expandedFolders.isEmpty())
    }

    @Test
    fun `the list flattens the expanded first section into rows`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            val list = (awaitState { it.content is CourseDetailContent.Content }.content as CourseDetailContent.Content).list
            val header = list.items.first() as CourseListItem.SectionHeader
            assertEquals("General", header.name)
            assertEquals("Summary", header.summary)
            val module = list.items[1] as CourseListItem.Module
            assertEquals(ModuleKind.SINGLE_FILE, module.kind)
            assertEquals(GroupPosition.BOTTOM, module.position)
        }
    }

    @Test
    fun `search query filters the rows and survives in saved state`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            awaitState { it.content is CourseDetailContent.Content }

            viewModel.onAction(CourseDetailAction.SearchQueryChange("nothing matches this"))
            assertEquals("nothing matches this", viewModel.searchQuery)
            assertEquals("nothing matches this", savedStateHandle.get<String>("searchQuery"))
            val filtered = awaitState { (it.content as? CourseDetailContent.Content)?.list?.noMatches == true }
            assertEquals("nothing matches this", filtered.searchQuery)
            assertTrue((filtered.content as CourseDetailContent.Content).list.items.isEmpty())
        }
    }

    @Test
    fun `module description HTML is cleaned before reaching the UI`() = runTest {
        coEvery { getCourseContentsUseCase(5) } returns flowOf(
            Resource.Success(
                sampleSections.map { section ->
                    section.copy(
                        summary = "<p></p>",
                        modules = section.modules.map { it.copy(description = "<b>Read</b> &amp; sign") }
                    )
                }
            )
        )

        initViewModel()

        viewModel.uiState.test {
            val state = awaitState { it.content is CourseDetailContent.Content }
            assertEquals(null, state.header().summary)
            val module = (state.content as CourseDetailContent.Content).list.items[1] as CourseListItem.Module
            assertEquals("Read & sign", module.module.description)
        }
    }

    @Test
    fun `file size formatting is correct`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            awaitState { it.content is CourseDetailContent.Content }
            assertEquals("200.0 KB", firstFile().formattedSize)
        }
    }

    @Test
    fun `downloadFile triggers download and monitors progress successfully`() = runTest {
        val fileUrl = "https://webeep.polimi.it/webservice/pluginfile.php/123/syllabus.pdf"
        coEvery {
            downloadFileUseCase(fileUrl, any(), "syllabus.pdf", "application/pdf")
        } returns Resource.Success(999L)
        every { downloadRepository.getDownloadProgress(999L) } returns flowOf(50, 100)

        initViewModel()

        viewModel.uiState.test {
            awaitState { it.content is CourseDetailContent.Content }

            viewModel.onAction(CourseDetailAction.DownloadFile(firstFile()))

            // Skip intermediate DOWNLOADING state and wait for COMPLETED
            awaitState { it.firstFileOrNull()?.downloadState == DownloadState.COMPLETED }
        }

        coVerify(exactly = 1) {
            downloadFileUseCase(
                fileUrl,
                match { it.startsWith("syllabus-") && it.endsWith(".pdf") },
                "syllabus.pdf",
                "application/pdf"
            )
        }
    }

    @Test
    fun `downloadFile transitions to FAILED state on download execution error`() = runTest {
        val fileUrl = "https://webeep.polimi.it/webservice/pluginfile.php/123/syllabus.pdf"
        coEvery {
            downloadFileUseCase(fileUrl, any(), "syllabus.pdf", "application/pdf")
        } returns Resource.Error(DataError.Unauthorized)

        initViewModel()

        viewModel.uiState.test {
            awaitState { it.content is CourseDetailContent.Content }

            viewModel.onAction(CourseDetailAction.DownloadFile(firstFile()))

            awaitState { it.firstFileOrNull()?.downloadState == DownloadState.FAILED }
        }
    }

    @Test
    fun `OpenFile emits the downloaded copy with its mime type`() = runTest {
        val local = File("syllabus.pdf")
        every { downloadRepository.downloadedFile(any()) } returns local
        initViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onAction(CourseDetailAction.OpenFile(firstFile()))

            assertEquals(CourseDetailEffect.OpenFile(local, "application/pdf"), awaitItem())
        }
        verify(exactly = 1) {
            downloadRepository.downloadedFile(match { it.startsWith("syllabus-") && it.endsWith(".pdf") })
        }
    }

    @Test
    fun `OpenFile emits nothing when the local copy is gone`() = runTest {
        every { downloadRepository.downloadedFile(any()) } returns null
        initViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onAction(CourseDetailAction.OpenFile(firstFile()))

            expectNoEvents()
        }
    }

    @Test
    fun `PreviewFile on a pluginfile emits OpenViewer with a token-free route`() = runTest {
        initViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val file = firstFile().copy(fileUrl = firstFile().fileUrl + "?token=secret")

        viewModel.effects.test {
            viewModel.onAction(CourseDetailAction.PreviewFile(file))

            val effect = awaitItem() as CourseDetailEffect.OpenViewer
            assertEquals(
                FileViewer(url = firstFile().fileUrl, title = "syllabus.pdf", mimeType = "application/pdf", courseId = 5),
                effect.route
            )
        }
    }

    @Test
    fun `PreviewFile on an external web link emits OpenLink`() = runTest {
        initViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val link = firstFile().copy(fileUrl = "https://example.com/slides?token=secret", mimeType = "text/html")

        viewModel.effects.test {
            viewModel.onAction(CourseDetailAction.PreviewFile(link))

            assertEquals(CourseDetailEffect.OpenLink("https://example.com/slides"), awaitItem())
        }
    }

    @Test
    fun `cache re-emit keeps an in-flight download in DOWNLOADING`() = runTest {
        val fileUrl = "https://webeep.polimi.it/webservice/pluginfile.php/123/syllabus.pdf"
        val cache = MutableStateFlow(sampleSections)
        coEvery { getCourseContentsUseCase(5) } returns cache.map { Resource.Success(it) }
        coEvery { downloadFileUseCase(fileUrl, any(), any(), any()) } returns Resource.Success(7L)
        every { downloadRepository.getDownloadProgress(7L) } returns MutableSharedFlow<Int>()

        initViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onAction(CourseDetailAction.DownloadFile(firstFile()))
        advanceUntilIdle()
        assertEquals(DownloadState.DOWNLOADING, firstFile().downloadState)

        // Background sync rewrites the cache with a fresh copy of the same tree.
        cache.value = sampleSections.map { it.copy(name = "General (synced)") }
        advanceUntilIdle()

        assertEquals("General (synced)", viewModel.uiState.value.header().name)
        assertEquals(DownloadState.DOWNLOADING, firstFile().downloadState)
    }

    @Test
    fun `cache re-emit keeps FAILED unless the file is now on disk`() = runTest {
        val fileUrl = "https://webeep.polimi.it/webservice/pluginfile.php/123/syllabus.pdf"
        val cache = MutableStateFlow(sampleSections)
        coEvery { getCourseContentsUseCase(5) } returns cache.map { Resource.Success(it) }
        coEvery { downloadFileUseCase(fileUrl, any(), any(), any()) } returns Resource.Error(DataError.Unauthorized)

        initViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onAction(CourseDetailAction.DownloadFile(firstFile()))
        advanceUntilIdle()
        assertEquals(DownloadState.FAILED, firstFile().downloadState)

        cache.value = sampleSections.map { it.copy(summary = "v2") }
        advanceUntilIdle()
        assertEquals(DownloadState.FAILED, firstFile().downloadState)

        every { downloadRepository.isFileDownloaded(any()) } returns true
        cache.value = sampleSections.map { it.copy(summary = "v3") }
        advanceUntilIdle()
        assertEquals(DownloadState.COMPLETED, firstFile().downloadState)
    }

    @Test
    fun `an unexpected repository exception ends in FAILED without crashing`() = runTest {
        val tokenProvider = io.mockk.mockk<it.polimi.webeep.core.datastore.TokenProvider>()
        coEvery { tokenProvider.getToken() } returns "token"
        every { downloadRepository.downloadFile(any(), any(), any(), any(), any()) } throws RuntimeException("boom")
        downloadFileUseCase = DownloadFileUseCase(downloadRepository, tokenProvider)

        initViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onAction(CourseDetailAction.DownloadFile(firstFile()))
        advanceUntilIdle()

        assertEquals(DownloadState.FAILED, firstFile().downloadState)
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<CourseDetailUiState>.awaitState(
        predicate: (CourseDetailUiState) -> Boolean
    ): CourseDetailUiState {
        var state = awaitItem()
        while (!predicate(state)) state = awaitItem()
        return state
    }

    private fun CourseDetailUiState.list(): CourseListContent = (content as CourseDetailContent.Content).list

    private fun CourseDetailUiState.header() = list().items.first() as CourseListItem.SectionHeader

    private fun CourseDetailContent.header() = (this as CourseDetailContent.Content).list.items.first() as CourseListItem.SectionHeader

    private fun CourseDetailUiState.firstFileOrNull(): FileUiModel? = (content as? CourseDetailContent.Content)
        ?.list?.items?.filterIsInstance<CourseListItem.Module>()?.firstOrNull()?.module?.files?.firstOrNull()

    private fun firstFile(): FileUiModel = viewModel.uiState.value.firstFileOrNull()!!
}
