package it.polimi.webeep.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import it.polimi.webeep.feature.settings.data.SettingsRepositoryImpl
import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var repository: SettingsRepositoryImpl

    /**
     * The repository keeps its DataStore in a process-wide property delegate that outlives a single
     * test, so each test starts by clearing that instance instead of relying on a fresh file.
     */
    private fun dataStore(): DataStore<Preferences> {
        val getter = Class.forName("it.polimi.webeep.feature.settings.data.SettingsRepositoryImplKt")
            .getDeclaredMethod("access\$getDataStore", Context::class.java)
        getter.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return getter.invoke(null, context) as DataStore<Preferences>
    }

    @Before
    fun setUp() {
        repository = SettingsRepositoryImpl(context)
        runBlocking { dataStore().edit { it.clear() } }
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
        dataStore().edit {
            it[stringPreferencesKey("theme_preference")] = "NOT_A_THEME"
            it[stringPreferencesKey("sync_frequency")] = "EVERY_SECOND"
        }

        assertEquals(ThemePreference.SYSTEM_DEFAULT, repository.themePreference.first())
        assertEquals(SyncFrequency.DEFAULT, repository.syncFrequency.first())
    }
}
