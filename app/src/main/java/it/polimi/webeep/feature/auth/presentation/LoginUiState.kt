package it.polimi.webeep.feature.auth.presentation

/** What the login screen renders: the button, or a spinner while a callback is being validated. */
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
}
