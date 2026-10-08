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
import it.polimi.webeep.core.ui.theme.WeBeepMotion

private const val FadeThroughInitialScale = 0.94f

/** How far the screen underneath moves while a detail screen slides over it. */
private const val ParallaxDivisor = 4

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
internal class NavTransitions(
    private val reduceMotion: Boolean
) {
    private enum class Pattern { FadeThrough, ContainerTransform, Slide }

    fun enter(from: String?, to: String?, pop: Boolean): EnterTransition {
        if (reduceMotion) return EnterTransition.None
        return when (patternFor(from, to)) {
            Pattern.FadeThrough -> fadeIn(WeBeepMotion.enter(WeBeepMotion.TransitionInMs)) +
                scaleIn(WeBeepMotion.spatialSpring(), initialScale = FadeThroughInitialScale)
            // As long as the shared-bounds animation, or that would be cut short.
            Pattern.ContainerTransform -> fadeIn(WeBeepMotion.enter(WeBeepMotion.ContainerTransformMs))
            // Push: the detail arrives from the right edge. Pop: the screen below comes back from its parallax.
            Pattern.Slide -> slideInHorizontally(slideSpec()) { width ->
                if (pop) -width / ParallaxDivisor else width
            }
        }
    }

    fun exit(from: String?, to: String?, pop: Boolean): ExitTransition {
        if (reduceMotion) return ExitTransition.None
        return when (patternFor(from, to)) {
            Pattern.FadeThrough -> fadeOut(WeBeepMotion.exit(WeBeepMotion.TransitionOutMs))
            Pattern.ContainerTransform -> fadeOut(WeBeepMotion.exit(WeBeepMotion.ContainerTransformMs))
            // Push: the screen below drifts left. Pop: the detail leaves to the right, with the finger.
            Pattern.Slide -> slideOutHorizontally(slideSpec()) { width ->
                if (pop) width else -width / ParallaxDivisor
            }
        }
    }

    private fun patternFor(from: String?, to: String?): Pattern {
        val pair = setOf(from, to)
        return when (pair) {
            setOf(Screen.Login.route, Screen.Dashboard.route) -> Pattern.FadeThrough
            setOf(Screen.Dashboard.route, Screen.CourseDetail.route) -> Pattern.ContainerTransform
            else -> Pattern.Slide
        }
    }

    /**
     * Duration-based, not a spring: a gesture seeks the transition by its progress,
     * and a tween maps progress to position predictably, so the screen tracks the finger.
     */
    private fun slideSpec() = tween<IntOffset>(
        durationMillis = WeBeepMotion.SlideMs,
        easing = WeBeepMotion.Standard
    )
}
