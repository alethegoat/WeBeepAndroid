package it.polimi.webeep.feature.settings

import it.polimi.webeep.feature.settings.domain.CheckForUpdatesNowUseCase
import it.polimi.webeep.feature.settings.domain.UpdateSyncFrequencyUseCase
import it.polimi.webeep.feature.settings.domain.UpdateWifiOnlySyncUseCase
import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.util.fakes.FakeSettingsRepository
import it.polimi.webeep.util.fakes.FakeSyncScheduling
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsUseCasesTest {

    private val settings = FakeSettingsRepository()
    private val scheduling = FakeSyncScheduling(settings.events)

    @Test
    fun `updating the frequency persists it before rescheduling`() = runTest {
        UpdateSyncFrequencyUseCase(settings, scheduling)(SyncFrequency.DAILY)

        assertEquals(SyncFrequency.DAILY, settings.syncFrequency.value)
        assertEquals(listOf("frequency=DAILY", "apply"), settings.events)
    }

    @Test
    fun `updating wifi only persists it before rescheduling`() = runTest {
        UpdateWifiOnlySyncUseCase(settings, scheduling)(true)

        assertEquals(true, settings.wifiOnlySync.value)
        assertEquals(listOf("wifiOnly=true", "apply"), settings.events)
    }

    @Test
    fun `checking now enqueues a manual sync`() = runTest {
        CheckForUpdatesNowUseCase(scheduling)()

        assertEquals(listOf(true), scheduling.immediateRequests)
    }
}
