package it.polimi.webeep.navigation

import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationActionsTest {

    private fun TestNavHostController.stack() = currentBackStack.value
        .filter { it.destination.route != null && it.destination !is androidx.navigation.NavGraph }

    @Test
    fun `navigateToLogin clears the whole back stack`() {
        val controller = NavTestSupport.newController(Dashboard)
        controller.navigate(CourseDetail(3))
        controller.navigate(FileViewer("https://x/f.pdf", "f"))

        controller.navigateToLogin()

        assertTrue(controller.currentDestination!!.hasRoute<Login>())
        assertEquals(1, controller.stack().size)
        assertFalse(controller.popBackStack())
    }

    @Test
    fun `navigateToLogin is a no-op when login is already shown`() {
        val controller = NavTestSupport.newController(Login)
        val before = controller.currentBackStackEntry

        controller.navigateToLogin()

        assertTrue(controller.currentDestination!!.hasRoute<Login>())
        assertEquals(before, controller.currentBackStackEntry)
        assertEquals(1, controller.stack().size)
    }

    @Test
    fun `navigateToDashboard replaces login so back never returns to it`() {
        val controller = NavTestSupport.newController(Login)

        controller.navigateToDashboard()

        assertTrue(controller.currentDestination!!.hasRoute<Dashboard>())
        assertEquals(1, controller.stack().size)
        assertFalse(controller.stack().any { it.destination.hasRoute<Login>() })
    }

    @Test
    fun `course detail keeps its id argument and back returns to the dashboard`() {
        val controller = NavTestSupport.newController(Dashboard)

        controller.navigate(CourseDetail(42))

        assertEquals(42, controller.currentBackStackEntry!!.toRoute<CourseDetail>().courseId)
        controller.navigateUp()
        assertTrue(controller.currentDestination!!.hasRoute<Dashboard>())
    }
}
