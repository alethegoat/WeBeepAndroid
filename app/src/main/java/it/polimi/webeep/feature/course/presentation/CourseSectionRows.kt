package it.polimi.webeep.feature.course.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.GroupedListItem
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme

/** Start inset that lines text up with row labels (after the file-type icon). */
private val RowTextInset = WeBeepSpacing.md + FileIconContainerSize + WeBeepSpacing.sm

/** Tappable title of a course section; its rows follow it while expanded. */
@Composable
internal fun SectionHeaderRow(item: CourseListItem.SectionHeader, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GroupedListItem(position = item.position, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(
                    start = WeBeepSpacing.md,
                    end = WeBeepSpacing.xxs,
                    top = WeBeepSpacing.sm,
                    bottom = WeBeepSpacing.sm
                )
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                if (item.summary != null) {
                    Text(
                        text = item.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = WeBeepSpacing.xxs)
                    )
                }
            }
            ExpandChevron(
                expanded = item.isExpanded,
                contentDescription = stringResource(
                    if (item.isExpanded) R.string.cd_collapse_section else R.string.cd_expand_section
                )
            )
        }
    }
}

@Composable
internal fun SectionEmptyRow(item: CourseListItem.SectionEmpty, modifier: Modifier = Modifier) {
    GroupedListItem(position = item.position, modifier = modifier) {
        Text(
            text = stringResource(R.string.no_materials_section),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.md)
        )
    }
}

/**
 * One module: a folder (tap toggles it), a single file (tap previews it), a link
 * (tap opens it) or a multi-file resource whose files follow as their own rows.
 */
@Composable
internal fun ModuleItemRow(
    item: CourseListItem.Module,
    onToggleFolder: (String) -> Unit,
    onPreviewClick: (FileUiModel) -> Unit,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val module = item.module
    val hasLinkPreview = item.kind == ModuleKind.LINK && !module.url.isNullOrBlank()

    val fileColors = WeBeepTheme.colors
    val (moduleIcon, moduleColor) = when (item.kind) {
        ModuleKind.FOLDER -> Icons.Rounded.Folder to fileColors.fileFolder
        ModuleKind.LINK -> Icons.Rounded.Link to fileColors.fileLink
        ModuleKind.SINGLE_FILE -> fileIconAndColor(module.files.first().filename, fileColors)
        ModuleKind.MULTI_FILE, ModuleKind.OTHER -> Icons.Rounded.Description to fileColors.fileGeneric
    }
    val onClick: (() -> Unit)? = when {
        item.kind == ModuleKind.FOLDER -> {
            { onToggleFolder(folderRootKey(module.id)) }
        }
        item.kind == ModuleKind.SINGLE_FILE -> {
            { onPreviewClick(module.files.first()) }
        }
        hasLinkPreview -> {
            { onPreviewClick(module.toLinkPreview()) }
        }
        else -> null
    }

    GroupedListItem(position = item.position, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = WeBeepSpacing.md,
                        end = WeBeepSpacing.xxs,
                        top = WeBeepSpacing.xxs,
                        bottom = WeBeepSpacing.xxs
                    )
            ) {
                FileTypeIcon(icon = moduleIcon, tint = moduleColor)
                Spacer(modifier = Modifier.width(WeBeepSpacing.sm))
                RowLabels(
                    title = module.name,
                    meta = if (item.kind == ModuleKind.SINGLE_FILE) module.files.first().formattedSize else null,
                    modifier = Modifier.weight(1f)
                )
                ModuleTrailing(
                    item = item,
                    hasLinkPreview = hasLinkPreview,
                    onDownloadClick = onDownloadClick,
                    onOpenClick = onOpenClick
                )
            }

            if (module.description != null) {
                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = RowTextInset, end = WeBeepSpacing.md, bottom = WeBeepSpacing.sm)
                )
            }
        }
    }
}

@Composable
private fun ModuleTrailing(
    item: CourseListItem.Module,
    hasLinkPreview: Boolean,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit
) {
    when {
        item.kind == ModuleKind.FOLDER -> ExpandChevron(
            expanded = item.isFolderExpanded,
            contentDescription = stringResource(
                if (item.isFolderExpanded) R.string.cd_collapse_folder else R.string.cd_expand_folder
            )
        )
        item.kind == ModuleKind.SINGLE_FILE -> {
            val file = item.module.files.first()
            FileActionButton(
                state = file.downloadState,
                onDownloadClick = { onDownloadClick(file) },
                onOpenClick = { onOpenClick(file) }
            )
        }
        hasLinkPreview -> Box(
            modifier = Modifier.size(WeBeepSpacing.touchTarget),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Visibility,
                contentDescription = stringResource(R.string.cd_preview),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(RowIconSize)
            )
        }
    }
}

private fun ModuleUiModel.toLinkPreview(): FileUiModel = FileUiModel(
    filename = name,
    filepath = "/",
    localFileName = "",
    fileUrl = url.orEmpty(),
    fileSize = 0L,
    mimeType = "text/html",
    formattedSize = ""
)
