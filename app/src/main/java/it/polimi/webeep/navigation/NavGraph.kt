@file:OptIn(ExperimentalSharedTransitionApi::class)

package it.polimi.webeep.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import it.polimi.webeep.core.ui.util.LocalNavSharedTransitionScope
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.feature.auth.presentation.loginDestination
import it.polimi.webeep.feature.course.presentation.courseDestination
import it.polimi.webeep.feature.dashboard.presentation.dashboardDestination
import it.polimi.webeep.feature.fileviewer.presentation.fileViewerDestination
import it.polimi.webeep.feature.settings.presentation.settingsDestination

/**
 * Wires the features' destinations together; each feature owns its own screen setup.
 * [startLoggedIn] picks the start destination (dashboard or login) and is read once, when the graph is first built.
 */
@Composable
fun NavGraph(startLoggedIn: Boolean, modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    val reduceMotion = LocalReduceMotion.current
    val transitions = remember(reduceMotion) { NavTransitions(reduceMotion) }
    // Opaque base: screens fading in/out never show the window behind them.
    SharedTransitionLayout(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        CompositionLocalProvider(LocalNavSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = if (startLoggedIn) Dashboard else Login,
                enterTransition = { transitions.enter(initialState.destination, targetState.destination, pop = false) },
                exitTransition = { transitions.exit(initialState.destination, targetState.destination, pop = false) },
                popEnterTransition = { transitions.enter(initialState.destination, targetState.destination, pop = true) },
                popExitTransition = { transitions.exit(initialState.destination, targetState.destination, pop = true) },
                // Passed explicitly: Navigation's defaults would shrink the leaving screen
                // (scaleOut to 0.7). The gesture scrubs the same pop transitions instead, so
                // it looks exactly like the in-app back arrow.
                predictivePopEnterTransition = {
                    transitions.enter(initialState.destination, targetState.destination, pop = true)
                },
                predictivePopExitTransition = {
                    transitions.exit(initialState.destination, targetState.destination, pop = true)
                }
            ) {
                loginDestination(onLoggedIn = navController::navigateToDashboard)
                dashboardDestination(
                    onOpenCourse = { courseId -> navController.navigate(CourseDetail(courseId)) },
                    onOpenSettings = { navController.navigate(Settings) }
                )
                settingsDestination(
                    onBack = navController::navigateUp,
                    onLoggedOut = navController::navigateToLogin
                )
                courseDestination(
                    onOpenViewer = { navController.navigate(it) },
                    onBack = navController::navigateUp
                )
                fileViewerDestination(onBack = navController::navigateUp)
            }
        }
    }
}

/**
 * Clears the whole back stack and shows the login screen. No-op when it is already shown,
 * so manual logout, session expiry and the SSO deep link can all call it without stacking entries.
 */
fun NavController.navigateToLogin() {
    if (currentDestination?.hasRoute<Login>() == true) return
    navigate(Login) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

/** Replaces the login screen with the dashboard, so back never returns to it. */
private fun NavController.navigateToDashboard() {
    navigate(Dashboard) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
