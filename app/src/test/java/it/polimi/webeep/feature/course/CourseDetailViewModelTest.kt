package it.polimi.webeep.feature.course

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.course.domain.GetCourseContentsUseCase
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.course.presentation.CourseDetailUiState
import it.polimi.webeep.feature.course.presentation.CourseDetailViewModel
import it.polimi.webeep.feature.course.presentation.DownloadState
import it.polimi.webeep.feature.dashboard.MainDispatcherRule
import it.polimi.webeep.feature.download.domain.DownloadFileUseCase
import it.polimi.webeep.feature.download.domain.DownloadRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
            savedStateHandle
        )
    }

    @Test
    fun `loadContents emits Loading then Success`() = runTest {
        coEvery { getCourseContentsUseCase(5) } returns flowOf(Resource.Loading, Resource.Success(sampleSections))

        initViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is CourseDetailUiState.Success)
            val success = state as CourseDetailUiState.Success
            assertEquals("Informatica 1", success.courseName)
            assertEquals(1, success.sections.size)
            assertEquals("General", success.sections.first().name)
        }
    }

    @Test
    fun `loadContents emits Error on failure`() = runTest {
        coEvery { getCourseContentsUseCase(5) } returns flowOf(Resource.Error("Errore API"))

        initViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is CourseDetailUiState.Error)
            assertEquals("Errore API", (state as CourseDetailUiState.Error).message)
        }
    }

    @Test
    fun `toggleSection changes section expanded state`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            val state1 = expectMostRecentItem() as CourseDetailUiState.Success
            assertTrue(state1.sections.first().isExpanded) // Espansa di default la prima

            viewModel.toggleSection(10)
            
            val state2 = expectMostRecentItem() as CourseDetailUiState.Success
            assertFalse(state2.sections.first().isExpanded)
        }
    }

    @Test
    fun `file size formatting is correct`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem() as CourseDetailUiState.Success
            val file = state.sections.first().modules.first().files.first()
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
            val successState = expectMostRecentItem() as CourseDetailUiState.Success
            val file = successState.sections.first().modules.first().files.first()
            
            viewModel.downloadFile(file)
            
            // Monitora il cambio di stato a COMPLETED alla fine
            val finalState = expectMostRecentItem() as CourseDetailUiState.Success
            val finalFile = finalState.sections.first().modules.first().files.first()
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
            val successState = expectMostRecentItem() as CourseDetailUiState.Success
            val file = successState.sections.first().modules.first().files.first()
            
            viewModel.downloadFile(file)
            
            val finalState = expectMostRecentItem() as CourseDetailUiState.Success
            val finalFile = finalState.sections.first().modules.first().files.first()
            assertEquals(DownloadState.FAILED, finalFile.downloadState)
        }
    }

    @Test
    fun `openFile delegates to downloadRepository`() = runTest {
        initViewModel()
        
        viewModel.uiState.test {
            val successState = expectMostRecentItem() as CourseDetailUiState.Success
            val file = successState.sections.first().modules.first().files.first()
            
            viewModel.openFile(file)
            
            coVerify(exactly = 1) {
                downloadRepository.openFile(match { it.startsWith("syllabus-") && it.endsWith(".pdf") })
            }
        }
    }
}
