package it.polimi.webeep.feature.auth.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.showErrorSnackbar
import it.polimi.webeep.core.ui.util.ObserveEffects

@Composable
fun LoginRoute(onLoggedIn: () -> Unit, modifier: Modifier = Modifier, viewModel: LoginViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val retryLabel = stringResource(R.string.retry)

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            LoginEffect.LoggedIn -> onLoggedIn()
            is LoginEffect.ShowError -> showErrorSnackbar(
                snackbarHostState = snackbarHostState,
                message = resources.getString(effect.messageRes),
                actionLabel = retryLabel
            )
        }
    }

    LoginScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onLoginClick = { SsoBrowser.open(context, viewModel.generateSsoUrl()) },
        modifier = modifier
    )
}
