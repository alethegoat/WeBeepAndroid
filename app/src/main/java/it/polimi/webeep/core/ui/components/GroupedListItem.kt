package it.polimi.webeep.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.util.LocalReduceMotion

/** Space between rows of the same group. */
val GroupedListGap = 2.dp

/** Extra space above the first row of a group, on top of [GroupedListGap]. */
val GroupedListSectionGap = 10.dp

/** Corners on the outside of a group (matches MaterialTheme.shapes.large). */
private val OuterCorner = 24.dp

/** Corners between two rows of the same group. */
private val InnerCorner = 4.dp

/** Where a row sits inside its group; decides which corners are rounded. */
enum class GroupPosition { TOP, MIDDLE, BOTTOM, SINGLE }

/** Position of row [index] in a group of [count] rows. */
fun groupPosition(index: Int, count: Int): GroupPosition = when {
    count <= 1 -> GroupPosition.SINGLE
    index == 0 -> GroupPosition.TOP
    index == count - 1 -> GroupPosition.BOTTOM
    else -> GroupPosition.MIDDLE
}

/**
 * One row of a grouped list (Material 3 Expressive style): rows are separate
 * surfaces a few dp apart, rounded on the outside of the group and only slightly
 * on the inside. Each row being its own surface lets long lists stay lazy, one
 * item per row. Corners animate when a row changes position, e.g. a section
 * header that becomes the top of an expanded group.
 */
@Composable
fun GroupedListItem(
    position: GroupPosition,
    modifier: Modifier = Modifier,
    color: Color = groupedListContainerColor(),
    content: @Composable () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val roundTop = position == GroupPosition.TOP || position == GroupPosition.SINGLE
    val roundBottom = position == GroupPosition.BOTTOM || position == GroupPosition.SINGLE
    val top by animateDpAsState(
        targetValue = if (roundTop) OuterCorner else InnerCorner,
        animationSpec = WeBeepMotion.smooth(WeBeepMotion.SHORT_MS, reduceMotion),
        label = "group_top_corner"
    )
    val bottom by animateDpAsState(
        targetValue = if (roundBottom) OuterCorner else InnerCorner,
        animationSpec = WeBeepMotion.smooth(WeBeepMotion.SHORT_MS, reduceMotion),
        label = "group_bottom_corner"
    )
    Surface(
        shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
        color = color,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth(),
        content = content
    )
}

/**
 * Row color that stands out from the screen background: the lightest container in
 * light mode (white rows on a soft grey page), a raised one on the OLED-black page.
 * Derived from the color scheme, so it follows dynamic color too.
 */
@Composable
@ReadOnlyComposable
fun groupedListContainerColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() > 0.5f) scheme.surfaceContainerLowest else scheme.surfaceContainer
}
