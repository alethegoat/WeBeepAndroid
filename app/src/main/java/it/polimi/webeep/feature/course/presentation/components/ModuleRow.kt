package it.polimi.webeep.feature.course.presentation.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.GroupedListItem
import it.polimi.webeep.core.ui.theme.WeBeepColors
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.feature.course.presentation.CourseListItem
import it.polimi.webeep.feature.course.presentation.FileUiModel
import it.polimi.webeep.feature.course.presentation.ModuleKind
import it.polimi.webeep.feature.course.presentation.ModuleUiModel
import it.polimi.webeep.feature.course.presentation.folderRootKey

/** Start inset that lines text up with row labels (after the file-type icon). */
private val RowTextInset = WeBeepSpacing.md + FileIconContainerSize + WeBeepSpacing.sm

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
    val (moduleIcon, moduleColor) = moduleIconAndColor(item.kind, module, WeBeepTheme.colors)
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
            ModuleMainRow(
                item = item,
                icon = moduleIcon,
                tint = moduleColor,
                hasLinkPreview = hasLinkPreview,
                onDownloadClick = onDownloadClick,
                onOpenClick = onOpenClick
            )
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

private fun moduleIconAndColor(kind: ModuleKind, module: ModuleUiModel, colors: WeBeepColors): Pair<ImageVector, Color> = when (kind) {
    ModuleKind.FOLDER -> Icons.Rounded.Folder to colors.fileFolder
    ModuleKind.LINK -> Icons.Rounded.Link to colors.fileLink
    ModuleKind.SINGLE_FILE -> fileIconAndColor(module.files.first().filename, colors)
    ModuleKind.MULTI_FILE, ModuleKind.OTHER -> Icons.Rounded.Description to colors.fileGeneric
}

@Composable
private fun ModuleMainRow(
    item: CourseListItem.Module,
    icon: ImageVector,
    tint: Color,
    hasLinkPreview: Boolean,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit
) {
    val module = item.module
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
        FileTypeIcon(icon = icon, tint = tint)
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
