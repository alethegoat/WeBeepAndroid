package it.polimi.webeep.feature.course.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepColors
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.bouncyClickable

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
        "pdf" -> Icons.Default.PictureAsPdf to colors.filePdf
        "doc", "docx" -> Icons.Default.Description to colors.fileWord
        "xls", "xlsx" -> Icons.Default.TableChart to colors.fileExcel
        "ppt", "pptx" -> Icons.Default.Slideshow to colors.filePowerPoint
        "zip", "rar", "7z" -> Icons.Default.FolderZip to colors.fileZip
        else -> Icons.Default.Description to colors.fileGeneric
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
            imageVector = Icons.Default.ExpandMore,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(RowIconSize)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}

/** Compact download / open action: 48dp touch target, 24dp icon, no filled circle. */
@Composable
internal fun FileActionButton(
    state: DownloadState,
    onDownloadClick: () -> Unit,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    Crossfade(
        targetState = state,
        animationSpec = WeBeepMotion.smooth(reduceMotion = reduceMotion),
        label = "file_action_crossfade",
        modifier = modifier
    ) { downloadState ->
        when (downloadState) {
            DownloadState.NOT_STARTED -> FileActionIcon(
                icon = Icons.Default.Download,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(R.string.cd_download_resource),
                onClick = onDownloadClick
            )
            DownloadState.DOWNLOADING -> {
                val description = stringResource(R.string.cd_downloading)
                Box(
                    modifier = Modifier
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
                icon = Icons.Default.CheckCircle,
                tint = WeBeepTheme.colors.success,
                contentDescription = stringResource(R.string.cd_open_file),
                onClick = onOpenClick
            )
            DownloadState.FAILED -> FileActionIcon(
                icon = Icons.Default.ErrorOutline,
                tint = MaterialTheme.colorScheme.error,
                contentDescription = stringResource(R.string.cd_error_retry),
                onClick = onDownloadClick
            )
        }
    }
}

@Composable
private fun FileActionIcon(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
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

@Composable
internal fun FileRow(
    file: FileUiModel,
    depth: Int,
    onPreviewClick: (FileUiModel) -> Unit,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val (fileIcon, fileColor) = fileIconAndColor(file.filename, WeBeepTheme.colors)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable { onPreviewClick(file) }
            .padding(
                start = WeBeepSpacing.md + indentFor(depth),
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

/**
 * Flat, background-free rendering of a module's file tree. [depth] is the
 * nesting level of the node's children; indentation is capped by [indentFor].
 */
@Composable
internal fun FolderTreeView(
    node: FolderNode,
    moduleId: Int,
    expandedFolders: Set<String>,
    onToggleFolder: (String) -> Unit,
    depth: Int,
    onPreviewClick: (FileUiModel) -> Unit,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit
) {
    node.files.forEach { file ->
        FileRow(
            file = file,
            depth = depth,
            onPreviewClick = onPreviewClick,
            onDownloadClick = onDownloadClick,
            onOpenClick = onOpenClick
        )
    }
    val sortedSubfolders = remember(node) { node.subfolders.values.sortedBy { it.name } }
    sortedSubfolders.forEach { subNode ->
        SubfolderItem(
            node = subNode,
            moduleId = moduleId,
            expandedFolders = expandedFolders,
            onToggleFolder = onToggleFolder,
            depth = depth,
            onPreviewClick = onPreviewClick,
            onDownloadClick = onDownloadClick,
            onOpenClick = onOpenClick
        )
    }
}

@Composable
private fun SubfolderItem(
    node: FolderNode,
    moduleId: Int,
    expandedFolders: Set<String>,
    onToggleFolder: (String) -> Unit,
    depth: Int,
    onPreviewClick: (FileUiModel) -> Unit,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val key = folderKey(moduleId, node.fullPath)
    val isExpanded = key in expandedFolders
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable { onToggleFolder(key) }
            .padding(
                start = WeBeepSpacing.md + indentFor(depth),
                end = WeBeepSpacing.xxs,
                top = WeBeepSpacing.xxs,
                bottom = WeBeepSpacing.xxs
            )
    ) {
        FileTypeIcon(icon = Icons.Default.Folder, tint = WeBeepTheme.colors.fileFolder)
        Spacer(modifier = Modifier.width(WeBeepSpacing.sm))
        RowLabels(title = node.name, meta = null, modifier = Modifier.weight(1f))
        ExpandChevron(
            expanded = isExpanded,
            contentDescription = stringResource(
                if (isExpanded) R.string.cd_collapse_folder else R.string.cd_expand_folder
            )
        )
    }
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(animationSpec = WeBeepMotion.enter(reduceMotion = reduceMotion)) +
            fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)),
        exit = shrinkVertically(animationSpec = WeBeepMotion.exit(reduceMotion = reduceMotion)) +
            fadeOut(WeBeepMotion.exit(reduceMotion = reduceMotion))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            FolderTreeView(
                node = node,
                moduleId = moduleId,
                expandedFolders = expandedFolders,
                onToggleFolder = onToggleFolder,
                depth = depth + 1,
                onPreviewClick = onPreviewClick,
                onDownloadClick = onDownloadClick,
                onOpenClick = onOpenClick
            )
        }
    }
}
