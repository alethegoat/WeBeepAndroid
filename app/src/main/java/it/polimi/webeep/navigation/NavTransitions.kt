package it.polimi.webeep.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import it.polimi.webeep.core.ui.theme.WeBeepMotion

/**
 * Picks the Material 3 transition pattern for a route pair:
 * - Login <-> Dashboard: fade-through (unrelated top-level destinations)
 * - Dashboard <-> CourseDetail: plain fade; the card container transform carries the motion
 * - everything else: shared-axis X, direction follows push/pop
 */
internal class NavTransitions(
    private val sharedAxisOffsetPx: Int,
    private val reduceMotion: Boolean
) {
    private enum class Pattern { FadeThrough, ContainerTransform, SharedAxisX }

    fun enter(from: String?, to: String?, pop: Boolean): EnterTransition {
        if (reduceMotion) return fadeIn(tween(0))
        return when (patternFor(from, to)) {
            Pattern.FadeThrough -> fadeIn(incomingSpec()) +
                scaleIn(incomingSpec(), initialScale = 0.92f)
            // Same length as the shared-bounds animation so it is not cut short.
            Pattern.ContainerTransform -> fadeIn(WeBeepMotion.enter(WeBeepMotion.LongMs))
            Pattern.SharedAxisX -> fadeIn(incomingSpec()) + slideInHorizontally(
                animationSpec = WeBeepMotion.enter(WeBeepMotion.TransitionMs)
            ) { if (pop) -sharedAxisOffsetPx else sharedAxisOffsetPx }
        }
    }

    fun exit(from: String?, to: String?, pop: Boolean): ExitTransition {
        if (reduceMotion) return fadeOut(tween(0))
        return when (patternFor(from, to)) {
            Pattern.FadeThrough -> fadeOut(outgoingSpec())
            Pattern.ContainerTransform -> fadeOut(WeBeepMotion.exit(WeBeepMotion.LongMs))
            Pattern.SharedAxisX -> fadeOut(outgoingSpec()) + slideOutHorizontally(
                animationSpec = WeBeepMotion.enter(WeBeepMotion.TransitionMs)
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

    private fun <T> incomingSpec() = tween<T>(
        durationMillis = WeBeepMotion.TransitionInMs,
        delayMillis = WeBeepMotion.TransitionOutMs,
        easing = WeBeepMotion.EmphasizedDecelerate
    )

    private fun <T> outgoingSpec() = tween<T>(
        durationMillis = WeBeepMotion.TransitionOutMs,
        easing = WeBeepMotion.EmphasizedAccelerate
    )
}
