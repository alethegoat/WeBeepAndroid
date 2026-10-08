package it.polimi.webeep.core.ui.util

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val PressedScale = 0.98f

/**
 * Clickable with immediate visual ack (ripple + a slight press scale), debounce, and
 * an optional light haptic on commit. Meant for cards and buttons; plain list rows
 * should use [clickable], which is cheaper. Honors [LocalReduceMotion] by skipping
 * the scale.
 */
fun Modifier.bouncyClickable(
    debounceMs: Long = 250L,
    enabled: Boolean = true,
    haptic: Boolean = false,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    // Plain holder, not state: the timestamp is never read during composition.
    val lastClickTime = remember { LongArray(1) }

    this
        .minimumInteractiveComponentSize()
        .then(PressScaleElement(interactionSource))
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = {
                val now = SystemClock.uptimeMillis()
                if (now - lastClickTime[0] >= debounceMs) {
                    lastClickTime[0] = now
                    if (haptic) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    onClick()
                }
            }
        )
}

private data class PressScaleElement(
    val interactionSource: InteractionSource
) : ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(interactionSource)

    override fun update(node: PressScaleNode) = node.update(interactionSource)
}

/**
 * Scales its content while pressed. The animation runs in the node's own scope and
 * is applied in a placement layer, so pressing never recomposes anything.
 */
private class PressScaleNode(
    private var interactionSource: InteractionSource
) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    private val scale = Animatable(1f)
    private var collectJob: Job? = null

    override fun onAttach() = collectPresses()

    fun update(source: InteractionSource) {
        if (source == interactionSource) return
        interactionSource = source
        collectJob?.cancel()
        collectPresses()
    }

    private fun collectPresses() {
        collectJob = coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                if (currentValueOf(LocalReduceMotion)) return@collectLatest
                when (interaction) {
                    is PressInteraction.Press -> scale.animateTo(
                        targetValue = PressedScale,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    is PressInteraction.Release, is PressInteraction.Cancel -> scale.animateTo(
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

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val current = scale.value
                scaleX = current
                scaleY = current
            }
        }
    }
}
