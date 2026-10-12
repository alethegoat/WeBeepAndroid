package it.polimi.webeep.feature.dashboard.presentation.components

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.components.ShimmerStyle
import it.polimi.webeep.core.ui.components.groupedListContainerColor
import it.polimi.webeep.core.ui.components.rememberShimmerStyle
import it.polimi.webeep.core.ui.components.shimmerPlaceholder
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme

/** Skeleton text-line sizes; the monogram size comes from [CourseCardDimens] to stay in sync. */
private val CodeLineWidth = 56.dp
private val SmallLineHeight = 12.dp
private val TitleLineHeight = 18.dp

@Composable
fun CourseCardSkeleton(modifier: Modifier = Modifier, style: ShimmerStyle = rememberShimmerStyle()) {
    val small = MaterialTheme.shapes.extraSmall
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = groupedListContainerColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WeBeepSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(CourseCardDimens.MonogramSize)
                    .shimmerPlaceholder(style, MaterialTheme.shapes.medium)
            )
            Spacer(modifier = Modifier.width(WeBeepSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(CodeLineWidth)
                        .height(SmallLineHeight)
                        .shimmerPlaceholder(style, small)
                )
                Spacer(modifier = Modifier.height(WeBeepSpacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(TitleLineHeight)
                        .shimmerPlaceholder(style, small)
                )
                Spacer(modifier = Modifier.height(WeBeepSpacing.xxs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(TitleLineHeight)
                        .shimmerPlaceholder(style, small)
                )
                Spacer(modifier = Modifier.height(WeBeepSpacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(SmallLineHeight)
                        .shimmerPlaceholder(style, small)
                )
            }
        }
    }
}

@Composable
fun CourseListSkeleton(modifier: Modifier = Modifier, itemCount: Int = 6, columns: Int = 1) {
    val style = rememberShimmerStyle()
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(WeBeepSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WeBeepSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(WeBeepSpacing.sm),
        userScrollEnabled = false
    ) {
        items(itemCount) {
            CourseCardSkeleton(style = style)
        }
    }
}

@Preview(showBackground = true, name = "Shimmer Card – Light")
@Composable
private fun CourseCardSkeletonLightPreview() {
    WeBeepTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
            CourseCardSkeleton()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117, name = "Shimmer Card – Dark")
@Composable
private fun CourseCardSkeletonDarkPreview() {
    WeBeepTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
            CourseCardSkeleton()
        }
    }
}

@Preview(showBackground = true, name = "Shimmer List – Light")
@Composable
private fun CourseListSkeletonPreview() {
    WeBeepTheme(darkTheme = false) {
        CourseListSkeleton(itemCount = 4)
    }
}
