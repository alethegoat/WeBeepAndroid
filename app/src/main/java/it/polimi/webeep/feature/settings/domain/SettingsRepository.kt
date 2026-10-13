package it.polimi.webeep.feature.settings.domain

import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

/** User preferences that survive restarts. */
interface SettingsRepository {

    /** Selected theme, [ThemePreference.SYSTEM_DEFAULT] until the user chooses. */
    val themePreference: Flow<ThemePreference>

    /** Whether to use Material You wallpaper colors (Android 12+); off by default. */
    val dynamicColorPreference: Flow<Boolean>

    /** Whether background sync may only run on unmetered networks; off by default. */
    val wifiOnlySync: Flow<Boolean>

    /** Interval between background syncs, [SyncFrequency.DEFAULT] until the user chooses. */
    val syncFrequency: Flow<SyncFrequency>

    /** Persists the theme; [themePreference] re-emits. */
    suspend fun setThemePreference(theme: ThemePreference)

    /** Persists the dynamic color choice. */
    suspend fun setDynamicColorPreference(enabled: Boolean)

    /** Persists the Wi-Fi-only choice. Does not reschedule sync: use [UpdateWifiOnlySyncUseCase] for that. */
    suspend fun setWifiOnlySync(enabled: Boolean)

    /** Persists the frequency. Does not reschedule sync: use [UpdateSyncFrequencyUseCase] for that. */
    suspend fun setSyncFrequency(frequency: SyncFrequency)
}
