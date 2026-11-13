package it.polimi.webeep.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationCallbacksTest {

    @get:Rule
    val rule = createComposeRule()

    private class Owner(state: Lifecycle.State) : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).also { it.currentState = state }
        override val lifecycle: Lifecycle get() = registry
    }

    private fun capture(owner: Owner): (String) -> Unit {
        lateinit var callback: (String) -> Unit
        rule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                callback = dropUnlessResumedWith { received.add(it) }
            }
        }
        rule.waitForIdle()
        return callback
    }

    private val received = mutableListOf<String>()

    @Test
    fun `callback runs with its argument while the screen is resumed`() {
        val callback = capture(Owner(Lifecycle.State.RESUMED))

        callback("a")
        callback("b")

        assertEquals(listOf("a", "b"), received)
    }

    @Test
    fun `callback is dropped when the screen is not resumed`() {
        val callback = capture(Owner(Lifecycle.State.STARTED))

        callback("a")

        assertEquals(emptyList<String>(), received)
    }

    @Test
    fun `callback is dropped once the screen leaves the resumed state`() {
        val owner = Owner(Lifecycle.State.RESUMED)
        val callback = capture(owner)

        owner.registry.currentState = Lifecycle.State.STARTED
        callback("late tap")

        assertEquals(emptyList<String>(), received)
    }
}
