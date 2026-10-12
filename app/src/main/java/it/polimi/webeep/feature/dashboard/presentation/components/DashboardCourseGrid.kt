package it.polimi.webeep.feature.dashboard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.staggeredEntrance
import it.polimi.webeep.feature.dashboard.presentation.CourseUiModel
import it.polimi.webeep.feature.dashboard.presentation.DashboardAction
import it.polimi.webeep.feature.dashboard.presentation.DashboardContent
import it.polimi.webeep.feature.dashboard.presentation.DashboardUiState
import it.polimi.webeep.feature.dashboard.presentation.components.CourseCard
import kotlinx.coroutines.flow.filter

/** Minimum card width: one column on phones, more on tablets / landscape. */
private val CourseGridMinCellWidth = 320.dp

/** Cards that take part in the first-load entrance (about one screenful). */
const val ENTRANCE_ITEM_COUNT = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCourseGrid(
    state: DashboardUiState,
    content: DashboardContent.Success,
    gridState: LazyGridState,
    contentPadding: PaddingValues,
    onAction: (DashboardAction) -> Unit,
    onCourseClick: (courseId: Int) -> Unit,
    playEntrance: Boolean,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onAction(DashboardAction.Refresh) },
        modifier = modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = CourseGridMinCellWidth),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + WeBeepSpacing.xs,
                bottom = contentPadding.calculateBottomPadding() + WeBeepSpacing.md,
                start = WeBeepSpacing.md,
                end = WeBeepSpacing.md
            ),
            horizontalArrangement = Arrangement.spacedBy(WeBeepSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(WeBeepSpacing.sm),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                DashboardListHeader(
                    isOnline = state.isOnline,
                    isCacheAvailable = content.courses.isNotEmpty(),
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = { onAction(DashboardAction.SearchQueryChange(it)) },
                    courseFilter = state.filter,
                    onFilterChange = { onAction(DashboardAction.FilterChange(it)) }
                )
            }
            if (content.visibleCourses.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                    NoCoursesItem(filtersActive = state.filtersActive, onAction = onAction)
                }
            } else {
                courseCards(content.visibleCourses, playEntrance, onCourseClick)
            }
        }
    }
}

private fun LazyGridScope.courseCards(courses: List<CourseUiModel>, playEntrance: Boolean, onCourseClick: (courseId: Int) -> Unit) {
    itemsIndexed(
        items = courses,
        key = { _, course -> course.id },
        contentType = { _, _ -> "course_card" }
    ) { index, course ->
        val reduceMotion = LocalReduceMotion.current
        CourseCard(
            course = course,
            onClick = { onCourseClick(course.id) },
            modifier = Modifier
                .animateItem(
                    fadeInSpec = WeBeepMotion.enter(reduceMotion = reduceMotion),
                    fadeOutSpec = WeBeepMotion.exit(reduceMotion = reduceMotion),
                    placementSpec = WeBeepMotion.softSpring(reduceMotion)
                )
                .then(
                    if (playEntrance && index < ENTRANCE_ITEM_COUNT) {
                        Modifier.staggeredEntrance(index)
                    } else {
                        Modifier
                    }
                )
        )
    }
}

/** Offers a way out: reset the filters if they hide everything, otherwise refresh. */
@Composable
private fun NoCoursesItem(filtersActive: Boolean, onAction: (DashboardAction) -> Unit, modifier: Modifier = Modifier) {
    EmptyStateView(
        icon = Icons.Rounded.Book,
        title = stringResource(R.string.no_courses_found),
        subtitle = stringResource(
            if (filtersActive) R.string.try_changing_filters else R.string.no_courses_enrolled
        ),
        actionLabel = stringResource(
            if (filtersActive) R.string.reset_filters else R.string.refresh
        ),
        onAction = {
            onAction(if (filtersActive) DashboardAction.ResetFilters else DashboardAction.Refresh)
        },
        modifier = modifier
    )
}
