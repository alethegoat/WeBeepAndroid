package it.polimi.webeep.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// SavedStateHandle.toRoute builds a Bundle, which needs Robolectric.
@RunWith(RobolectricTestRunner::class)
class RoutesTest {

    @Test
    fun `course detail reads its id from the saved state`() {
        val route = SavedStateHandle(mapOf("courseId" to 5)).toRoute<CourseDetail>()

        assertEquals(CourseDetail(5), route)
    }

    @Test
    fun `file viewer optional arguments default to null`() {
        val route = SavedStateHandle(mapOf("url" to "https://x/y.pdf", "title" to "y.pdf")).toRoute<FileViewer>()

        assertEquals(FileViewer("https://x/y.pdf", "y.pdf"), route)
        assertNull(route.mimeType)
        assertNull(route.courseId)
    }

    @Test
    fun `file viewer reads every argument`() {
        val handle = SavedStateHandle(
            mapOf("url" to "https://x/y.pdf", "title" to "y.pdf", "mimeType" to "application/pdf", "courseId" to 7)
        )

        assertEquals(FileViewer("https://x/y.pdf", "y.pdf", "application/pdf", 7), handle.toRoute<FileViewer>())
    }
}
