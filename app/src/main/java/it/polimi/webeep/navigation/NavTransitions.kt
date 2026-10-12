package it.polimi.webeep.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import it.polimi.webeep.core.ui.theme.WeBeepMotion

private const val FADE_THROUGH_INITIAL_SCALE = 0.94f

/** How far the screen underneath moves while a detail screen slides over it. */
private const val PARALLAX_DIVISOR = 4

/**
 * Picks the transition pattern for a route pair:
 * - Login <-> Dashboard: fade-through (unrelated top-level destinations)
 * - Dashboard <-> CourseDetail: plain fade; the card container transform carries the motion
 * - everything else (file viewer, settings): the detail screen slides in from the
 *   right over a slight parallax, and slides back out to the right
 *
 * Every pop transition is also what the system back gesture scrubs, so it must
 * move in proportion to the finger: a full-width slide does, a quick fade does not.
 * Back by gesture and back by the in-app arrow therefore look the same.
 */
internal class NavTransitions(private val reduceMotion: Boolean) {
    private enum class Pattern { FADE_THROUGH, CONTAINER_TRANSFORM, SLIDE }

    /** Enter animation for navigating from [from] to [to]; [pop] is true when going back. None with reduced motion. */
    fun enter(from: NavDestination, to: NavDestination, pop: Boolean): EnterTransition {
        if (reduceMotion) return EnterTransition.None
        return when (patternFor(from, to)) {
            Pattern.FADE_THROUGH -> fadeIn(WeBeepMotion.enter(WeBeepMotion.TRANSITION_IN_MS)) +
                scaleIn(WeBeepMotion.spatialSpring(), initialScale = FADE_THROUGH_INITIAL_SCALE)
            // As long as the shared-bounds animation, or that would be cut short.
            Pattern.CONTAINER_TRANSFORM -> fadeIn(WeBeepMotion.enter(WeBeepMotion.CONTAINER_TRANSFORM_MS))
            // Push: the detail arrives from the right edge. Pop: the screen below comes back from its parallax.
            Pattern.SLIDE -> slideInHorizontally(slideSpec()) { width ->
                if (pop) -width / PARALLAX_DIVISOR else width
            }
        }
    }

    /** Exit animation matching [enter]. None with reduced motion. */
    fun exit(from: NavDestination, to: NavDestination, pop: Boolean): ExitTransition {
        if (reduceMotion) return ExitTransition.None
        return when (patternFor(from, to)) {
            Pattern.FADE_THROUGH -> fadeOut(WeBeepMotion.exit(WeBeepMotion.TRANSITION_OUT_MS))
            Pattern.CONTAINER_TRANSFORM -> fadeOut(WeBeepMotion.exit(WeBeepMotion.CONTAINER_TRANSFORM_MS))
            // Push: the screen below drifts left. Pop: the detail leaves to the right, with the finger.
            Pattern.SLIDE -> slideOutHorizontally(slideSpec()) { width ->
                if (pop) width else -width / PARALLAX_DIVISOR
            }
        }
    }

    private fun patternFor(from: NavDestination, to: NavDestination) = when {
        from.isPair<Login, Dashboard>(to) -> Pattern.FADE_THROUGH
        from.isPair<Dashboard, CourseDetail>(to) -> Pattern.CONTAINER_TRANSFORM
        else -> Pattern.SLIDE
    }

    /**
     * Duration-based, not a spring: a gesture seeks the transition by its progress,
     * and a tween maps progress to position predictably, so the screen tracks the finger.
     */
    private fun slideSpec() = tween<IntOffset>(
        durationMillis = WeBeepMotion.SLIDE_MS,
        easing = WeBeepMotion.Standard
    )

    /** Whether this and [other] are the two destinations [A] and [B], in either order. */
    private inline fun <reified A : Any, reified B : Any> NavDestination.isPair(other: NavDestination) =
        (hasRoute<A>() && other.hasRoute<B>()) || (hasRoute<B>() && other.hasRoute<A>())
}
