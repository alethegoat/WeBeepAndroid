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

    const val ShortMs = 180
    const val MediumMs = 280
    const val LongMs = 420

    /** Material 3 shared-axis / fade-through: outgoing fades first, incoming follows. */
    const val TransitionMs = 300
    const val TransitionOutMs = 90
    const val TransitionInMs = TransitionMs - TransitionOutMs
    const val SharedAxisOffsetDp = 30

    /** Card -> course container transform; navigation fades last exactly as long. */
    const val ContainerTransformMs = 350

    /** Scale the leaving screen reaches at the end of a predictive back gesture. */
    const val PredictiveBackScale = 0.9f

    fun duration(ms: Int, reduceMotion: Boolean): Int = if (reduceMotion) 0 else ms

    fun <T> enter(durationMs: Int = MediumMs, reduceMotion: Boolean = false) =
        tween<T>(durationMillis = duration(durationMs, reduceMotion), easing = EmphasizedDecelerate)

    fun <T> exit(durationMs: Int = ShortMs, reduceMotion: Boolean = false) =
        tween<T>(durationMillis = duration(durationMs, reduceMotion), easing = EmphasizedAccelerate)

    fun <T> smooth(durationMs: Int = MediumMs, reduceMotion: Boolean = false) =
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

    fun <T> softSpring(reduceMotion: Boolean = false) = if (reduceMotion) {
        tween<T>(durationMillis = 0)
    } else {
        spring(
            dampingRatio = 0.9f,
            stiffness = Spring.StiffnessMediumLow
        )
    }
}
