package it.polimi.webeep.feature.course

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.polimi.webeep.R
import it.polimi.webeep.core.util.DataError
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.course.domain.GetCourseContentsUseCase
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.core.ui.components.GroupPosition
import it.polimi.webeep.feature.course.presentation.CourseDetailUiState
import it.polimi.webeep.feature.course.presentation.CourseListItem
import it.polimi.webeep.feature.course.presentation.ModuleKind
import it.polimi.webeep.feature.course.presentation.CourseDetailViewModel
import it.polimi.webeep.feature.course.presentation.DownloadState
import it.polimi.webeep.feature.dashboard.MainDispatcherRule
import it.polimi.webeep.feature.download.domain.DownloadFileUseCase
import it.polimi.webeep.feature.download.domain.DownloadRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
        every { downloadRepository.openFile(any()) } returns true
    }

    private fun initViewModel() {
        viewModel = CourseDetailViewModel(
            getCourseContentsUseCase,
            downloadFileUseCase,
            downloadRepository,
            courseRepository,
            savedStateHandle,
            kotlinx.coroutines.test.UnconfinedTestDispatcher(),
            kotlinx.coroutines.test.UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `loadContents emits Loading then Success`() = runTest {
        coEvery { getCourseContentsUseCase(5) } returns flowOf(Resource.Loading, Resource.Success(sampleSections))

        initViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state !is CourseDetailUiState.Success) {
                state = awaitItem()
            }
            assertTrue(state is CourseDetailUiState.Success)
            val success = state as CourseDetailUiState.Success
            assertEquals("Informatica 1", success.courseName)
            assertEquals(1, success.sections.size)
            assertEquals("General", success.sections.first().name)
        }
    }

    @Test
    fun `loadContents emits Error on failure`() = runTest {
        coEvery { getCourseContentsUseCase(5) } returns flowOf(Resource.Error(DataError.Network))

        initViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state !is CourseDetailUiState.Error) {
                state = awaitItem()
            }
            assertTrue(state is CourseDetailUiState.Error)
            assertEquals(R.string.error_data_network, (state as CourseDetailUiState.Error).messageRes)
        }
    }

    @Test
    fun `toggleSection changes section expanded state`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            var state1 = awaitItem()
            while (state1 !is CourseDetailUiState.Success) {
                state1 = awaitItem()
            }
            val successState = state1 as CourseDetailUiState.Success
            assertTrue(successState.sections.first().isExpanded) // Espansa di default la prima

            viewModel.toggleSection(10)
            
            val state2 = awaitItem() as CourseDetailUiState.Success
            assertFalse(state2.sections.first().isExpanded)
        }
    }

    @Test
    fun `toggleFolder opens and closes a folder and survives in saved state`() = runTest {
        initViewModel()

        viewModel.toggleFolder("101:")
        assertEquals(setOf("101:"), viewModel.expandedFolders.value)
        assertEquals(arrayListOf("101:"), savedStateHandle.get<ArrayList<String>>("expandedFolders"))

        viewModel.toggleFolder("101:")
        assertTrue(viewModel.expandedFolders.value.isEmpty())
    }

    @Test
    fun `listContent flattens the expanded first section into rows`() = runTest {
        initViewModel()

        viewModel.listContent.test {
            var content = awaitItem()
            while (content == null) {
                content = awaitItem()
            }
            val header = content.items.first() as CourseListItem.SectionHeader
            assertEquals("General", header.name)
            assertEquals("Summary", header.summary)
            val module = content.items[1] as CourseListItem.Module
            assertEquals(ModuleKind.SingleFile, module.kind)
            assertEquals(GroupPosition.Bottom, module.position)
        }
    }

    @Test
    fun `search query filters the rows and survives in saved state`() = runTest {
        initViewModel()

        viewModel.listContent.test {
            var content = awaitItem()
            while (content == null) {
                content = awaitItem()
            }

            viewModel.setSearchQuery("nothing matches this")
            assertEquals("nothing matches this", savedStateHandle.get<String>("searchQuery"))
            var filtered = awaitItem()
            while (filtered?.noMatches != true) {
                filtered = awaitItem()
            }
            assertTrue(filtered.items.isEmpty())
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
            var state = awaitItem()
            while (state !is CourseDetailUiState.Success) {
                state = awaitItem()
            }
            val section = (state as CourseDetailUiState.Success).sections.first()
            assertEquals(null, section.summary)
            assertEquals("Read & sign", section.modules.first().description)
        }
    }

    @Test
    fun `file size formatting is correct`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state !is CourseDetailUiState.Success) {
                state = awaitItem()
            }
            val successState = state as CourseDetailUiState.Success
            val file = successState.sections.first().modules.first().files.first()
            assertEquals("200.0 KB", file.formattedSize)
        }
    }

    @Test
    fun `downloadFile triggers download and monitors progress successfully`() = runTest {
        val fileUrl = "https://webeep.polimi.it/webservice/pluginfile.php/123/syllabus.pdf"
        coEvery {
            downloadFileUseCase.execute(fileUrl, any(), "syllabus.pdf", "application/pdf")
        } returns 999L
        every { downloadRepository.getDownloadProgress(999L) } returns flowOf(50, 100)

        initViewModel()

        viewModel.uiState.test {
            var successState = awaitItem()
            while (successState !is CourseDetailUiState.Success) {
                successState = awaitItem()
            }
            val file = (successState as CourseDetailUiState.Success).sections.first().modules.first().files.first()
            
            viewModel.downloadFile(file)
            
            // Skip intermediate DOWNLOADING state and wait for COMPLETED
            var finalState = awaitItem()
            while (finalState !is CourseDetailUiState.Success || 
                   finalState.sections.first().modules.first().files.first().downloadState != DownloadState.COMPLETED) {
                finalState = awaitItem()
            }
            val finalFile = (finalState as CourseDetailUiState.Success).sections.first().modules.first().files.first()
            assertEquals(DownloadState.COMPLETED, finalFile.downloadState)
        }

        coVerify(exactly = 1) {
            downloadFileUseCase.execute(
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
            downloadFileUseCase.execute(fileUrl, any(), "syllabus.pdf", "application/pdf")
        } throws Exception("Errore download")

        initViewModel()

        viewModel.uiState.test {
            var successState = awaitItem()
            while (successState !is CourseDetailUiState.Success) {
                successState = awaitItem()
            }
            val file = (successState as CourseDetailUiState.Success).sections.first().modules.first().files.first()
            
            viewModel.downloadFile(file)
            
            var finalState = awaitItem()
            while (finalState !is CourseDetailUiState.Success || 
                   finalState.sections.first().modules.first().files.first().downloadState != DownloadState.FAILED) {
                finalState = awaitItem()
            }
            val finalFile = (finalState as CourseDetailUiState.Success).sections.first().modules.first().files.first()
            assertEquals(DownloadState.FAILED, finalFile.downloadState)
        }
    }

    @Test
    fun `openFile delegates to downloadRepository`() = runTest {
        initViewModel()
        
        viewModel.uiState.test {
            var successState = awaitItem()
            while (successState !is CourseDetailUiState.Success) {
                successState = awaitItem()
            }
            val file = (successState as CourseDetailUiState.Success).sections.first().modules.first().files.first()
            
            viewModel.openFile(file)
            
            coVerify(exactly = 1) {
                downloadRepository.openFile(match { it.startsWith("syllabus-") && it.endsWith(".pdf") })
            }
        }
    }

    @Test
    fun `cache re-emit keeps an in-flight download in DOWNLOADING`() = runTest {
        val fileUrl = "https://webeep.polimi.it/webservice/pluginfile.php/123/syllabus.pdf"
        val cache = MutableStateFlow(sampleSections)
        coEvery { getCourseContentsUseCase(5) } returns cache.map { Resource.Success(it) }
        coEvery { downloadFileUseCase.execute(fileUrl, any(), any(), any()) } returns 7L
        every { downloadRepository.getDownloadProgress(7L) } returns MutableSharedFlow<Int>()

        initViewModel()
        advanceUntilIdle()
        viewModel.downloadFile(firstFile())
        advanceUntilIdle()
        assertEquals(DownloadState.DOWNLOADING, firstFile().downloadState)

        // Background sync rewrites the cache with a fresh copy of the same tree.
        cache.value = sampleSections.map { it.copy(name = "General (synced)") }
        advanceUntilIdle()

        val state = viewModel.uiState.value as CourseDetailUiState.Success
        assertEquals("General (synced)", state.sections.first().name)
        assertEquals(DownloadState.DOWNLOADING, firstFile().downloadState)
    }

    @Test
    fun `cache re-emit keeps FAILED unless the file is now on disk`() = runTest {
        val fileUrl = "https://webeep.polimi.it/webservice/pluginfile.php/123/syllabus.pdf"
        val cache = MutableStateFlow(sampleSections)
        coEvery { getCourseContentsUseCase(5) } returns cache.map { Resource.Success(it) }
        coEvery { downloadFileUseCase.execute(fileUrl, any(), any(), any()) } throws Exception("boom")

        initViewModel()
        advanceUntilIdle()
        viewModel.downloadFile(firstFile())
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

    private fun firstFile() =
        (viewModel.uiState.value as CourseDetailUiState.Success).sections.first().modules.first().files.first()
}
