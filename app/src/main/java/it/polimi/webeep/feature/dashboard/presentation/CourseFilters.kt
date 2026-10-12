package it.polimi.webeep.feature.dashboard.presentation

enum class CourseFilter {
    ACTIVE,
    PAST,
    ALL
}

/** The filter shown when nothing has been chosen yet. */
internal val DEFAULT_COURSE_FILTER = CourseFilter.ACTIVE

internal fun filterCourses(courses: List<CourseUiModel>, query: String, filter: CourseFilter, nowEpochSeconds: Long): List<CourseUiModel> {
    val needle = query.trim()
    return courses.filter { course ->
        val matchesSearch = needle.isEmpty() ||
            course.fullname.contains(needle, ignoreCase = true) ||
            course.shortname.contains(needle, ignoreCase = true)
        val matchesFilter = when (filter) {
            CourseFilter.ALL -> true
            CourseFilter.ACTIVE -> course.endDate == 0L || course.endDate >= nowEpochSeconds
            CourseFilter.PAST -> course.endDate != 0L && course.endDate < nowEpochSeconds
        }
        matchesSearch && matchesFilter
    }
}
