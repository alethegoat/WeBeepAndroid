package it.polimi.webeep.core.ui.components

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion

/** Glow strength on screens where the aurora is only a calm backdrop. */
private const val StaticAuroraIntensity = 0.7f

/**
 * Opaque aurora/glow background that adapts to light/dark mode. Each screen that
 * wants it draws its own, so screens stay opaque during navigation transitions.
 *
 * With [animated] the glows drift slowly (login screen); otherwise they are drawn
 * once, dimmer, into a cached layer. The drifting positions are read in the draw
 * phase only, so the animation never recomposes. Motion also stops when
 * reduce-motion is on or the host lifecycle is not RESUMED.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AuroraLayer(animated = animated, isDark = isDark)
        content()
    }
}

@Composable
private fun AuroraLayer(
    animated: Boolean,
    isDark: Boolean
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val reduceMotion = LocalReduceMotion.current
    val animateAurora = animated && !reduceMotion && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val motion = if (animateAurora) animatedAuroraMotion() else StaticAuroraMotion

    val intensity = if (animated) 1f else StaticAuroraIntensity
    val colors = WeBeepTheme.colors
    val primary = colors.auroraPrimary.copy(alpha = (if (isDark) 0.38f else 0.42f) * intensity)
    val secondary = colors.auroraSecondary.copy(alpha = (if (isDark) 0.22f else 0.28f) * intensity)
    val tertiary = colors.auroraTertiary.copy(alpha = (if (isDark) 0.14f else 0.22f) * intensity)

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            // Own layer: the glows are recorded once and reused while screens animate above.
            .graphicsLayer()
            .drawBehind {
                val pulse = motion.pulse.value
                glow(primary, Offset(motion.x1.value, motion.y1.value), size.minDimension * 0.95f * pulse)
                glow(
                    secondary,
                    Offset(motion.x2.value, motion.y2.value),
                    size.minDimension * 0.8f * (1.08f - (pulse - 1f))
                )
                glow(tertiary, Offset(motion.x3.value, motion.y3.value), size.minDimension * 0.7f * pulse)
            }
    )
}

private fun DrawScope.glow(color: Color, center: Offset, radius: Float) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                color,
                color.copy(alpha = color.alpha * 0.45f),
                color.copy(alpha = color.alpha * 0.12f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        )
    )
}

/** Glow centres and pulse; read them only while drawing. */
private class AuroraMotion(
    val x1: State<Float>,
    val y1: State<Float>,
    val x2: State<Float>,
    val y2: State<Float>,
    val x3: State<Float>,
    val y3: State<Float>,
    val pulse: State<Float>
)

private val StaticAuroraMotion = AuroraMotion(
    x1 = mutableFloatStateOf(280f),
    y1 = mutableFloatStateOf(220f),
    x2 = mutableFloatStateOf(860f),
    y2 = mutableFloatStateOf(980f),
    x3 = mutableFloatStateOf(520f),
    y3 = mutableFloatStateOf(720f),
    pulse = mutableFloatStateOf(1f)
)

@Composable
private fun animatedAuroraMotion(): AuroraMotion {
    val transition = rememberInfiniteTransition(label = "aurora_anim")
    return AuroraMotion(
        x1 = transition.drift(-180f, 980f, 18000, "aurora_x1"),
        y1 = transition.drift(-120f, 860f, 24000, "aurora_y1"),
        x2 = transition.drift(1180f, 80f, 21000, "aurora_x2"),
        y2 = transition.drift(420f, 1180f, 16000, "aurora_y2"),
        x3 = transition.drift(240f, 920f, 26000, "aurora_x3"),
        y3 = transition.drift(1400f, 80f, 28000, "aurora_y3"),
        pulse = transition.drift(0.94f, 1.08f, 12000, "aurora_pulse")
    )
}

@Composable
private fun InfiniteTransition.drift(from: Float, to: Float, durationMs: Int, label: String): State<Float> =
    animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = WeBeepMotion.SoftInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = label
    )
