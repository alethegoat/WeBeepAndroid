package it.polimi.webeep.feature.course.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import it.polimi.webeep.R
import it.polimi.webeep.core.common.CourseTitle
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.courseSharedBounds
import it.polimi.webeep.core.ui.util.rememberNavTransitionRunning
import it.polimi.webeep.feature.course.presentation.components.CourseContentList
import it.polimi.webeep.feature.course.presentation.components.CourseSectionsSkeleton
import it.polimi.webeep.feature.course.presentation.components.CourseTopBar
import it.polimi.webeep.feature.course.presentation.components.MaxContentWidth
import kotlinx.coroutines.flow.filter

/** Searchable list of the course's sections, folders and files, driven entirely by [state]. */
@Composable
fun CourseDetailScreen(
    state: CourseDetailUiState,
    onAction: (CourseDetailAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    HideKeyboardOnScroll(lazyListState)

    // Driven by the list itself (not nested-scroll offsets) so the bar is also
    // opaque when the list is restored already scrolled.
    val isContentScrolled by remember(lazyListState) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    val isHeaderHidden by remember(lazyListState) {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }
    val showsList = state.content is CourseDetailContent.Content
    val fallbackTitle = stringResource(R.string.course_content_title)
    val courseTitle = state.courseTitle
        ?: remember(fallbackTitle) { CourseTitle(code = null, name = fallbackTitle, teacher = null, academicYear = null) }

    Scaffold(
        // One stable, opaque container for the card -> screen transform, whatever the state.
        modifier = modifier.courseSharedBounds(state.courseId, isScreen = true),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CourseTopBar(
                title = courseTitle.name,
                showTitle = showsList && isHeaderHidden,
                isScrolled = showsList && isContentScrolled,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        CourseDetailContent(
            state = state,
            courseTitle = courseTitle,
            lazyListState = lazyListState,
            innerPadding = innerPadding,
            onAction = onAction
        )
    }
}

/** Collected, not read in composition: scrolling never recomposes the screen. */
@Composable
private fun HideKeyboardOnScroll(lazyListState: LazyListState) {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(lazyListState, keyboard) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .filter { it }
            .collect { keyboard?.hide() }
    }
}

/** Empty and Content share a key: going from one to the other is a data change, not a stage change. */
private fun CourseDetailContent.stageKey(): Any = when (this) {
    CourseDetailContent.Empty, is CourseDetailContent.Content -> CourseDetailContent.Content::class
    else -> this::class
}

@Composable
private fun CourseDetailContent(
    state: CourseDetailUiState,
    courseTitle: CourseTitle,
    lazyListState: LazyListState,
    innerPadding: PaddingValues,
    onAction: (CourseDetailAction) -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val isNavTransitionRunning = rememberNavTransitionRunning()
    AnimatedContent(
        targetState = state.content,
        contentKey = { it.stageKey() },
        transitionSpec = {
            if (isNavTransitionRunning()) {
                // Contents landing mid navigation swap in at once: one animation at a time.
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)) togetherWith
                    fadeOut(WeBeepMotion.exit(reduceMotion = reduceMotion))
            }
        },
        label = "course_state",
        modifier = Modifier.fillMaxSize()
    ) { content ->
        when (content) {
            CourseDetailContent.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                CourseSectionsSkeleton(
                    modifier = Modifier
                        .padding(top = innerPadding.calculateTopPadding())
                        .widthIn(max = MaxContentWidth)
                )
            }
            CourseDetailContent.Empty -> EmptyStateView(
                icon = Icons.Rounded.Book,
                title = stringResource(R.string.course_empty_title),
                subtitle = stringResource(R.string.course_empty_subtitle),
                actionLabel = stringResource(R.string.refresh),
                onAction = { onAction(CourseDetailAction.Retry) }
            )
            is CourseDetailContent.Content -> CourseContentList(
                content = content.list,
                courseTitle = courseTitle,
                searchQuery = state.searchQuery,
                lazyListState = lazyListState,
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + WeBeepSpacing.xs,
                    bottom = innerPadding.calculateBottomPadding() + WeBeepSpacing.lg
                ),
                onAction = onAction
            )
            is CourseDetailContent.Error -> EmptyStateView(
                icon = Icons.Rounded.Description,
                title = stringResource(R.string.sync_error_title),
                subtitle = stringResource(content.messageRes),
                actionLabel = stringResource(R.string.retry),
                onAction = { onAction(CourseDetailAction.Retry) }
            )
        }
    }
}

@Composable
private fun PreviewScreen(state: CourseDetailUiState) {
    WeBeepTheme {
        CourseDetailScreen(state = state, onAction = {}, onBackClick = {})
    }
}

@Preview(name = "Loading")
@Composable
private fun CourseDetailScreenPreviewLoading() {
    PreviewScreen(previewCourseState(CourseDetailContent.Loading))
}

@Preview(name = "Content with expanded folders")
@Composable
private fun CourseDetailScreenPreviewContent() {
    val folders = previewExpandedFolders
    PreviewScreen(previewCourseState(CourseDetailContent.Content(previewList(folders = folders)), folders = folders))
}

@Preview(name = "Search without results")
@Composable
private fun CourseDetailScreenPreviewNoResults() {
    val query = "thermodynamics"
    PreviewScreen(previewCourseState(CourseDetailContent.Content(previewList(query = query)), query = query))
}

@Preview(name = "Empty course")
@Composable
private fun CourseDetailScreenPreviewEmpty() {
    PreviewScreen(previewCourseState(CourseDetailContent.Empty))
}

@Preview(name = "Error")
@Composable
private fun CourseDetailScreenPreviewError() {
    PreviewScreen(previewCourseState(CourseDetailContent.Error(R.string.error_data_unknown)))
}

@Preview(name = "Offline")
@Composable
private fun CourseDetailScreenPreviewOffline() {
    // Offline with nothing cached: the network error is the only thing to show.
    PreviewScreen(previewCourseState(CourseDetailContent.Error(R.string.error_data_network)))
}
