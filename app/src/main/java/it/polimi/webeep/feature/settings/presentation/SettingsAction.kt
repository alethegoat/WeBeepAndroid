package it.polimi.webeep.feature.settings.presentation

import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference

/** User intents the settings screen sends to its ViewModel. */
sealed interface SettingsAction {
    data class ThemeChange(val theme: ThemePreference) : SettingsAction
    data class DynamicColorChange(val enabled: Boolean) : SettingsAction
    data class WifiOnlyChange(val enabled: Boolean) : SettingsAction
    data class FrequencyChange(val frequency: SyncFrequency) : SettingsAction

    /** Run a sync now, even when background sync is set to never. */
    data object CheckNow : SettingsAction

    /** Sign out and wipe local data. */
    data object Logout : SettingsAction
}
