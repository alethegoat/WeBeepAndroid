package it.polimi.webeep.feature.settings.presentation

import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference

/**
 * Everything the settings screen renders. [lastCheckAt] is when the background sync last looked
 * for new files (epoch millis); [checkInProgress] is true from "check now" until a newer check.
 */
data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM_DEFAULT,
    val dynamicColorPreference: Boolean = false,
    val wifiOnlySync: Boolean = false,
    val syncFrequency: SyncFrequency = SyncFrequency.DEFAULT,
    val lastCheckAt: Long? = null,
    val checkInProgress: Boolean = false
)
