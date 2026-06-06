package it.polimi.webeep.core.ui.util

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * a modifier that provides tactile feedback via a slight elastic compression (scale-down)
 * when the element is touched.
 * it automatically includes a debounce mechanism to prevent double clicks and multitouch bugs
 * by ignoring subsequent clicks that occur within [debouncems] milliseconds.
 */
fun Modifier.bouncyClickable(
    debounceMs: Long = 250L,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    var lastClickTime by remember { mutableStateOf(0L) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    coroutineScope.launch {
                        scale.animateTo(
                            targetValue = 0.96f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        )
                    }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    coroutineScope.launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        )
                    }
                }
            }
        }
    }

    this
        .graphicsLayer {
            // deferred state read: only invalidates the draw phase
            val currentScale = scale.value
            scaleX = currentScale
            scaleY = currentScale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // removes the classic gray ripple for a cleaner look
            enabled = enabled,
            onClick = {
                val now = SystemClock.uptimeMillis()
                if (now - lastClickTime >= debounceMs) {
                    lastClickTime = now
                    onClick()
                }
            }
        )
}

