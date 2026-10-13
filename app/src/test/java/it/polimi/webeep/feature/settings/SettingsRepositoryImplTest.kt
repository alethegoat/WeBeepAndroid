package it.polimi.webeep.feature.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import it.polimi.webeep.feature.settings.data.SettingsRepositoryImpl
import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testScope = CoroutineScope(UnconfinedTestDispatcher() + Job())
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(scope = testScope) {
            File(tempFolder.root, "settings.preferences_pb")
        }
        repository = SettingsRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `defaults apply before anything is saved`() = runTest {
        assertEquals(ThemePreference.SYSTEM_DEFAULT, repository.themePreference.first())
        assertFalse(repository.dynamicColorPreference.first())
        assertFalse(repository.wifiOnlySync.first())
        assertEquals(SyncFrequency.DEFAULT, repository.syncFrequency.first())
    }

    @Test
    fun `theme preference round-trips`() = runTest {
        repository.setThemePreference(ThemePreference.DARK)

        assertEquals(ThemePreference.DARK, repository.themePreference.first())
    }

    @Test
    fun `dynamic color preference round-trips`() = runTest {
        repository.setDynamicColorPreference(true)

        assertTrue(repository.dynamicColorPreference.first())
    }

    @Test
    fun `wifi only sync round-trips`() = runTest {
        repository.setWifiOnlySync(true)
        assertTrue(repository.wifiOnlySync.first())

        repository.setWifiOnlySync(false)
        assertFalse(repository.wifiOnlySync.first())
    }

    @Test
    fun `sync frequency round-trips`() = runTest {
        repository.setSyncFrequency(SyncFrequency.entries.first { it != SyncFrequency.DEFAULT })

        assertEquals(SyncFrequency.entries.first { it != SyncFrequency.DEFAULT }, repository.syncFrequency.first())
    }

    @Test
    fun `settings do not overwrite each other`() = runTest {
        repository.setThemePreference(ThemePreference.LIGHT)
        repository.setWifiOnlySync(true)

        assertEquals(ThemePreference.LIGHT, repository.themePreference.first())
        assertTrue(repository.wifiOnlySync.first())
    }

    @Test
    fun `unknown stored enum names fall back to the defaults`() = runTest {
        dataStore.edit {
            it[stringPreferencesKey("theme_preference")] = "NOT_A_THEME"
            it[stringPreferencesKey("sync_frequency")] = "EVERY_SECOND"
        }

        assertEquals(ThemePreference.SYSTEM_DEFAULT, repository.themePreference.first())
        assertEquals(SyncFrequency.DEFAULT, repository.syncFrequency.first())
    }
}
