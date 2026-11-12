package it.polimi.webeep.feature.settings.presentation

import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference
import it.polimi.webeep.feature.sync.domain.SyncRunStatus

/**
 * Everything the settings screen renders. [lastCheckAt] is when the background sync last looked
 * for new files (epoch millis); [checkStatus] mirrors the real state of the one-off sync started by "check now".
 */
data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM_DEFAULT,
    val dynamicColorPreference: Boolean = false,
    val wifiOnlySync: Boolean = false,
    val syncFrequency: SyncFrequency = SyncFrequency.DEFAULT,
    val lastCheckAt: Long? = null,
    val checkStatus: SyncRunStatus = SyncRunStatus.IDLE
)
