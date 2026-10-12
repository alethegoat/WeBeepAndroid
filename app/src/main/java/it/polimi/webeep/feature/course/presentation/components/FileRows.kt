package it.polimi.webeep.feature.course.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.GroupedListItem
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.feature.course.presentation.CourseListItem
import it.polimi.webeep.feature.course.presentation.FileUiModel

/** A file inside a multi-file module or a folder, indented by its [CourseListItem.File.depth]. */
@Composable
internal fun FileItemRow(
    item: CourseListItem.File,
    onPreviewClick: (FileUiModel) -> Unit,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val file = item.file
    val (fileIcon, fileColor) = fileIconAndColor(file.filename, WeBeepTheme.colors)
    GroupedListItem(position = item.position, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onPreviewClick(file) }
                .padding(
                    start = WeBeepSpacing.md + indentFor(item.depth),
                    end = WeBeepSpacing.xxs,
                    top = WeBeepSpacing.xxs,
                    bottom = WeBeepSpacing.xxs
                )
        ) {
            FileTypeIcon(icon = fileIcon, tint = fileColor)
            Spacer(modifier = Modifier.width(WeBeepSpacing.sm))
            RowLabels(
                title = file.filename,
                meta = file.formattedSize,
                modifier = Modifier.weight(1f)
            )
            FileActionButton(
                state = file.downloadState,
                onDownloadClick = { onDownloadClick(file) },
                onOpenClick = { onOpenClick(file) }
            )
        }
    }
}

/** A sub-folder of a folder module; its content rows follow it when expanded. */
@Composable
internal fun SubfolderItemRow(item: CourseListItem.Subfolder, onToggleFolder: (String) -> Unit, modifier: Modifier = Modifier) {
    GroupedListItem(position = item.position, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onToggleFolder(item.folderKey) }
                .padding(
                    start = WeBeepSpacing.md + indentFor(item.depth),
                    end = WeBeepSpacing.xxs,
                    top = WeBeepSpacing.xxs,
                    bottom = WeBeepSpacing.xxs
                )
        ) {
            FileTypeIcon(icon = Icons.Rounded.Folder, tint = WeBeepTheme.colors.fileFolder)
            Spacer(modifier = Modifier.width(WeBeepSpacing.sm))
            RowLabels(title = item.name, meta = null, modifier = Modifier.weight(1f))
            ExpandChevron(
                expanded = item.isExpanded,
                contentDescription = stringResource(
                    if (item.isExpanded) R.string.cd_collapse_folder else R.string.cd_expand_folder
                )
            )
        }
    }
}
