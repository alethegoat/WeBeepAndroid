package it.polimi.webeep.feature.auth.presentation

/** User intents of the login screen. */
sealed interface LoginAction {
    /** The login button was tapped: start an SSO attempt. */
    data object StartSso : LoginAction
}
