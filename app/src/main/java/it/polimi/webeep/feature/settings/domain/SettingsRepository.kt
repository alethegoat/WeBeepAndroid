package it.polimi.webeep.feature.settings.domain

import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

/** User preferences that survive restarts. */
interface SettingsRepository {

    val themePreference: Flow<ThemePreference>

    val dynamicColorPreference: Flow<Boolean>

    val wifiOnlySync: Flow<Boolean>

    val syncFrequency: Flow<SyncFrequency>

    suspend fun setThemePreference(theme: ThemePreference)

    suspend fun setDynamicColorPreference(enabled: Boolean)

    suspend fun setWifiOnlySync(enabled: Boolean)

    suspend fun setSyncFrequency(frequency: SyncFrequency)
}
