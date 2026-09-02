package it.polimi.webeep.feature.dashboard

import it.polimi.webeep.core.ui.components.CourseUiModel
import it.polimi.webeep.feature.dashboard.presentation.CourseFilter
import it.polimi.webeep.feature.dashboard.presentation.filterCourses
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseFiltersTest {

    private val now = 1_700_000_000L
    private val courses = listOf(
        course(1, "Informatica", "INFO-1", endDate = 0L),
        course(2, "Analisi Matematica", "MAT-1", endDate = now + 10_000),
        course(3, "Fisica Sperimentale", "FIS-1", endDate = now - 10_000),
    )

    @Test
    fun activeFilterKeepsOpenAndUndatedCourses() {
        val result = filterCourses(courses, query = "", filter = CourseFilter.Active, nowEpochSeconds = now)
        assertEquals(listOf(1, 2), result.map { it.id })
    }

    @Test
    fun pastFilterKeepsExpiredCourses() {
        val result = filterCourses(courses, query = "", filter = CourseFilter.Past, nowEpochSeconds = now)
        assertEquals(listOf(3), result.map { it.id })
    }

    @Test
    fun searchMatchesFullnameAndShortname() {
        val byName = filterCourses(courses, "analisi", CourseFilter.All, now)
        assertEquals(listOf(2), byName.map { it.id })
        val byCode = filterCourses(courses, "fis-1", CourseFilter.All, now)
        assertEquals(listOf(3), byCode.map { it.id })
    }

    @Test
    fun blankQueryDoesNotFilterByText() {
        val result = filterCourses(courses, "   ", CourseFilter.All, now)
        assertEquals(3, result.size)
    }

    private fun course(
        id: Int,
        fullname: String,
        shortname: String,
        endDate: Long,
    ) = CourseUiModel(
        id = id,
        shortname = shortname,
        fullname = fullname,
        endDate = endDate,
    )
}
