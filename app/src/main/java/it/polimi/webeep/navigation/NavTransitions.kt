package it.polimi.webeep.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import it.polimi.webeep.core.ui.theme.WeBeepMotion

private const val FadeThroughInitialScale = 0.94f

/**
 * Picks the Material 3 transition pattern for a route pair:
 * - Login <-> Dashboard: fade-through (unrelated top-level destinations)
 * - Dashboard <-> CourseDetail: plain fade; the card container transform carries the motion
 * - everything else: shared-axis X, direction follows push/pop
 *
 * Movement uses springs and fades have no start delay, so a transition interrupted
 * by a quick back never stalls or jumps. The system back gesture plays the same pop
 * transition as the in-app back arrow, just driven by the finger.
 */
internal class NavTransitions(
    private val sharedAxisOffsetPx: Int,
    private val reduceMotion: Boolean
) {
    private enum class Pattern { FadeThrough, ContainerTransform, SharedAxisX }

    fun enter(from: String?, to: String?, pop: Boolean): EnterTransition {
        if (reduceMotion) return EnterTransition.None
        return when (patternFor(from, to)) {
            Pattern.FadeThrough -> fadeIn(incomingFade()) +
                scaleIn(WeBeepMotion.spatialSpring(), initialScale = FadeThroughInitialScale)
            // As long as the shared-bounds animation, or that would be cut short.
            Pattern.ContainerTransform -> fadeIn(WeBeepMotion.enter(WeBeepMotion.ContainerTransformMs))
            Pattern.SharedAxisX -> fadeIn(incomingFade()) + slideInHorizontally(
                animationSpec = WeBeepMotion.spatialSpring()
            ) { if (pop) -sharedAxisOffsetPx else sharedAxisOffsetPx }
        }
    }

    fun exit(from: String?, to: String?, pop: Boolean): ExitTransition {
        if (reduceMotion) return ExitTransition.None
        return when (patternFor(from, to)) {
            Pattern.FadeThrough -> fadeOut(outgoingFade())
            Pattern.ContainerTransform -> fadeOut(WeBeepMotion.exit(WeBeepMotion.ContainerTransformMs))
            Pattern.SharedAxisX -> fadeOut(outgoingFade()) + slideOutHorizontally(
                animationSpec = WeBeepMotion.spatialSpring()
            ) { if (pop) sharedAxisOffsetPx else -sharedAxisOffsetPx }
        }
    }

    private fun patternFor(from: String?, to: String?): Pattern {
        val pair = setOf(from, to)
        return when (pair) {
            setOf(Screen.Login.route, Screen.Dashboard.route) -> Pattern.FadeThrough
            setOf(Screen.Dashboard.route, Screen.CourseDetail.route) -> Pattern.ContainerTransform
            else -> Pattern.SharedAxisX
        }
    }

    private fun <T> incomingFade() = WeBeepMotion.enter<T>(WeBeepMotion.TransitionInMs)

    private fun <T> outgoingFade() = WeBeepMotion.exit<T>(WeBeepMotion.TransitionOutMs)
}
