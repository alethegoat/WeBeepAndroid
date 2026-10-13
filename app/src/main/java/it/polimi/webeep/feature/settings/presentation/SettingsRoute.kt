package it.polimi.webeep.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.polimi.webeep.core.ui.util.ObserveEffects

/** Stateful entry of the settings screen: collects the ViewModel state, forwards [onLoggedOut] on the logout effect. */
@Composable
fun SettingsRoute(
    onBackClick: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            SettingsEffect.LoggedOut -> onLoggedOut()
        }
    }

    SettingsScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        modifier = modifier
    )
}
