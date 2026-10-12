package it.polimi.webeep.feature.auth.presentation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import it.polimi.webeep.navigation.Login

fun NavGraphBuilder.loginDestination(onLoggedIn: () -> Unit) {
    composable<Login> {
        LoginScreen(viewModel = hiltViewModel(), onLoginSuccess = onLoggedIn)
    }
}
