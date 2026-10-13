package it.polimi.webeep.feature.dashboard.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.AuroraBackground
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.STAGGER_STEP_MS
import it.polimi.webeep.feature.dashboard.presentation.components.CourseListSkeleton
import it.polimi.webeep.feature.dashboard.presentation.components.DashboardCourseGrid
import it.polimi.webeep.feature.dashboard.presentation.components.DashboardTopBar
import it.polimi.webeep.feature.dashboard.presentation.components.ENTRANCE_ITEM_COUNT
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter

/** Long enough for the last staggered card to settle. */
private const val ENTRANCE_DURATION_MS = ENTRANCE_ITEM_COUNT * STAGGER_STEP_MS + WeBeepMotion.MEDIUM_MS + 100L

/** Course grid with search, filters, pull-to-refresh and loading, empty and error states, driven entirely by [state]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAction: (DashboardAction) -> Unit,
    onCourseClick: (courseId: Int) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyGridState = rememberLazyGridState()
    HideKeyboardOnScroll(lazyGridState)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val reduceMotion = LocalReduceMotion.current
    val entrancePending = rememberEntrancePending(loaded = state.content is DashboardContent.Success)

    // Opaque, calm backdrop: the course screen grows over it without ghosting.
    AuroraBackground(modifier = modifier, animated = false) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                DashboardTopBar(
                    userName = state.userName.takeIf { state.content is DashboardContent.Success },
                    avatarUrl = state.userAvatar,
                    scrollBehavior = scrollBehavior,
                    onSettingsClick = onSettingsClick
                )
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = state.content,
                contentKey = { it::class },
                transitionSpec = {
                    fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)) togetherWith
                        fadeOut(WeBeepMotion.exit(reduceMotion = reduceMotion))
                },
                label = "dashboard_state",
                modifier = Modifier.fillMaxSize()
            ) { content ->
                when (content) {
                    is DashboardContent.Loading -> CourseListSkeleton(
                        modifier = Modifier
                            .padding(top = innerPadding.calculateTopPadding())
                            .testTag(TestTags.COURSE_SKELETON)
                    )
                    is DashboardContent.Success -> DashboardCourseGrid(
                        state = state,
                        content = content,
                        gridState = lazyGridState,
                        contentPadding = innerPadding,
                        onAction = onAction,
                        onCourseClick = onCourseClick,
                        playEntrance = entrancePending
                    )
                    is DashboardContent.Error -> EmptyStateView(
                        icon = Icons.Rounded.Warning,
                        title = stringResource(R.string.dashboard_error_title),
                        subtitle = stringResource(content.messageRes),
                        actionLabel = stringResource(R.string.retry),
                        onAction = { onAction(DashboardAction.Retry) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/** Collected, not read in composition: scrolling never recomposes the screen. */
@Composable
private fun HideKeyboardOnScroll(gridState: LazyGridState) {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(gridState, keyboard) {
        snapshotFlow { gridState.isScrollInProgress }
            .filter { it }
            .collect { keyboard?.hide() }
    }
}

/**
 * Cards rise in one after another on the first load only: saved, so coming
 * back from a course (or rotating) shows them in place. True while it still has to play.
 */
@Composable
private fun rememberEntrancePending(loaded: Boolean): Boolean {
    val reduceMotion = LocalReduceMotion.current
    var entrancePlayed by rememberSaveable { mutableStateOf(reduceMotion) }
    LaunchedEffect(loaded) {
        if (loaded && !entrancePlayed) {
            delay(ENTRANCE_DURATION_MS)
            entrancePlayed = true
        }
    }
    return !entrancePlayed
}

internal val previewCourses = listOf(
    CourseUiModel(
        id = 1,
        shortname = "052537",
        fullname = "052537 - TECHNOLOGIES FOR INFORMATION SYSTEMS (TANCA LETIZIA) [2025-26]",
        progress = 0.65f
    ),
    CourseUiModel(
        id = 2,
        shortname = "097683",
        fullname = "097683 - Mathematical Analysis 1 (Rossi Mario) [2025-26]",
        progress = 0.3f
    )
)

@Composable
private fun PreviewScreen(state: DashboardUiState) {
    WeBeepTheme {
        DashboardScreen(state = state, onAction = {}, onCourseClick = {}, onSettingsClick = {})
    }
}

internal fun previewContentState(courses: List<CourseUiModel>, isOnline: Boolean = true) = DashboardUiState(
    content = DashboardContent.Success(courses = courses, visibleCourses = courses),
    userName = "Alessio Rossi",
    isOnline = isOnline
)

@Preview(name = "Loading")
@Composable
private fun DashboardScreenPreviewLoading() {
    PreviewScreen(DashboardUiState())
}

@Preview(name = "Content")
@Composable
private fun DashboardScreenPreviewContent() {
    PreviewScreen(previewContentState(previewCourses))
}

@Preview(name = "Empty")
@Composable
private fun DashboardScreenPreviewEmpty() {
    PreviewScreen(previewContentState(emptyList()))
}

@Preview(name = "Error")
@Composable
private fun DashboardScreenPreviewError() {
    PreviewScreen(DashboardUiState(content = DashboardContent.Error(R.string.error_data_network)))
}

@Preview(name = "Offline with cache")
@Composable
private fun DashboardScreenPreviewOfflineWithCache() {
    PreviewScreen(previewContentState(previewCourses, isOnline = false))
}
