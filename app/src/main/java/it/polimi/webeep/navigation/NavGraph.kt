@file:OptIn(ExperimentalSharedTransitionApi::class)

package it.polimi.webeep.navigation

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.ui.util.LocalNavAnimatedVisibilityScope
import it.polimi.webeep.core.ui.util.LocalNavSharedTransitionScope
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.feature.auth.presentation.LoginScreen
import it.polimi.webeep.feature.auth.presentation.LoginViewModel
import it.polimi.webeep.feature.course.presentation.CourseDetailScreen
import it.polimi.webeep.feature.course.presentation.CourseDetailViewModel
import it.polimi.webeep.feature.dashboard.presentation.DashboardScreen
import it.polimi.webeep.feature.dashboard.presentation.DashboardViewModel
import it.polimi.webeep.feature.fileviewer.presentation.FileViewerScreen
import it.polimi.webeep.feature.fileviewer.presentation.FileViewerViewModel
import it.polimi.webeep.feature.settings.presentation.SettingsScreen
import it.polimi.webeep.feature.settings.presentation.SettingsViewModel

/**
 * Navigation callbacks are wrapped in [dropUnlessResumed]: an entry is resumed only
 * once its enter transition is over, so taps during a transition (a second card,
 * a double back) are ignored instead of stacking destinations.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(startDestination: String, modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    val reduceMotion = LocalReduceMotion.current
    val transitions = remember(reduceMotion) { NavTransitions(reduceMotion) }
    // Opaque base: screens fading in/out never show the window behind them.
    SharedTransitionLayout(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        CompositionLocalProvider(LocalNavSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = {
                    transitions.enter(initialState.destination.route, targetState.destination.route, pop = false)
                },
                exitTransition = {
                    transitions.exit(initialState.destination.route, targetState.destination.route, pop = false)
                },
                popEnterTransition = {
                    transitions.enter(initialState.destination.route, targetState.destination.route, pop = true)
                },
                popExitTransition = {
                    transitions.exit(initialState.destination.route, targetState.destination.route, pop = true)
                },
                // Passed explicitly: Navigation's defaults would shrink the leaving screen
                // (scaleOut to 0.7). The gesture scrubs the same pop transitions instead, so
                // it looks exactly like the in-app back arrow.
                predictivePopEnterTransition = {
                    transitions.enter(initialState.destination.route, targetState.destination.route, pop = true)
                },
                predictivePopExitTransition = {
                    transitions.exit(initialState.destination.route, targetState.destination.route, pop = true)
                }
            ) {
                composable(route = Screen.Login.route) {
                    LoginEntry(navController = navController)
                }

                composable(route = Screen.Dashboard.route) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        val viewModel: DashboardViewModel = hiltViewModel()
                        DashboardScreen(
                            viewModel = viewModel,
                            onCourseClick = dropUnlessResumedWith { courseId: Int ->
                                navController.navigate(Screen.CourseDetail.createRoute(courseId))
                            },
                            onSettingsClick = dropUnlessResumed {
                                navController.navigate(Screen.Settings.route)
                            }
                        )
                    }
                }

                composable(route = Screen.Settings.route) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        val viewModel: SettingsViewModel = hiltViewModel()
                        SettingsScreen(
                            viewModel = viewModel,
                            onBackClick = dropUnlessResumed { navController.navigateUp() },
                            onLogoutSuccess = { navController.navigateToLogin() }
                        )
                    }
                }

                composable(
                    route = Screen.CourseDetail.route,
                    arguments = listOf(
                        navArgument(Screen.CourseDetail.COURSE_ID_ARG) {
                            type = NavType.IntType
                        }
                    )
                ) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        val context = LocalContext.current
                        val viewModel: CourseDetailViewModel = hiltViewModel()
                        CourseDetailScreen(
                            viewModel = viewModel,
                            onPreviewFile = dropUnlessResumedWith { file ->
                                val url = MoodleFileUrlBuilder.stripAuthFromUrl(file.fileUrl)
                                if (MoodleFileUrlBuilder.isExternalWebLink(url)) {
                                    // Straight to the Custom Tab: no viewer flash on the way.
                                    CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
                                } else {
                                    navController.navigate(
                                        Screen.FileViewer.createRoute(
                                            url = url,
                                            title = file.filename,
                                            mimeType = file.mimeType,
                                            courseId = viewModel.courseId
                                        )
                                    )
                                }
                            },
                            onBackClick = dropUnlessResumed { navController.navigateUp() }
                        )
                    }
                }

                composable(
                    route = Screen.FileViewer.route,
                    arguments = listOf(
                        navArgument(Screen.FileViewer.URL_ARG) {
                            type = NavType.StringType
                            nullable = false
                        },
                        navArgument(Screen.FileViewer.TITLE_ARG) {
                            type = NavType.StringType
                            nullable = false
                        },
                        navArgument(Screen.FileViewer.MIME_TYPE_ARG) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument(Screen.FileViewer.COURSE_ID_ARG) {
                            type = NavType.IntType
                            defaultValue = Screen.FileViewer.NO_COURSE
                        }
                    )
                ) {
                    val viewModel: FileViewerViewModel = hiltViewModel()
                    FileViewerScreen(
                        viewModel = viewModel,
                        onBackClick = dropUnlessResumed { navController.navigateUp() }
                    )
                }
            }
        }
    }
}

/** [dropUnlessResumed] for callbacks that take an argument. */
@Composable
private fun <T> dropUnlessResumedWith(block: (T) -> Unit): (T) -> Unit {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBlock by rememberUpdatedState(block)
    return remember(lifecycleOwner) {
        { value ->
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                currentBlock(value)
            }
        }
    }
}

/**
 * Clears the whole back stack and shows the login screen. No-op when it is already shown,
 * so manual logout and session expiry can both call it without stacking entries.
 */
fun NavController.navigateToLogin() {
    if (currentDestination?.route == Screen.Login.route) return
    navigate(Screen.Login.route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun LoginEntry(navController: NavHostController) {
    val viewModel: LoginViewModel = hiltViewModel()

    LoginScreen(
        viewModel = viewModel,
        onLoginSuccess = {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    )
}
