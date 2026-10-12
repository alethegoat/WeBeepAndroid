package it.polimi.webeep.feature.sync.domain

import it.polimi.webeep.feature.dashboard.domain.model.Course
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseSyncOrderTest {

    private val now = 1_000L

    @Test
    fun `ongoing courses come before ended ones, favourites first within each group`() {
        val endedFavourite = course(1, endDate = 500L, favourite = true)
        val ongoing = course(2, endDate = 2_000L)
        val noEndDate = course(3, endDate = 0L, favourite = true)
        val ended = course(4, endDate = 100L)

        val sorted = listOf(endedFavourite, ongoing, noEndDate, ended).sortedWith(CourseSyncOrder.currentFirst(now))

        assertEquals(listOf(3, 2, 1, 4), sorted.map { it.id })
    }

    private fun course(id: Int, endDate: Long, favourite: Boolean = false) = Course(
        id = id,
        shortname = "C$id",
        fullname = "Course $id",
        summary = null,
        courseImage = null,
        progress = null,
        isFavourite = favourite,
        category = null,
        startDate = 0L,
        endDate = endDate
    )
}
