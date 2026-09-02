package it.polimi.webeep.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemePreference {
    SYSTEM_DEFAULT, LIGHT, DARK
}

enum class SyncFrequency {
    FIFTEEN_MINUTES, ONE_HOUR, TWELVE_HOURS, DAILY, NEVER
}

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_KEY = stringPreferencesKey("theme_preference")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color_preference")
    private val WIFI_ONLY_SYNC_KEY = booleanPreferencesKey("wifi_only_sync")
    private val SYNC_FREQUENCY_KEY = stringPreferencesKey("sync_frequency")

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { preferences ->
        val raw = preferences[THEME_KEY]
        ThemePreference.entries.firstOrNull { it.name == raw } ?: ThemePreference.SYSTEM_DEFAULT
    }

    val dynamicColorPreference: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR_KEY] ?: false
    }

    val wifiOnlySync: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WIFI_ONLY_SYNC_KEY] ?: false
    }

    val syncFrequency: Flow<SyncFrequency> = context.dataStore.data.map { preferences ->
        val raw = preferences[SYNC_FREQUENCY_KEY]
        SyncFrequency.entries.firstOrNull { it.name == raw } ?: SyncFrequency.FIFTEEN_MINUTES
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    suspend fun setDynamicColorPreference(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    suspend fun setWifiOnlySync(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WIFI_ONLY_SYNC_KEY] = enabled
        }
    }

    suspend fun setSyncFrequency(frequency: SyncFrequency) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_FREQUENCY_KEY] = frequency.name
        }
    }
}
