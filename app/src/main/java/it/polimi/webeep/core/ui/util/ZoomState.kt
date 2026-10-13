package it.polimi.webeep.core.ui.util

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import it.polimi.webeep.core.ui.theme.WeBeepMotion

/**
 * Pinch / pan / double-tap zoom shared by the image and PDF viewers. Zoom is
 * anchored to the gesture centroid, so the point under the fingers stays put.
 */
@Stable
class ZoomState(private val maxScale: Float = 5f) {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    private var containerSize = IntSize.Zero

    val isZoomed: Boolean get() = scale > 1.01f

    internal fun onSizeChanged(size: IntSize) {
        containerSize = size
        offset = clamp(offset, scale)
    }

    /**
     * Applies one gesture step and returns the vertical pan that did not fit in
     * the zoomed viewport, so a scrolling container can take it.
     */
    fun applyGesture(centroid: Offset, zoom: Float, pan: Offset): Float {
        val newScale = (scale * zoom).coerceIn(1f, maxScale)
        val fromCenter = centroid - center()
        val desired = fromCenter - (fromCenter - offset) * (newScale / scale) + pan
        val clamped = clamp(desired, newScale)
        scale = newScale
        offset = clamped
        return desired.y - clamped.y
    }

    /** Double-tap behaviour: zoom into [focus] at 1x, back to 1x otherwise. */
    suspend fun toggleZoom(focus: Offset, spec: AnimationSpec<Float> = WeBeepMotion.smooth()) {
        val target = if (isZoomed) 1f else DOUBLE_TAP_SCALE
        animateTo(target, focus, spec)
    }

    /** Animates to [targetScale] (clamped to `1..maxScale`) keeping [focus] under the same screen point. */
    suspend fun animateTo(targetScale: Float, focus: Offset, spec: AnimationSpec<Float> = WeBeepMotion.smooth()) {
        val startScale = scale
        val startOffset = offset
        val fromCenter = focus - center()
        val endScale = targetScale.coerceIn(1f, maxScale)
        val endOffset = clamp(fromCenter - (fromCenter - startOffset) * (endScale / startScale), endScale)
        animate(0f, 1f, animationSpec = spec) { progress, _ ->
            scale = startScale + (endScale - startScale) * progress
            offset = startOffset + (endOffset - startOffset) * progress
        }
    }

    /** Back to 1x, centered, without animation. */
    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    private fun center() = Offset(containerSize.width / 2f, containerSize.height / 2f)

    private fun clamp(value: Offset, forScale: Float): Offset {
        val maxX = containerSize.width * (forScale - 1f) / 2f
        val maxY = containerSize.height * (forScale - 1f) / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    private companion object {
        const val DOUBLE_TAP_SCALE = 2.5f
    }
}

/** Remembers a [ZoomState] whose scale is capped at [maxScale]. */
@Composable
fun rememberZoomState(maxScale: Float = 5f): ZoomState = remember { ZoomState(maxScale) }

/**
 * Handles pinch and, while zoomed, one-finger pan. At 1x single-finger drags are
 * left alone so lists and pagers keep scrolling. Events are taken in the
 * Initial pass, before a scrolling child can react to a pinch.
 *
 * [onVerticalOverflow] receives the vertical pan beyond the zoomed viewport
 * (screen px, already divided by scale) and [onVerticalFling] the release
 * velocity; a PDF list uses them to keep scrolling pages while zoomed.
 */
fun Modifier.zoomable(
    state: ZoomState,
    onVerticalOverflow: ((Float) -> Unit)? = null,
    onVerticalFling: ((Float) -> Unit)? = null
): Modifier = this
    .onSizeChanged { state.onSizeChanged(it) }
    .pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val velocity = VelocityTracker()
            velocity.addPosition(down.uptimeMillis, down.position)
            var panning = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.count { it.pressed }
                if (pressed >= 2 || (pressed == 1 && state.isZoomed)) {
                    val overflow = state.applyGesture(
                        centroid = event.calculateCentroid(useCurrent = false),
                        zoom = event.calculateZoom(),
                        pan = event.calculatePan()
                    )
                    if (overflow != 0f) onVerticalOverflow?.invoke(overflow / state.scale)
                    event.changes.forEach { change ->
                        if (change.positionChanged()) change.consume()
                    }
                    panning = pressed == 1
                    event.changes.firstOrNull()?.let { velocity.addPosition(it.uptimeMillis, it.position) }
                } else {
                    panning = false
                }
            } while (event.changes.any { it.pressed })
            if (panning && state.isZoomed) {
                onVerticalFling?.invoke(velocity.calculateVelocity().y / state.scale)
            }
        }
    }
    .graphicsLayer {
        scaleX = state.scale
        scaleY = state.scale
        translationX = state.offset.x
        translationY = state.offset.y
    }
