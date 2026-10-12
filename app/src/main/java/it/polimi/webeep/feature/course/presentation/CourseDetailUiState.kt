package it.polimi.webeep.feature.course.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import it.polimi.webeep.core.common.CourseTitle

/**
 * Everything the course screen draws. [searchQuery] and [expandedFolders] live here (and in the
 * saved state) so they survive process death; [content] already holds the filtered, flattened rows.
 */
@Immutable
data class CourseDetailUiState(
    val courseId: Int,
    /** Null until the course name is known; the screen then shows a localized placeholder. */
    val courseTitle: CourseTitle? = null,
    val content: CourseDetailContent = CourseDetailContent.Loading,
    val searchQuery: String = "",
    val expandedFolders: Set<String> = emptySet()
)

/** The mutually exclusive stages of the course contents. */
@Immutable
sealed interface CourseDetailContent {
    data object Loading : CourseDetailContent

    /** The course has no sections at all (as opposed to a search hiding them). */
    data object Empty : CourseDetailContent

    /** Rows are built off the main thread before this state exists, so the list never shows up empty. */
    data class Content(val list: CourseListContent) : CourseDetailContent

    data class Error(@StringRes val messageRes: Int) : CourseDetailContent
}
