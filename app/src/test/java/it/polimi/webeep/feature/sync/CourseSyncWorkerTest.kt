package it.polimi.webeep.feature.sync

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.model.Course
import it.polimi.webeep.feature.sync.data.SyncNotifier
import it.polimi.webeep.feature.sync.data.SyncStateStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Runs the real worker, detector, store and notifier end to end; only the network and the
 * database are faked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CourseSyncWorkerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Application
    private lateinit var courseRepository: CourseRepository
    private lateinit var store: SyncStateStore
    private var cachedContents: List<Section> = emptyList()
    private var remoteContents: List<Section> = emptyList()

    private val dashboardRepository: DashboardRepository = mockk()
    private val tokenProvider: TokenProvider = mockk()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        store = SyncStateStore({ File(tempFolder.root, SyncStateStore.FILE_NAME) }, Dispatchers.IO)
        courseRepository = mockk()
        every { courseRepository.getCachedCourseContents(COURSE_ID) } answers { flowOf(cachedContents) }
        coEvery { courseRepository.getCourseContents(COURSE_ID) } answers { Resource.Success(remoteContents) }
        every { dashboardRepository.getCoursesFromCache() } returns flowOf(listOf(course()))
        coEvery { dashboardRepository.syncCourses() } returns Resource.Success(emptyList())
        coEvery { tokenProvider.getToken() } returns "token"
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun `first sync is silent, a later new file is notified`() = runTest {
        remoteContents = contents(file("a.pdf", time = 100))
        assertEquals(ListenableWorker.Result.success(), runWorker())
        assertTrue(postedNotifications().isEmpty())

        remoteContents = contents(file("a.pdf", time = 100), file("b.pdf", time = 200))
        runWorker()

        val lines = courseNotificationLines()
        assertEquals(1, lines.size)
        assertTrue(lines.single().contains("b.pdf"))
        assertNotNull(store.update { it to it.lastCheckAt })
    }

    @Test
    fun `updated file is notified`() = runTest {
        remoteContents = contents(file("a.pdf", time = 100))
        runWorker()

        remoteContents = contents(file("a.pdf", time = 300))
        runWorker()

        assertTrue(courseNotificationLines().single().contains("a.pdf"))
    }

    @Test
    fun `file already seen in the app is still notified`() = runTest {
        remoteContents = contents(file("a.pdf", time = 100))
        runWorker()

        // The course screen refreshed the cache before the background sync ran.
        remoteContents = contents(file("a.pdf", time = 100), file("b.pdf", time = 200))
        cachedContents = remoteContents
        runWorker()

        assertTrue(courseNotificationLines().single().contains("b.pdf"))
    }

    @Test
    fun `changes found while notifications are blocked are delivered later`() = runTest {
        remoteContents = contents(file("a.pdf", time = 100))
        runWorker()

        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        remoteContents = contents(file("a.pdf", time = 100), file("b.pdf", time = 200))
        runWorker()
        assertTrue(postedNotifications().isEmpty())

        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        runWorker()

        assertTrue(courseNotificationLines().single().contains("b.pdf"))
    }

    private suspend fun runWorker(): ListenableWorker.Result {
        val worker = TestListenableWorkerBuilder.from(context, CourseSyncWorker::class.java)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker = CourseSyncWorker(
                    appContext,
                    workerParameters,
                    courseRepository,
                    dashboardRepository,
                    tokenProvider,
                    store,
                    SyncNotifier(context, store)
                )
            })
            .build()
        return worker.doWork()
    }

    private fun postedNotifications(): List<Notification> {
        val manager = context.getSystemService(NotificationManager::class.java)
        return shadowOf(manager).allNotifications
    }

    private fun courseNotificationLines(): List<String> {
        val manager = context.getSystemService(NotificationManager::class.java)
        val notification = shadowOf(manager).getNotification(COURSE_ID)
        assertNotNull("No notification posted for the course", notification)
        return notification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            .orEmpty()
            .map { it.toString() }
    }

    private fun course() = Course(
        id = COURSE_ID,
        shortname = "AN1",
        fullname = "Analisi 1",
        summary = null,
        courseImage = null,
        progress = null,
        isFavourite = false,
        category = null,
        startDate = 0L,
        endDate = 0L
    )

    private fun contents(vararg files: FileResource) = listOf(
        Section(
            id = 1,
            name = "Materiale",
            summary = null,
            sectionNumber = 0,
            modules = listOf(
                Module(
                    id = 10,
                    name = "Slides",
                    modName = "folder",
                    iconUrl = null,
                    description = null,
                    url = null,
                    files = files.toList()
                )
            )
        )
    )

    private fun file(name: String, time: Long) = FileResource(
        filename = name,
        filepath = "/",
        fileUrl = "https://webeep.polimi.it/webservice/pluginfile.php/5/mod_folder/content/1/$name",
        fileSize = 1_000L,
        mimeType = "application/pdf",
        timeModified = time
    )

    private companion object {
        const val COURSE_ID = 42
    }
}
