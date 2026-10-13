package it.polimi.webeep.feature.dashboard.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * Everything the dashboard draws. [searchQuery] and [filter] live here (and in the saved
 * state) so they survive process death; [content] already holds the filtered list.
 */
@Immutable
data class DashboardUiState(
    val content: DashboardContent = DashboardContent.Loading,
    /** Blank until the profile is loaded; the screen shows a localized placeholder. */
    val userName: String = "",
    val userAvatar: String = "",
    val searchQuery: String = "",
    val filter: CourseFilter = DEFAULT_COURSE_FILTER,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true
) {
    val filtersActive: Boolean get() = searchQuery.isNotEmpty() || filter != DEFAULT_COURSE_FILTER
}

/** The mutually exclusive stages of the course list. */
@Immutable
sealed interface DashboardContent {
    data object Loading : DashboardContent

    /** [courses] is the unfiltered list (cache availability); [visibleCourses] is what is shown. */
    data class Success(val courses: List<CourseUiModel>, val visibleCourses: List<CourseUiModel>) : DashboardContent

    data class Error(@StringRes val messageRes: Int) : DashboardContent
}
