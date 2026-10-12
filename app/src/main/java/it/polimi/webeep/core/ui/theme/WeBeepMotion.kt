package it.polimi.webeep.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Shared motion tokens. Prefer decelerate + no overshoot so UI feels smooth,
 * not bouncy.
 */
object WeBeepMotion {
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val SoftInOut = CubicBezierEasing(0.45f, 0.05f, 0.55f, 0.95f)

    const val SHORT_MS = 180
    const val MEDIUM_MS = 280
    const val LONG_MS = 420

    /** Material 3 fade-through: outgoing fades first, incoming follows. */
    const val TRANSITION_MS = 300
    const val TRANSITION_OUT_MS = 90
    const val TRANSITION_IN_MS = TRANSITION_MS - TRANSITION_OUT_MS

    /** Detail screens sliding in and out (file viewer, settings). */
    const val SLIDE_MS = 350

    /** Card -> course container transform; navigation fades last exactly as long. */
    const val CONTAINER_TRANSFORM_MS = 350

    /** [ms], or 0 when [reduceMotion] is on. */
    fun duration(ms: Int, reduceMotion: Boolean): Int = if (reduceMotion) 0 else ms

    /** Tween for things appearing (decelerating); instant with [reduceMotion]. */
    fun <T> enter(durationMs: Int = MEDIUM_MS, reduceMotion: Boolean = false) =
        tween<T>(durationMillis = duration(durationMs, reduceMotion), easing = EmphasizedDecelerate)

    /** Tween for things leaving (accelerating); instant with [reduceMotion]. */
    fun <T> exit(durationMs: Int = SHORT_MS, reduceMotion: Boolean = false) =
        tween<T>(durationMillis = duration(durationMs, reduceMotion), easing = EmphasizedAccelerate)

    /** Plain standard-easing tween; instant with [reduceMotion]. */
    fun <T> smooth(durationMs: Int = MEDIUM_MS, reduceMotion: Boolean = false) =
        tween<T>(durationMillis = duration(durationMs, reduceMotion), easing = Standard)

    /**
     * Critically damped spring for movement (slides, scales, bounds). Unlike a delayed
     * tween it stays smooth when interrupted, e.g. a quick back right after a push.
     */
    fun <T> spatialSpring(reduceMotion: Boolean = false) = if (reduceMotion) {
        tween<T>(durationMillis = 0)
    } else {
        spring<T>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    /** Slightly under-damped spring for small movements; instant with [reduceMotion]. */
    fun <T> softSpring(reduceMotion: Boolean = false) = if (reduceMotion) {
        tween<T>(durationMillis = 0)
    } else {
        spring(
            dampingRatio = 0.9f,
            stiffness = Spring.StiffnessMediumLow
        )
    }
}
