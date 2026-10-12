package it.polimi.webeep.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import it.polimi.webeep.feature.settings.domain.SettingsRepository
import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : SettingsRepository {
    private val themeKey = stringPreferencesKey("theme_preference")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color_preference")
    private val wifiOnlySyncKey = booleanPreferencesKey("wifi_only_sync")
    private val syncFrequencyKey = stringPreferencesKey("sync_frequency")

    override val themePreference: Flow<ThemePreference> = context.dataStore.data.map { preferences ->
        val raw = preferences[themeKey]
        ThemePreference.entries.firstOrNull { it.name == raw } ?: ThemePreference.SYSTEM_DEFAULT
    }

    override val dynamicColorPreference: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[dynamicColorKey] ?: false
    }

    override val wifiOnlySync: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[wifiOnlySyncKey] ?: false
    }

    override val syncFrequency: Flow<SyncFrequency> = context.dataStore.data.map { preferences ->
        val raw = preferences[syncFrequencyKey]
        SyncFrequency.entries.firstOrNull { it.name == raw } ?: SyncFrequency.DEFAULT
    }

    override suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }

    override suspend fun setDynamicColorPreference(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[dynamicColorKey] = enabled
        }
    }

    override suspend fun setWifiOnlySync(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[wifiOnlySyncKey] = enabled
        }
    }

    override suspend fun setSyncFrequency(frequency: SyncFrequency) {
        context.dataStore.edit { preferences ->
            preferences[syncFrequencyKey] = frequency.name
        }
    }
}
