package it.polimi.webeep.feature.settings

import app.cash.turbine.test
import it.polimi.webeep.feature.settings.domain.CheckForUpdatesNowUseCase
import it.polimi.webeep.feature.settings.domain.UpdateSyncFrequencyUseCase
import it.polimi.webeep.feature.settings.domain.UpdateWifiOnlySyncUseCase
import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference
import it.polimi.webeep.feature.settings.presentation.SettingsAction
import it.polimi.webeep.feature.settings.presentation.SettingsEffect
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

        vm.onAction(SettingsAction.ThemeChange(ThemePreference.LIGHT))
        vm.onAction(SettingsAction.DynamicColorChange(true))

        assertEquals(ThemePreference.LIGHT, settings.themePreference.value)
        assertTrue(settings.dynamicColorPreference.value)
        assertEquals(0, scheduling.applyCount)
    }

    @Test
    fun `changing the frequency persists it then reschedules`() = runTest {
        viewModel().onAction(SettingsAction.FrequencyChange(SyncFrequency.NEVER))

        assertEquals(SyncFrequency.NEVER, settings.syncFrequency.value)
        assertEquals(listOf("frequency=NEVER", "apply"), settings.events)
    }

    @Test
    fun `changing wifi only persists it then reschedules`() = runTest {
        viewModel().onAction(SettingsAction.WifiOnlyChange(true))

        assertEquals(listOf("wifiOnly=true", "apply"), settings.events)
    }

    @Test
    fun `ui state exposes the last check time`() = runTest {
        syncState.lastCheckAt.value = 10L

        viewModel().uiState.test {
            assertEquals(10L, expectMostRecentItem().lastCheckAt)
        }
    }

    @Test
    fun `check now enqueues a manual sync and stays in progress until a newer check`() = runTest {
        syncState.lastCheckAt.value = 10L
        val vm = viewModel()

        vm.uiState.test {
            assertFalse(expectMostRecentItem().checkInProgress)
            vm.onAction(SettingsAction.CheckNow)
            assertTrue(awaitItem().checkInProgress)
            syncState.lastCheckAt.value = 20L
            val done = awaitItem()
            assertFalse(done.checkInProgress)
            assertEquals(20L, done.lastCheckAt)
        }
        assertEquals(listOf(true), scheduling.immediateRequests)
    }

    @Test
    fun `logout clears the session then emits LoggedOut`() = runTest {
        auth.isLoggedInValue = true
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(SettingsAction.Logout)

            assertEquals(SettingsEffect.LoggedOut, awaitItem())
            assertFalse(auth.isLoggedInValue)
        }
    }
}
