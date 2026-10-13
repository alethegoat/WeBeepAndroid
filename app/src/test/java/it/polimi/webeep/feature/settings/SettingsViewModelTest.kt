package it.polimi.webeep.feature.settings

import app.cash.turbine.test
import it.polimi.webeep.feature.settings.domain.CheckForUpdatesNowUseCase
import it.polimi.webeep.feature.settings.domain.UpdateSyncFrequencyUseCase
import it.polimi.webeep.feature.settings.domain.UpdateWifiOnlySyncUseCase
import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference
import it.polimi.webeep.feature.settings.presentation.SettingsUiState
import it.polimi.webeep.feature.settings.presentation.SettingsViewModel
import it.polimi.webeep.util.MainDispatcherRule
import it.polimi.webeep.util.fakes.FakeAuthRepository
import it.polimi.webeep.util.fakes.FakeSettingsRepository
import it.polimi.webeep.util.fakes.FakeSyncScheduling
import it.polimi.webeep.util.fakes.FakeSyncStateRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private val auth = FakeAuthRepository()
    private val scheduling = FakeSyncScheduling(settings.events)
    private val syncState = FakeSyncStateRepository()

    private fun viewModel() = SettingsViewModel(
        settingsRepository = settings,
        authRepository = auth,
        updateWifiOnlySync = UpdateWifiOnlySyncUseCase(settings, scheduling),
        updateSyncFrequency = UpdateSyncFrequencyUseCase(settings, scheduling),
        checkForUpdatesNow = CheckForUpdatesNowUseCase(scheduling),
        syncStateRepository = syncState
    )

    @Test
    fun `ui state starts with the defaults`() = runTest {
        viewModel().uiState.test {
            assertEquals(SettingsUiState(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ui state follows the repository flows`() = runTest {
        settings.themePreference.value = ThemePreference.DARK
        settings.dynamicColorPreference.value = true
        settings.wifiOnlySync.value = true
        settings.syncFrequency.value = SyncFrequency.DAILY

        viewModel().uiState.test {
            assertEquals(
                SettingsUiState(
                    themePreference = ThemePreference.DARK,
                    dynamicColorPreference = true,
                    wifiOnlySync = true,
                    syncFrequency = SyncFrequency.DAILY
                ),
                expectMostRecentItem()
            )
        }
    }

    @Test
    fun `changing theme and dynamic color persists them without rescheduling`() = runTest {
        val vm = viewModel()

        vm.setThemePreference(ThemePreference.LIGHT)
        vm.setDynamicColorPreference(true)

        assertEquals(ThemePreference.LIGHT, settings.themePreference.value)
        assertTrue(settings.dynamicColorPreference.value)
        assertEquals(0, scheduling.applyCount)
    }

    @Test
    fun `changing the frequency persists it then reschedules`() = runTest {
        viewModel().setSyncFrequency(SyncFrequency.NEVER)

        assertEquals(SyncFrequency.NEVER, settings.syncFrequency.value)
        assertEquals(listOf("frequency=NEVER", "apply"), settings.events)
    }

    @Test
    fun `changing wifi only persists it then reschedules`() = runTest {
        viewModel().setWifiOnlySync(true)

        assertEquals(listOf("wifiOnly=true", "apply"), settings.events)
    }

    @Test
    fun `check now enqueues a manual sync and stays in progress until a newer check`() = runTest {
        syncState.lastCheckAt.value = 10L
        val vm = viewModel()

        vm.checkInProgress.test {
            assertFalse(awaitItem())
            vm.checkNow()
            assertTrue(awaitItem())
            syncState.lastCheckAt.value = 20L
            assertFalse(awaitItem())
        }
        assertEquals(listOf(true), scheduling.immediateRequests)
    }

    @Test
    fun `logout clears the session then calls back`() = runTest {
        auth.isLoggedInValue = true
        var completed = false

        viewModel().logout { completed = true }

        assertFalse(auth.isLoggedInValue)
        assertTrue(completed)
    }
}
