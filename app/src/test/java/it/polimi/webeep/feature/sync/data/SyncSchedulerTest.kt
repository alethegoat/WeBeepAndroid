package it.polimi.webeep.feature.sync.data

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.feature.settings.domain.SettingsRepository
import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncSchedulerTest {

    private lateinit var workManager: WorkManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var tokenProvider: TokenProvider
    private lateinit var scheduler: SyncScheduler

    @Before
    fun setup() {
        workManager = mockk(relaxed = true)
        settingsRepository = mockk()
        every { settingsRepository.syncFrequency } returns flowOf(SyncFrequency.ONE_HOUR)
        every { settingsRepository.wifiOnlySync } returns flowOf(false)
        tokenProvider = mockk(relaxed = true)
        coEvery { tokenProvider.getToken() } returns "token"
        scheduler = SyncScheduler(settingsRepository, tokenProvider) { workManager }
    }

    @Test
    fun `immediate sync keeps running work and requires battery not low`() = runTest {
        val request = slot<OneTimeWorkRequest>()

        scheduler.enqueueImmediate()

        verify {
            workManager.enqueueUniqueWork(
                SyncScheduler.UNIQUE_ONE_SHOT_WORK,
                ExistingWorkPolicy.KEEP,
                capture(request)
            )
        }
        assertTrue(request.captured.workSpec.constraints.requiresBatteryNotLow())
    }

    @Test
    fun `immediate sync is skipped without a session`() = runTest {
        coEvery { tokenProvider.getToken() } returns null

        scheduler.enqueueImmediate()

        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any<String>(), any(), any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `periodic sync is kept when the session is persisted but not cached in memory yet`() = runTest {
        // Cold start: the in-memory token is not loaded yet, the persisted one exists.
        every { tokenProvider.getTokenSync() } returns null

        scheduler.applyCurrentSettings(replaceExisting = false)

        verify(exactly = 0) { workManager.cancelUniqueWork(SyncScheduler.UNIQUE_PERIODIC_WORK) }
        verify {
            workManager.enqueueUniquePeriodicWork(
                SyncScheduler.UNIQUE_PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                any<PeriodicWorkRequest>()
            )
        }
    }

    @Test
    fun `manual check runs even when background sync is disabled`() = runTest {
        every { settingsRepository.syncFrequency } returns flowOf(SyncFrequency.NEVER)

        scheduler.enqueueImmediate(manual = true)

        verify {
            workManager.enqueueUniqueWork(SyncScheduler.UNIQUE_ONE_SHOT_WORK, ExistingWorkPolicy.KEEP, any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `default sync frequency is one hour`() {
        assertEquals(SyncFrequency.ONE_HOUR, SyncFrequency.DEFAULT)
    }
}
