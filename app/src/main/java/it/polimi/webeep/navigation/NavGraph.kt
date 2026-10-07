@file:OptIn(ExperimentalSharedTransitionApi::class)

package it.polimi.webeep.navigation

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.util.LocalNavAnimatedVisibilityScope
import it.polimi.webeep.core.ui.util.LocalNavSharedTransitionScope
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.polimi.webeep.feature.auth.presentation.LoginScreen
import it.polimi.webeep.feature.auth.presentation.LoginViewModel
import it.polimi.webeep.feature.course.presentation.CourseDetailScreen
import it.polimi.webeep.feature.course.presentation.CourseDetailViewModel
import it.polimi.webeep.feature.dashboard.presentation.DashboardScreen
import it.polimi.webeep.feature.dashboard.presentation.DashboardViewModel
import it.polimi.webeep.feature.fileviewer.presentation.FileViewerScreen
import it.polimi.webeep.feature.fileviewer.presentation.FileViewerViewModel
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.feature.settings.presentation.SettingsScreen
import it.polimi.webeep.feature.settings.presentation.SettingsViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val reduceMotion = LocalReduceMotion.current
    val context = LocalContext.current
    val sharedAxisOffsetPx = with(LocalDensity.current) { WeBeepMotion.SharedAxisOffsetDp.dp.roundToPx() }
    val transitions = remember(sharedAxisOffsetPx, reduceMotion) {
        NavTransitions(sharedAxisOffsetPx, reduceMotion)
    }
    SharedTransitionLayout(modifier = modifier) {
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
                    onCourseClick = { courseId ->
                        navController.navigate(Screen.CourseDetail.createRoute(courseId))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
        }

        composable(route = Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.navigateUp() },
                onLogoutSuccess = { navController.navigateToLogin() }
            )
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
                val viewModel: CourseDetailViewModel = hiltViewModel()
                CourseDetailScreen(
                    viewModel = viewModel,
                    onPreviewFile = { file ->
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
                    onBackClick = {
                        navController.navigateUp()
                    }
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
                onBackClick = { navController.navigateUp() }
            )
        }
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
