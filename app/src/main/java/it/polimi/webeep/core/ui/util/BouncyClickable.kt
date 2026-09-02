package it.polimi.webeep.core.ui.util

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Clickable with immediate visual ack (ripple + optional scale), debounce, and
 * a light haptic on commit. Honors [LocalReduceMotion] by skipping scale.
 */
fun Modifier.bouncyClickable(
    debounceMs: Long = 250L,
    enabled: Boolean = true,
    haptic: Boolean = false,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val indication = LocalIndication.current

    if (!reduceMotion) {
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        coroutineScope.launch {
                            scale.animateTo(
                                targetValue = 0.98f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        coroutineScope.launch {
                            scale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    this
        .minimumInteractiveComponentSize()
        .graphicsLayer {
            if (!reduceMotion) {
                val currentScale = scale.value
                scaleX = currentScale
                scaleY = currentScale
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = {
                val now = SystemClock.uptimeMillis()
                if (now - lastClickTime >= debounceMs) {
                    lastClickTime = now
                    if (haptic) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    onClick()
                }
            }
        )
}
