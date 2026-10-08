package it.polimi.webeep.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion

private const val SHIMMER_GRADIENT_SPAN = 500f

/** Number of section placeholders shown while a course loads. */
private const val SectionPlaceholderCount = 5

/** Rows inside the first (expanded) placeholder section. */
private const val RowPlaceholderCount = 3

/**
 * Everything a placeholder needs to draw. One instance is shared by a whole list, so
 * a single animation drives every block; [progress] is read only while drawing, so
 * the shimmer redraws but never recomposes.
 */
@Stable
class ShimmerStyle internal constructor(
    val progress: State<Float>,
    val baseColor: Color,
    val highlightColor: Color,
    val animated: Boolean,
)

@Composable
fun rememberShimmerStyle(): ShimmerStyle {
    val animated = !LocalReduceMotion.current
    val progress = if (animated) {
        rememberInfiniteTransition(label = "shimmer").animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmer_translate",
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
                end = Offset(translate, translate),
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
    style: ShimmerStyle = rememberShimmerStyle(),
) {
    Box(
        modifier = modifier
            .then(
                if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
            )
            .height(height)
            .shimmerPlaceholder(style, shape),
    )
}

@Composable
fun ShimmerCourseCard(
    modifier: Modifier = Modifier,
    style: ShimmerStyle = rememberShimmerStyle(),
) {
    val small = MaterialTheme.shapes.extraSmall
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = groupedListContainerColor(),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WeBeepSpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shimmerPlaceholder(style, MaterialTheme.shapes.medium),
            )
            Spacer(modifier = Modifier.width(WeBeepSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(12.dp)
                        .shimmerPlaceholder(style, small),
                )
                Spacer(modifier = Modifier.height(WeBeepSpacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(18.dp)
                        .shimmerPlaceholder(style, small),
                )
                Spacer(modifier = Modifier.height(WeBeepSpacing.xxs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(18.dp)
                        .shimmerPlaceholder(style, small),
                )
                Spacer(modifier = Modifier.height(WeBeepSpacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(12.dp)
                        .shimmerPlaceholder(style, small),
                )
            }
        }
    }
}

@Composable
fun ShimmerCourseList(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
    columns: Int = 1,
) {
    val style = rememberShimmerStyle()
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(WeBeepSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WeBeepSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(WeBeepSpacing.sm),
        userScrollEnabled = false,
    ) {
        items(itemCount) {
            ShimmerCourseCard(style = style)
        }
    }
}

/**
 * Course-content placeholder shaped like the real list: the search field, then
 * grouped sections, the first one expanded with a few file rows.
 */
@Composable
fun ShimmerCourseSections(
    modifier: Modifier = Modifier,
) {
    val style = rememberShimmerStyle()
    val small = MaterialTheme.shapes.extraSmall
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WeBeepSpacing.md),
        verticalArrangement = Arrangement.spacedBy(GroupedListGap),
    ) {
        Box(
            modifier = Modifier
                .padding(top = WeBeepSpacing.xs)
                .fillMaxWidth()
                .height(WeBeepSpacing.searchHeight)
                .shimmerPlaceholder(style, CircleShape),
        )
        repeat(SectionPlaceholderCount) { section ->
            val count = 1 + if (section == 0) RowPlaceholderCount else 0
            GroupedListItem(
                position = groupPosition(index = 0, count = count),
                modifier = Modifier.padding(top = GroupedListSectionGap),
            ) {
                Box(
                    modifier = Modifier
                        .padding(WeBeepSpacing.md)
                        .fillMaxWidth(if (section % 2 == 0) 0.55f else 0.7f)
                        .height(18.dp)
                        .shimmerPlaceholder(style, small),
                )
            }
            repeat(count - 1) { row ->
                GroupedListItem(position = groupPosition(index = row + 1, count = count)) {
                    Row(
                        modifier = Modifier.padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shimmerPlaceholder(style, MaterialTheme.shapes.small),
                        )
                        Spacer(modifier = Modifier.width(WeBeepSpacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(14.dp)
                                    .shimmerPlaceholder(style, small),
                            )
                            Spacer(modifier = Modifier.height(WeBeepSpacing.xs))
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(10.dp)
                                    .shimmerPlaceholder(style, small),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── previews ─────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Shimmer Card – Light")
@Composable
private fun ShimmerCourseCardLightPreview() {
    WeBeepTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
            ShimmerCourseCard()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117, name = "Shimmer Card – Dark")
@Composable
private fun ShimmerCourseCardDarkPreview() {
    WeBeepTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
            ShimmerCourseCard()
        }
    }
}

@Preview(showBackground = true, name = "Shimmer List – Light")
@Composable
private fun ShimmerCourseListPreview() {
    WeBeepTheme(darkTheme = false) {
        ShimmerCourseList(itemCount = 4)
    }
}

@Preview(showBackground = true, name = "Shimmer Sections – Light")
@Composable
private fun ShimmerCourseSectionsPreview() {
    WeBeepTheme(darkTheme = false) {
        ShimmerCourseSections()
    }
}
