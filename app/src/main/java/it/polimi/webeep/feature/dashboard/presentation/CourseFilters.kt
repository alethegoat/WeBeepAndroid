package it.polimi.webeep.feature.dashboard.presentation

enum class CourseFilter {
    Active,
    Past,
    All
}

internal fun filterCourses(courses: List<CourseUiModel>, query: String, filter: CourseFilter, nowEpochSeconds: Long): List<CourseUiModel> {
    val needle = query.trim()
    return courses.filter { course ->
        val matchesSearch = needle.isEmpty() ||
            course.fullname.contains(needle, ignoreCase = true) ||
            course.shortname.contains(needle, ignoreCase = true)
        val matchesFilter = when (filter) {
            CourseFilter.All -> true
            CourseFilter.Active -> course.endDate == 0L || course.endDate >= nowEpochSeconds
            CourseFilter.Past -> course.endDate != 0L && course.endDate < nowEpochSeconds
        }
        matchesSearch && matchesFilter
    }
}
