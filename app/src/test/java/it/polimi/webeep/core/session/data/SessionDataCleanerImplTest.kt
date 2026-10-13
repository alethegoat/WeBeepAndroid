package it.polimi.webeep.core.session.data

import android.app.Application
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import it.polimi.webeep.core.common.AppDirectories
import it.polimi.webeep.core.database.WeBeepDatabase
import it.polimi.webeep.feature.download.domain.DownloadRepository
import it.polimi.webeep.feature.sync.domain.SyncNotifications
import it.polimi.webeep.feature.sync.domain.SyncStateRepository
import java.io.File
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SessionDataCleanerImplTest {

    private val database: WeBeepDatabase = mockk(relaxed = true)
    private val downloads: DownloadRepository = mockk(relaxed = true)
    private val syncState: SyncStateRepository = mockk(relaxed = true)
    private val notifications: SyncNotifications = mockk(relaxed = true)
    private lateinit var application: Application

    @Before
    fun setup() {
        application = RuntimeEnvironment.getApplication()
    }

    private fun TestScope.cleaner(): SessionDataCleanerImpl {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return SessionDataCleanerImpl(application, database, downloads, syncState, notifications, dispatcher, dispatcher)
    }

    @Test
    fun `wipe clears every store and deletes the cache directories`() = runTest {
        val dirs = listOf(AppDirectories.HTTP_CACHE, AppDirectories.VIEWER_CACHE, AppDirectories.IMAGE_CACHE)
            .map { File(application.cacheDir, it) }
        dirs.forEach { File(it, "nested").apply { mkdirs() }.resolve("f.bin").writeText("x") }

        cleaner().wipe()

        verify(exactly = 1) { database.clearAllTables() }
        verify(exactly = 1) { downloads.clearAllDownloads() }
        coVerify(exactly = 1) { syncState.clear() }
        verify(exactly = 1) { notifications.cancelAll() }
        dirs.forEach { assertFalse(it.exists()) }
    }

    @Test
    fun `a failing step does not stop the remaining ones`() = runTest {
        every { database.clearAllTables() } throws IllegalStateException("db closed")
        every { downloads.clearAllDownloads() } throws java.io.IOException("disk")
        coEvery { syncState.clear() } throws IllegalStateException("store")
        val cache = File(application.cacheDir, AppDirectories.VIEWER_CACHE).apply { mkdirs() }

        cleaner().wipe()

        verify(exactly = 1) { downloads.clearAllDownloads() }
        coVerify(exactly = 1) { syncState.clear() }
        verify(exactly = 1) { notifications.cancelAll() }
        assertFalse(cache.exists())
    }
}
