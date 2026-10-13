package it.polimi.webeep.core.ui.util

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Delay between two consecutive items of a staggered entrance. */
const val STAGGER_STEP_MS = 35L

/**
 * Fades the content in and lifts it by [offset] once, when it is first attached,
 * after [index] stagger steps. Runs entirely in a placement layer (no
 * recomposition); apply it only while the entrance should play, e.g. on the first
 * load of a list and not when coming back to it.
 */
fun Modifier.staggeredEntrance(index: Int, offset: Dp = 16.dp): Modifier = this then StaggeredEntranceElement(index, offset)

private data class StaggeredEntranceElement(val index: Int, val offset: Dp) : ModifierNodeElement<StaggeredEntranceNode>() {
    override fun create() = StaggeredEntranceNode(index, offset)

    override fun update(node: StaggeredEntranceNode) {
        node.offset = offset
    }
}

private class StaggeredEntranceNode(private val index: Int, var offset: Dp) :
    Modifier.Node(),
    LayoutModifierNode {

    private val progress = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            delay(index * STAGGER_STEP_MS)
            progress.animateTo(1f, WeBeepMotion.enter(WeBeepMotion.MEDIUM_MS))
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val value = progress.value
                alpha = value
                translationY = (1f - value) * offset.toPx()
            }
        }
    }
}
