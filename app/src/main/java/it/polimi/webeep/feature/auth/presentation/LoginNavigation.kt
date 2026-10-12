package it.polimi.webeep.feature.auth.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import it.polimi.webeep.navigation.Login

/** Registers the login destination. */
fun NavGraphBuilder.loginDestination(onLoggedIn: () -> Unit) {
    composable<Login> {
        LoginRoute(onLoggedIn = onLoggedIn)
    }
}
