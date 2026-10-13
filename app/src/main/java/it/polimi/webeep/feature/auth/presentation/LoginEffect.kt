package it.polimi.webeep.feature.auth.presentation

import androidx.annotation.StringRes

/** One-shot events the login ViewModel asks the UI to perform. */
sealed interface LoginEffect {
    /** A valid session exists; leave the login screen. */
    data object LoggedIn : LoginEffect

    /** Show [messageRes] in a snackbar with a retry action. */
    data class ShowError(@StringRes val messageRes: Int) : LoginEffect
}
