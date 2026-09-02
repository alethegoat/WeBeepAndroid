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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.theme.WeBeepTheme

import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import it.polimi.webeep.core.ui.util.LocalReduceMotion

/**
 * modifier that adds a performant animated shimmer effect.
 * avoids continuous recomposition by reading the animation only in the draw phase.
 */
fun Modifier.shimmerShader(
    translateValue: Float,
    baseColor: Color,
    highlightColor: Color,
): Modifier = this.drawWithContent {
    drawContent()
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateValue - 500f, translateValue - 500f),
        end = Offset(translateValue, translateValue),
    )
    drawRect(brush = brush)
}

/**
 * base composable for a shimmer element with configurable dimensions.
 *
 * @param modifier modifier to customize layout and style.
 * @param width element width. if `null`, fills available space.
 * @param height element height.
 * @param shape element shape (default: rounded rectangle 8dp).
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
) {
    val reduceMotion = LocalReduceMotion.current
    val translateAnim = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "shimmer")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmer_translate",
        ).value
    }

    Box(
        modifier = modifier
            .then(
                if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
            )
            .height(height)
            .clip(shape)
            .then(
                if (reduceMotion) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                } else {
                    Modifier.shimmerShader(
                        translateValue = translateAnim,
                        baseColor = MaterialTheme.colorScheme.surfaceVariant,
                        highlightColor = MaterialTheme.colorScheme.surface
                    )
                }
            ),
    )
}

/**
 * shimmer placeholder that mimics the shape of a [coursecard].
 *
 * shows a rounded rectangle with placeholder lines for title,
 * subtitle, category chip and progress indicator.
 */
@Composable
fun ShimmerCourseCard(
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val shimmerModifier = if (reduceMotion) {
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    } else {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnim = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmer_translate",
        )
        Modifier.shimmerShader(
            translateValue = translateAnim.value,
            baseColor = MaterialTheme.colorScheme.surfaceVariant,
            highlightColor = MaterialTheme.colorScheme.surface
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(228.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // upper area: category chip + star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // placeholder chip
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(shimmerModifier),
                )
                // placeholder star
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .then(shimmerModifier),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // lower area: title, subtitle, progress
            Column {
                // title (2 lines)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .then(shimmerModifier),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .then(shimmerModifier),
                )
                Spacer(modifier = Modifier.height(12.dp))

                // subtitle
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .then(shimmerModifier),
                )
                Spacer(modifier = Modifier.height(12.dp))

                // progress bar placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .then(shimmerModifier),
                )
            }
        }
    }
}

/**
 * grid of [shimmercoursecard] to simulate course list loading.
 *
 * @param itemCount number of shimmer cards to show (default: 6).
 * @param columns number of grid columns (default: 1).
 */
@Composable
fun ShimmerCourseList(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
    columns: Int = 1,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(itemCount) {
            ShimmerCourseCard()
        }
    }
}

// ── previews ─────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Shimmer Card – Light")
@Composable
private fun ShimmerCourseCardLightPreview() {
    WeBeepTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            ShimmerCourseCard()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117, name = "Shimmer Card – Dark")
@Composable
private fun ShimmerCourseCardDarkPreview() {
    WeBeepTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
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
