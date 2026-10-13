package it.polimi.webeep.feature.course.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.common.CourseTitle
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.components.GroupPosition
import it.polimi.webeep.core.ui.components.GroupedListGap
import it.polimi.webeep.core.ui.components.GroupedListSectionGap
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.feature.course.presentation.CourseDetailAction
import it.polimi.webeep.feature.course.presentation.CourseListContent
import it.polimi.webeep.feature.course.presentation.CourseListItem
import it.polimi.webeep.feature.course.presentation.FileUiModel

internal val MaxContentWidth = 840.dp
private val EmptySearchHeight = 320.dp

/** Header, search field and the flattened rows of the course, in one lazy column. */
@Composable
internal fun CourseContentList(
    content: CourseListContent,
    courseTitle: CourseTitle,
    searchQuery: String,
    lazyListState: LazyListState,
    contentPadding: PaddingValues,
    onAction: (CourseDetailAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val onSearchQueryChange = { query: String -> onAction(CourseDetailAction.SearchQueryChange(query)) }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = MaxContentWidth),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(GroupedListGap)
        ) {
            item(key = "course_header", contentType = "course_header") {
                CourseHeader(
                    title = courseTitle,
                    modifier = Modifier
                        .padding(horizontal = WeBeepSpacing.md)
                        .padding(top = WeBeepSpacing.xs)
                )
            }
            item(key = "course_search", contentType = "course_search") {
                CourseSearchBar(query = searchQuery, onQueryChange = onSearchQueryChange)
            }
            if (content.noMatches) {
                noMatchesItem(onReset = { onSearchQueryChange("") })
            } else {
                courseRows(content.items, onAction)
            }
        }
    }
}

private fun LazyListScope.noMatchesItem(onReset: () -> Unit) {
    item(key = "course_no_results", contentType = "course_no_results") {
        EmptyStateView(
            icon = Icons.Rounded.Description,
            title = stringResource(R.string.no_materials_found),
            subtitle = stringResource(R.string.try_changing_filters),
            actionLabel = stringResource(R.string.reset_filters),
            onAction = onReset,
            modifier = Modifier.height(EmptySearchHeight)
        )
    }
}

private fun LazyListScope.courseRows(rows: List<CourseListItem>, onAction: (CourseDetailAction) -> Unit) {
    items(
        items = rows,
        key = { it.key },
        contentType = { it.contentType }
    ) { item ->
        val reduceMotion = LocalReduceMotion.current
        val startsGroup = item.position == GroupPosition.TOP || item.position == GroupPosition.SINGLE
        val rowModifier = Modifier
            .then(
                if (reduceMotion) {
                    Modifier
                } else {
                    // Expanding/collapsing inserts or removes rows: they fade, the rest glide.
                    Modifier.animateItem(
                        fadeInSpec = WeBeepMotion.enter(),
                        fadeOutSpec = WeBeepMotion.exit(),
                        placementSpec = WeBeepMotion.spatialSpring()
                    )
                }
            )
            .padding(horizontal = WeBeepSpacing.md)
            .padding(top = if (startsGroup) GroupedListSectionGap else 0.dp)
        CourseListRow(item = item, onAction = onAction, modifier = rowModifier)
    }
}

@Composable
private fun CourseListRow(item: CourseListItem, onAction: (CourseDetailAction) -> Unit, modifier: Modifier = Modifier) {
    val onToggleFolder = { key: String -> onAction(CourseDetailAction.ToggleFolder(key)) }
    val onPreview = { file: FileUiModel -> onAction(CourseDetailAction.PreviewFile(file)) }
    val onDownload = { file: FileUiModel -> onAction(CourseDetailAction.DownloadFile(file)) }
    val onOpen = { file: FileUiModel -> onAction(CourseDetailAction.OpenFile(file)) }
    when (item) {
        is CourseListItem.SectionHeader -> SectionHeaderRow(
            item = item,
            onClick = { onAction(CourseDetailAction.ToggleSection(item.sectionId)) },
            modifier = modifier
        )
        is CourseListItem.SectionEmpty -> SectionEmptyRow(item = item, modifier = modifier)
        is CourseListItem.Module -> ModuleItemRow(
            item = item,
            onToggleFolder = onToggleFolder,
            onPreviewClick = onPreview,
            onDownloadClick = onDownload,
            onOpenClick = onOpen,
            modifier = modifier
        )
        is CourseListItem.Subfolder -> SubfolderItemRow(item = item, onToggleFolder = onToggleFolder, modifier = modifier)
        is CourseListItem.File -> FileItemRow(
            item = item,
            onPreviewClick = onPreview,
            onDownloadClick = onDownload,
            onOpenClick = onOpen,
            modifier = modifier
        )
    }
}
