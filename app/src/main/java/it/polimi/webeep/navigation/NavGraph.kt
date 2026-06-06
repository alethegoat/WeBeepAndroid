package it.polimi.webeep.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
import it.polimi.webeep.feature.settings.presentation.SettingsScreen
import it.polimi.webeep.feature.settings.presentation.SettingsViewModel

/**
 * main navigation graph for the webeep app.
 */
@Composable
fun NavGraph(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(350))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(350))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(350))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(350))
        }
    ) {
        // login entry point without token
        composable(route = Screen.Login.route) {
            LoginEntry(
                token = null,
                navController = navController
            )
        }

        // login entry point invoked via sso deep link containing the token
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

        composable(route = Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.navigateUp() },
                onLogoutSuccess = {
                    // clear the backstack when logging out to prevent going back
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
            val viewModel: CourseDetailViewModel = hiltViewModel()
            CourseDetailScreen(
                viewModel = viewModel,
                onPreviewFile = { file ->
                    navController.navigate(
                        Screen.FileViewer.createRoute(
                            url = file.fileUrl,
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
