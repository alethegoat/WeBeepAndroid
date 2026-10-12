package it.polimi.webeep.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.util.LocalReduceMotion

private const val SHIMMER_GRADIENT_SPAN = 500f

/**
 * Everything a placeholder needs to draw. One instance is shared by a whole list, so
 * a single animation drives every block; [progress] is read only while drawing, so
 * the shimmer redraws but never recomposes.
 */
@Stable
class ShimmerStyle internal constructor(val progress: State<Float>, val baseColor: Color, val highlightColor: Color, val animated: Boolean)

@Composable
fun rememberShimmerStyle(): ShimmerStyle {
    val animated = !LocalReduceMotion.current
    val progress = if (animated) {
        rememberInfiniteTransition(label = "shimmer").animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_translate"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    return remember(progress, base, highlight, animated) {
        ShimmerStyle(progress, base, highlight, animated)
    }
}

/** Placeholder block clipped to [shape]: shimmering, or flat when reduce-motion is on. */
fun Modifier.shimmerPlaceholder(style: ShimmerStyle, shape: Shape): Modifier {
    val clipped = clip(shape)
    if (!style.animated) return clipped.background(style.baseColor)
    return clipped.drawBehind {
        val translate = style.progress.value
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(style.baseColor, style.highlightColor, style.baseColor),
                start = Offset(translate - SHIMMER_GRADIENT_SPAN, translate - SHIMMER_GRADIENT_SPAN),
                end = Offset(translate, translate)
            )
        )
    }
}

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    shape: Shape = MaterialTheme.shapes.extraSmall,
    style: ShimmerStyle = rememberShimmerStyle()
) {
    Box(
        modifier = modifier
            .then(
                if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
            )
            .height(height)
            .shimmerPlaceholder(style, shape)
    )
}
