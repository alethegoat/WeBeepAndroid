package it.polimi.webeep.feature.dashboard

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.MissingTokenException
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.database.dao.CourseDao
import it.polimi.webeep.core.database.entity.CourseEntity
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.dto.CourseDto
import it.polimi.webeep.core.network.dto.SiteInfoDto
import it.polimi.webeep.feature.dashboard.data.DashboardRepositoryImpl
import it.polimi.webeep.feature.dashboard.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DashboardRepositoryTest {

    @MockK
    lateinit var apiService: MoodleApiService

    @MockK
    lateinit var courseDao: CourseDao

    @MockK
    lateinit var tokenProvider: TokenProvider

    private lateinit var repository: DashboardRepositoryImpl

    private val sampleSiteInfo = SiteInfoDto(
        sitename = "WeBeep",
        username = "studente123",
        firstname = "Mario",
        lastname = "Rossi",
        fullname = "Mario Rossi",
        lang = "it",
        userid = 12345,
        siteurl = "https://webeep.polimi.it",
        userpictureurl = "https://webeep.polimi.it/pluginfile.php/123/user/icon/f1",
        downloadfiles = 1,
        release = "4.2",
        version = "123",
        functions = emptyList()
    )

    private val sampleCourseDtos = listOf(
        CourseDto(
            id = 1,
            shortname = "CS101",
            fullname = "Intro to CS",
            displayname = "Intro to CS",
            summary = "Summary",
            summaryformat = 1,
            startdate = 1000L,
            enddate = 2000L,
            visible = 1,
            progress = 0.5,
            completed = false,
            isfavourite = true,
            hidden = false,
            enrolledusercount = 100,
            lastaccess = 1500L,
            overviewfiles = emptyList(),
            category = 12
        )
    )

    private val sampleCourseEntities = listOf(
        CourseEntity(
            id = 1,
            shortname = "CS101",
            fullname = "Intro to CS",
            displayname = "Intro to CS",
            summary = "Summary",
            startdate = 1000L,
            enddate = 2000L,
            progress = 0.5,
            courseImage = null,
            category = "12",
            isFavourite = true,
            lastSyncTimestamp = 123456789L
        )
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        repository = DashboardRepositoryImpl(apiService, courseDao, tokenProvider, Dispatchers.Unconfined)
    }

    @Test
    fun `getCoursesFromCache retrieves and maps entities to domain models`() = runTest {
        every { courseDao.getAllCourses() } returns flowOf(sampleCourseEntities)

        val result = repository.getCoursesFromCache().first()

        assertEquals(1, result.size)
        val domain = result.first()
        assertEquals(1, domain.id)
        assertEquals("CS101", domain.shortname)
        assertEquals("Intro to CS", domain.fullname)
        assertEquals(0.5f, domain.progress)
        assertTrue(domain.isFavourite)
    }

    @Test
    fun `syncCourses fails when token is missing`() = runTest {
        coEvery { tokenProvider.getToken() } returns null

        val result = repository.syncCourses()

        assertTrue(result is Resource.Error)
        assertEquals(DataError.Unauthorized, (result as Resource.Error).error)
        assertTrue(result.throwable is MissingTokenException)
    }

    @Test
    fun `syncCourses fetches, caches and returns courses successfully`() = runTest {
        coEvery { tokenProvider.getToken() } returns "mock_token"
        coEvery { tokenProvider.getUserId() } returns 12345
        coEvery { apiService.getEnrolledCourses(userId = 12345) } returns sampleCourseDtos
        coEvery { courseDao.replaceAllCourses(any()) } returns Unit

        val result = repository.syncCourses()

        assertTrue(result is Resource.Success)
        val courses = (result as Resource.Success).data
        assertEquals(1, courses.size)
        assertEquals("Intro to CS", courses.first().fullname)

        coVerify(exactly = 1) { courseDao.replaceAllCourses(any()) }
    }

    @Test
    fun `syncCourses updates userId if missing locally`() = runTest {
        coEvery { tokenProvider.getToken() } returns "mock_token"
        coEvery { tokenProvider.getUserId() } returns null
        coEvery { apiService.getSiteInfo(wsToken = null) } returns sampleSiteInfo
        coEvery { tokenProvider.updateUserId(12345) } returns Unit
        coEvery { apiService.getEnrolledCourses(userId = 12345) } returns sampleCourseDtos
        coEvery { courseDao.replaceAllCourses(any()) } returns Unit

        val result = repository.syncCourses()

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { tokenProvider.updateUserId(12345) }
        coVerify(exactly = 0) { tokenProvider.saveAuthData(any(), any()) }
    }

    @Test
    fun `syncCourses refetches site info when stored userId is zero`() = runTest {
        coEvery { tokenProvider.getToken() } returns "mock_token"
        coEvery { tokenProvider.getUserId() } returns 0
        coEvery { apiService.getSiteInfo(wsToken = null) } returns sampleSiteInfo
        coEvery { tokenProvider.updateUserId(12345) } returns Unit
        coEvery { apiService.getEnrolledCourses(userId = 12345) } returns sampleCourseDtos
        coEvery { courseDao.replaceAllCourses(any()) } returns Unit

        val result = repository.syncCourses()

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { apiService.getEnrolledCourses(userId = 12345) }
    }

    @Test
    fun `getUserInfo fetches site info without putting token in avatar url`() = runTest {
        coEvery { tokenProvider.getToken() } returns "mock_token"
        coEvery { apiService.getSiteInfo(wsToken = null) } returns sampleSiteInfo
        coEvery { tokenProvider.updateUserId(12345) } returns Unit

        val result = repository.getUserInfo()

        assertTrue(result is Resource.Success)
        val profile = (result as Resource.Success).data
        assertEquals("Mario Rossi", profile.fullName)
        val avatar = profile.avatarUrl.orEmpty()
        assertTrue(avatar.contains("/webservice/pluginfile.php/"))
        assertFalse(avatar.contains("token="))
    }

    @Test
    fun `cancelling a running call propagates instead of becoming an error result`() = runTest {
        coEvery { tokenProvider.getToken() } returns "mock_token"
        coEvery { apiService.getSiteInfo(wsToken = null) } coAnswers { awaitCancellation() }
        var result: Resource<UserProfile>? = null

        val job = launch { result = repository.getUserInfo() }
        runCurrent()
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertNull(result)
    }
}
