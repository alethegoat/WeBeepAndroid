package it.polimi.webeep.feature.course.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.GroupedListItem
import it.polimi.webeep.core.ui.theme.WeBeepColors
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion

/** Size of the tinted square that hosts a file-type icon. */
internal val FileIconContainerSize = 40.dp

/** Standard Material icon size used inside rows and icon buttons. */
internal val RowIconSize = 24.dp

/** Indentation added for every nested folder level. */
private val FolderIndentStep = WeBeepSpacing.sm

/** Deeper levels stop indenting so long file names stay readable. */
private const val MaxVisualIndentLevels = 3

private const val FileIconBackgroundAlpha = 0.14f

private val ProgressStrokeWidth = 2.dp

internal fun indentFor(depth: Int): Dp = FolderIndentStep * depth.coerceIn(0, MaxVisualIndentLevels)

internal fun fileIconAndColor(filename: String, colors: WeBeepColors): Pair<ImageVector, Color> {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf" -> Icons.Rounded.PictureAsPdf to colors.filePdf
        "doc", "docx" -> Icons.Rounded.Description to colors.fileWord
        "xls", "xlsx" -> Icons.Rounded.TableChart to colors.fileExcel
        "ppt", "pptx" -> Icons.Rounded.Slideshow to colors.filePowerPoint
        "zip", "rar", "7z" -> Icons.Rounded.FolderZip to colors.fileZip
        else -> Icons.Rounded.Description to colors.fileGeneric
    }
}

/** Tinted rounded square with a file-type glyph; decorative, so no content description. */
@Composable
internal fun FileTypeIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(FileIconContainerSize)
            .clip(MaterialTheme.shapes.small)
            .background(tint.copy(alpha = FileIconBackgroundAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(RowIconSize)
        )
    }
}

/** Chevron that rotates when [expanded] changes, instant when reduce-motion is on. */
@Composable
internal fun ExpandChevron(
    expanded: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = WeBeepMotion.smooth(reduceMotion = reduceMotion),
        label = "chevron_rotation"
    )
    Box(
        modifier = modifier.size(WeBeepSpacing.touchTarget),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(RowIconSize)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}

/**
 * Compact download / open action: 48dp touch target, 24dp icon, no filled circle.
 * A plain switch, not a crossfade: every visible file row has one, and the state
 * changes rarely.
 */
@Composable
internal fun FileActionButton(
    state: DownloadState,
    onDownloadClick: () -> Unit,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        DownloadState.NOT_STARTED -> FileActionIcon(
            icon = Icons.Rounded.Download,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = stringResource(R.string.cd_download_resource),
            onClick = onDownloadClick,
            modifier = modifier
        )
        DownloadState.DOWNLOADING -> {
            val description = stringResource(R.string.cd_downloading)
            Box(
                modifier = modifier
                    .size(WeBeepSpacing.touchTarget)
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = ProgressStrokeWidth,
                    modifier = Modifier.size(RowIconSize - WeBeepSpacing.xxs)
                )
            }
        }
        DownloadState.COMPLETED -> FileActionIcon(
            icon = Icons.Rounded.CheckCircle,
            tint = WeBeepTheme.colors.success,
            contentDescription = stringResource(R.string.cd_open_file),
            onClick = onOpenClick,
            modifier = modifier
        )
        DownloadState.FAILED -> FileActionIcon(
            icon = Icons.Rounded.ErrorOutline,
            tint = MaterialTheme.colorScheme.error,
            contentDescription = stringResource(R.string.cd_error_retry),
            onClick = onDownloadClick,
            modifier = modifier
        )
    }
}

@Composable
private fun FileActionIcon(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(RowIconSize)
        )
    }
}

/** Name (max 2 lines) plus optional metadata line, shared by module and file rows. */
@Composable
internal fun RowLabels(
    title: String,
    meta: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!meta.isNullOrBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

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
internal fun SubfolderItemRow(
    item: CourseListItem.Subfolder,
    onToggleFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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

internal data class FolderNode(
    val name: String,
    val fullPath: String,
    val files: MutableList<FileUiModel> = mutableListOf(),
    val subfolders: MutableMap<String, FolderNode> = mutableMapOf()
)

internal fun buildFolderTree(files: List<FileUiModel>): FolderNode {
    val root = FolderNode("", "")
    files.forEach { file ->
        val segments = file.filepath.trim('/').split('/').filter { it.isNotEmpty() }
        var current = root
        var pathAccumulator = ""
        for (segment in segments) {
            pathAccumulator += "/$segment"
            current = current.subfolders.getOrPut(segment) { FolderNode(segment, pathAccumulator) }
        }
        current.files.add(file)
    }
    return root
}
