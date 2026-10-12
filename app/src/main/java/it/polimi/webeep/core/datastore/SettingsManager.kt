package it.polimi.webeep.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemePreference {
    SYSTEM_DEFAULT,
    LIGHT,
    DARK
}

enum class SyncFrequency {
    FIFTEEN_MINUTES,
    ONE_HOUR,
    TWELVE_HOURS,
    DAILY,
    NEVER
}

@Singleton
class SettingsManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_preference")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color_preference")
    private val wifiOnlySyncKey = booleanPreferencesKey("wifi_only_sync")
    private val syncFrequencyKey = stringPreferencesKey("sync_frequency")

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { preferences ->
        val raw = preferences[themeKey]
        ThemePreference.entries.firstOrNull { it.name == raw } ?: ThemePreference.SYSTEM_DEFAULT
    }

    val dynamicColorPreference: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[dynamicColorKey] ?: false
    }

    val wifiOnlySync: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[wifiOnlySyncKey] ?: false
    }

    val syncFrequency: Flow<SyncFrequency> = context.dataStore.data.map { preferences ->
        val raw = preferences[syncFrequencyKey]
        SyncFrequency.entries.firstOrNull { it.name == raw } ?: DEFAULT_SYNC_FREQUENCY
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }

    suspend fun setDynamicColorPreference(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[dynamicColorKey] = enabled
        }
    }

    suspend fun setWifiOnlySync(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[wifiOnlySyncKey] = enabled
        }
    }

    suspend fun setSyncFrequency(frequency: SyncFrequency) {
        context.dataStore.edit { preferences ->
            preferences[syncFrequencyKey] = frequency.name
        }
    }

    companion object {
        val DEFAULT_SYNC_FREQUENCY = SyncFrequency.ONE_HOUR
    }
}
