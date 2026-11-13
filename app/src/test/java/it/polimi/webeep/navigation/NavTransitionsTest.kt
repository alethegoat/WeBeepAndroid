package it.polimi.webeep.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavTransitionsTest {

    private lateinit var login: NavDestination
    private lateinit var dashboard: NavDestination
    private lateinit var course: NavDestination
    private lateinit var settings: NavDestination
    private lateinit var viewer: NavDestination

    @Before
    fun setup() {
        val graph = NavTestSupport.newController(Login).graph
        login = graph.findNode(Login::class)!!
        dashboard = graph.findNode(Dashboard::class)!!
        course = graph.findNode(CourseDetail::class)!!
        settings = graph.findNode(Settings::class)!!
        viewer = graph.findNode(FileViewer::class)!!
    }

    private val motion = NavTransitions(reduceMotion = false)
    private val reduced = NavTransitions(reduceMotion = true)

    // The transition classes are opaque, so the kind of animation is read from their description.
    private fun kinds(text: String) = Triple("Fade - null" !in text, "Slide - null" !in text, "Scale - null" !in text)

    @Test
    fun `reduced motion disables every transition`() {
        for (from in listOf(login, dashboard, course, settings, viewer)) {
            for (to in listOf(login, dashboard, course, settings, viewer)) {
                for (pop in listOf(false, true)) {
                    assertEquals(EnterTransition.None, reduced.enter(from, to, pop))
                    assertEquals(ExitTransition.None, reduced.exit(from, to, pop))
                }
            }
        }
    }

    @Test
    fun `login and dashboard fade through in both directions`() {
        for ((from, to) in listOf(login to dashboard, dashboard to login)) {
            val (fade, slide, scale) = kinds(motion.enter(from, to, pop = false).toString())
            assertTrue(fade && !slide && scale)
            val (exitFade, exitSlide, _) = kinds(motion.exit(from, to, pop = false).toString())
            assertTrue(exitFade && !exitSlide)
        }
    }

    @Test
    fun `dashboard and course detail only fade`() {
        for ((from, to) in listOf(dashboard to course, course to dashboard)) {
            for (pop in listOf(false, true)) {
                val (fade, slide, scale) = kinds(motion.enter(from, to, pop).toString())
                assertTrue(fade && !slide && !scale)
                val (exitFade, exitSlide, exitScale) = kinds(motion.exit(from, to, pop).toString())
                assertTrue(exitFade && !exitSlide && !exitScale)
            }
        }
    }

    @Test
    fun `other destination pairs slide`() {
        val pairs = listOf(dashboard to settings, course to viewer, settings to dashboard, login to settings)
        for ((from, to) in pairs) {
            for (pop in listOf(false, true)) {
                val (_, enterSlide, _) = kinds(motion.enter(from, to, pop).toString())
                val (_, exitSlide, _) = kinds(motion.exit(from, to, pop).toString())
                assertTrue("$from -> $to pop=$pop", enterSlide && exitSlide)
            }
        }
    }

    @Test
    fun `fade patterns do not depend on the pop flag but the pattern differs per pair`() {
        assertEquals(
            motion.enter(login, dashboard, pop = false).toString(),
            motion.enter(login, dashboard, pop = true).toString()
        )
        assertNotEquals(
            motion.enter(login, dashboard, pop = false).toString(),
            motion.enter(dashboard, course, pop = false).toString()
        )
    }
}
