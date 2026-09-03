@file:OptIn(ExperimentalSharedTransitionApi::class)

package it.polimi.webeep.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.util.LocalNavAnimatedVisibilityScope
import it.polimi.webeep.core.ui.util.LocalNavSharedTransitionScope
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
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
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalNavSharedTransitionScope provides this) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            when {
                reduceMotion -> fadeIn(animationSpec = WeBeepMotion.enter(durationMs = 0, reduceMotion = true))
                isCourseListTransform() -> fadeIn(animationSpec = WeBeepMotion.enter())
                else -> slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = WeBeepMotion.enter(),
                    initialOffset = { it / 6 }
                ) + fadeIn(animationSpec = WeBeepMotion.enter())
            }
        },
        exitTransition = {
            when {
                reduceMotion -> fadeOut(animationSpec = WeBeepMotion.exit(durationMs = 0, reduceMotion = true))
                isCourseListTransform() -> fadeOut(animationSpec = WeBeepMotion.exit())
                else -> slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = WeBeepMotion.exit(),
                    targetOffset = { it / 10 }
                ) + fadeOut(animationSpec = WeBeepMotion.exit())
            }
        },
        popEnterTransition = {
            when {
                reduceMotion -> fadeIn(animationSpec = WeBeepMotion.enter(durationMs = 0, reduceMotion = true))
                isCourseListTransform() -> fadeIn(animationSpec = WeBeepMotion.enter())
                else -> slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = WeBeepMotion.enter(),
                    initialOffset = { it / 6 }
                ) + fadeIn(animationSpec = WeBeepMotion.enter())
            }
        },
        popExitTransition = {
            when {
                reduceMotion -> fadeOut(animationSpec = WeBeepMotion.exit(durationMs = 0, reduceMotion = true))
                isCourseListTransform() -> fadeOut(animationSpec = WeBeepMotion.exit())
                else -> slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = WeBeepMotion.exit(),
                    targetOffset = { it / 10 }
                ) + fadeOut(animationSpec = WeBeepMotion.exit())
            }
        }
    ) {
        composable(route = Screen.Login.route) {
            LoginEntry(
                token = null,
                navController = navController
            )
        }

        composable(
            route = Screen.Login.routeWithToken,
            arguments = listOf(
                navArgument(Screen.Login.TOKEN_ARG) {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            LoginEntry(
                token = backStackEntry.arguments?.getString(Screen.Login.TOKEN_ARG),
                navController = navController
            )
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
                onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
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
                        navController.navigate(
                            Screen.FileViewer.createRoute(
                                url = MoodleFileUrlBuilder.stripAuthFromUrl(file.fileUrl),
                                title = file.filename,
                                mimeType = file.mimeType
                            )
                        )
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

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isCourseListTransform(): Boolean {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return (from == Screen.Dashboard.route && to == Screen.CourseDetail.route) ||
        (from == Screen.CourseDetail.route && to == Screen.Dashboard.route)
}

@Composable
private fun LoginEntry(
    token: String?,
    navController: NavHostController
) {
    val viewModel: LoginViewModel = hiltViewModel()

    LaunchedEffect(token) {
        if (!token.isNullOrBlank()) {
            viewModel.handleSsoCallback(token)
        } else {
            viewModel.resetState()
        }
    }

    LoginScreen(
        viewModel = viewModel,
        onLoginSuccess = {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    )
}
