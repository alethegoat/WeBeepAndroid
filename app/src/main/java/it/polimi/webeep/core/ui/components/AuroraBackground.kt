package it.polimi.webeep.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion

/**
 * Animated aurora/glow background that adapts to light/dark mode.
 * Animation runs only while the host lifecycle is RESUMED.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    content: @Composable () -> Unit
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val reduceMotion = LocalReduceMotion.current
    val animateAurora = !reduceMotion && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val motion = auroraMotion(animateAurora)

    val backgroundColor = MaterialTheme.colorScheme.background
    val colors = WeBeepTheme.colors

    val primary = colors.auroraPrimary.copy(alpha = if (isDark) 0.38f else 0.42f)
    val secondary = colors.auroraSecondary.copy(alpha = if (isDark) 0.22f else 0.28f)
    val tertiary = colors.auroraTertiary.copy(alpha = if (isDark) 0.14f else 0.22f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .drawBehind {
                fun glow(color: Color, center: Offset, radius: Float) {
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
                glow(primary, Offset(motion.x1, motion.y1), size.minDimension * 0.95f * motion.pulse)
                glow(secondary, Offset(motion.x2, motion.y2), size.minDimension * 0.8f * (1.08f - (motion.pulse - 1f)))
                glow(tertiary, Offset(motion.x3, motion.y3), size.minDimension * 0.7f * motion.pulse)
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private data class AuroraMotion(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val x3: Float,
    val y3: Float,
    val pulse: Float
)

@Composable
private fun auroraMotion(animate: Boolean): AuroraMotion {
    if (!animate) {
        return AuroraMotion(
            x1 = 280f,
            y1 = 220f,
            x2 = 860f,
            y2 = 980f,
            x3 = 520f,
            y3 = 720f,
            pulse = 1f
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aurora_anim")
    val spec = { duration: Int ->
        infiniteRepeatable(
            animation = tween<Float>(duration, easing = WeBeepMotion.SoftInOut),
            repeatMode = RepeatMode.Reverse
        )
    }
    val glowX1 by infiniteTransition.animateFloat(
        initialValue = -180f,
        targetValue = 980f,
        animationSpec = spec(18000),
        label = "aurora_x1"
    )
    val glowY1 by infiniteTransition.animateFloat(
        initialValue = -120f,
        targetValue = 860f,
        animationSpec = spec(24000),
        label = "aurora_y1"
    )
    val glowX2 by infiniteTransition.animateFloat(
        initialValue = 1180f,
        targetValue = 80f,
        animationSpec = spec(21000),
        label = "aurora_x2"
    )
    val glowY2 by infiniteTransition.animateFloat(
        initialValue = 420f,
        targetValue = 1180f,
        animationSpec = spec(16000),
        label = "aurora_y2"
    )
    val glowX3 by infiniteTransition.animateFloat(
        initialValue = 240f,
        targetValue = 920f,
        animationSpec = spec(26000),
        label = "aurora_x3"
    )
    val glowY3 by infiniteTransition.animateFloat(
        initialValue = 1400f,
        targetValue = 80f,
        animationSpec = spec(28000),
        label = "aurora_y3"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = spec(12000),
        label = "aurora_pulse"
    )

    return AuroraMotion(glowX1, glowY1, glowX2, glowY2, glowX3, glowY3, pulseScale)
}
