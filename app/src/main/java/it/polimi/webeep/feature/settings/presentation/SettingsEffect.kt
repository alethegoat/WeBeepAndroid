package it.polimi.webeep.feature.settings.presentation

/** One-shot events the settings ViewModel asks the UI to perform. */
sealed interface SettingsEffect {
    /** The session is cleared; the app must leave the signed-in screens. */
    data object LoggedOut : SettingsEffect
}
