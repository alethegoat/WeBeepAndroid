package it.polimi.webeep.feature.course.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.components.GroupPosition
import it.polimi.webeep.core.ui.components.GroupedListGap
import it.polimi.webeep.core.ui.components.GroupedListItem
import it.polimi.webeep.core.ui.components.GroupedListSectionGap
import it.polimi.webeep.core.ui.components.ShimmerStyle
import it.polimi.webeep.core.ui.components.groupPosition
import it.polimi.webeep.core.ui.components.rememberShimmerStyle
import it.polimi.webeep.core.ui.components.shimmerPlaceholder
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme

/** Number of section placeholders shown while a course loads. */
private const val SECTION_PLACEHOLDER_COUNT = 5

/** Rows inside the first (expanded) placeholder section. */
private const val ROW_PLACEHOLDER_COUNT = 3

private const val WIDE_TITLE_FRACTION = 0.7f
private const val NARROW_TITLE_FRACTION = 0.55f
private const val ROW_TITLE_FRACTION = 0.75f

private val SectionTitleHeight = 18.dp
private val RowTitleHeight = 14.dp
private val RowMetaWidth = 48.dp
private val RowMetaHeight = 10.dp

/**
 * Course-content placeholder shaped like the real list: the search field, then
 * grouped sections, the first one expanded with a few file rows.
 */
@Composable
fun CourseSectionsSkeleton(modifier: Modifier = Modifier) {
    val style = rememberShimmerStyle()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WeBeepSpacing.md),
        verticalArrangement = Arrangement.spacedBy(GroupedListGap)
    ) {
        Box(
            modifier = Modifier
                .padding(top = WeBeepSpacing.xs)
                .fillMaxWidth()
                .height(WeBeepSpacing.searchHeight)
                .shimmerPlaceholder(style, CircleShape)
        )
        repeat(SECTION_PLACEHOLDER_COUNT) { section ->
            val count = 1 + if (section == 0) ROW_PLACEHOLDER_COUNT else 0
            SkeletonSectionTitle(style = style, count = count, isWide = section % 2 != 0)
            repeat(count - 1) { row ->
                SkeletonFileRow(style = style, position = groupPosition(index = row + 1, count = count))
            }
        }
    }
}

@Composable
private fun SkeletonSectionTitle(style: ShimmerStyle, count: Int, isWide: Boolean) {
    GroupedListItem(
        position = groupPosition(index = 0, count = count),
        modifier = Modifier.padding(top = GroupedListSectionGap)
    ) {
        Box(
            modifier = Modifier
                .padding(WeBeepSpacing.md)
                .fillMaxWidth(if (isWide) WIDE_TITLE_FRACTION else NARROW_TITLE_FRACTION)
                .height(SectionTitleHeight)
                .shimmerPlaceholder(style, MaterialTheme.shapes.extraSmall)
        )
    }
}

@Composable
private fun SkeletonFileRow(style: ShimmerStyle, position: GroupPosition) {
    val small = MaterialTheme.shapes.extraSmall
    GroupedListItem(position = position) {
        Row(
            modifier = Modifier.padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(FileIconContainerSize)
                    .shimmerPlaceholder(style, MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(WeBeepSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ROW_TITLE_FRACTION)
                        .height(RowTitleHeight)
                        .shimmerPlaceholder(style, small)
                )
                Spacer(modifier = Modifier.height(WeBeepSpacing.xs))
                Box(
                    modifier = Modifier
                        .width(RowMetaWidth)
                        .height(RowMetaHeight)
                        .shimmerPlaceholder(style, small)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Skeleton")
@Composable
private fun CourseSectionsSkeletonPreview() {
    WeBeepTheme(darkTheme = false) {
        CourseSectionsSkeleton()
    }
}
